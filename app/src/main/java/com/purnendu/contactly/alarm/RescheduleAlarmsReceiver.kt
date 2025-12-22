package com.purnendu.contactly.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.purnendu.contactly.data.ContactsRepository
import com.purnendu.contactly.data.SchedulesRepository
import com.purnendu.contactly.data.local.room.AppDatabase
import com.purnendu.contactly.utils.AlarmRequestCodeUtils
import com.purnendu.contactly.utils.DayUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class RescheduleAlarmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val alarmManager = context.getSystemService(AlarmManager::class.java)
                val repo = SchedulesRepository(AppDatabase.getDataBase(context))
                val contactsRepo = ContactsRepository.get(context)
                val entities = repo.getAllEntities()

                entities.forEach { e ->
                    // Check if the contact still exists
                    val contact = try {
                        contactsRepo.fetchContactById(e.contactId)
                    } catch (ex: SecurityException) {
                        Log.e("RescheduleAlarmsRx", "No permission to access contacts", ex)
                        null
                    }
                    
                    // If contact doesn't exist, remove from database and skip scheduling
                    if (contact == null) {
                        Log.d("RescheduleAlarmsRx", "Contact ${e.contactId} not found, removing schedule")
                        repo.deleteByContactId(e.contactId)
                        return@forEach // Skip to next schedule
                    }

                    // Extract selected days from bitmask (e.g., 127 = all days)
                    val daysList = DayUtils.extractDaysFromBitmask(e.selectedDays)
                    
                    // If no days selected, skip this schedule
                    if (daysList.isEmpty()) {
                        Log.w("RescheduleAlarmsRx", "No days selected for schedule ${e.scheduleId}, skipping")
                        return@forEach
                    }

                    // Schedule alarms for each selected day
                    daysList.forEach { dayOfWeek ->
                        // Calculate next occurrence for this specific day
                        val applyAt = DayUtils.calculateNextOccurrence(e.startAtMillis, dayOfWeek)
                        val revertAt = DayUtils.calculateNextOccurrence(e.endAtMillis, dayOfWeek)

                        // Generate unique request codes using centralized utility
                        val applyReqCode = AlarmRequestCodeUtils.generateApplyRequestCode(e.contactId, dayOfWeek)
                        val revertReqCode = AlarmRequestCodeUtils.generateRevertRequestCode(e.contactId, dayOfWeek)

                        // Create APPLY alarm intent
                        val applyIntent = Intent(context, AliasAlarmReceiver::class.java).apply {
                            action = AliasAlarmReceiver.ACTION_ALIAS
                            putExtra(AliasAlarmReceiver.EXTRA_OPERATION, AliasAlarmReceiver.OP_APPLY)
                            putExtra(AliasAlarmReceiver.EXTRA_CONTACT_ID, e.contactId)
                            putExtra(AliasAlarmReceiver.EXTRA_NAME, e.temporaryName)
                            putExtra(AliasAlarmReceiver.EXTRA_SCHEDULE_ID, e.scheduleId)
                            putExtra(AliasAlarmReceiver.EXTRA_DAY_OF_WEEK, dayOfWeek)
                            putExtra(AliasAlarmReceiver.EXTRA_SCHEDULE_TYPE, e.scheduleType) // 0 = ONE_TIME, 1 = REPEAT
                        }
                        
                        val applyPending = PendingIntent.getBroadcast(
                            context,
                            applyReqCode,
                            applyIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )

                        // Schedule APPLY alarm
                        try {
                            alarmManager.setExactAndAllowWhileIdle(
                                AlarmManager.RTC,
                                applyAt,
                                applyPending
                            )
                            Log.d("RescheduleAlarmsRx", "Scheduled APPLY for contact ${e.contactId}, day $dayOfWeek at $applyAt")
                        } catch (ex: SecurityException) {
                            Log.e("RescheduleAlarmsRx", "Failed to schedule APPLY alarm", ex)
                        }

                        // Create REVERT alarm intent
                        val revertIntent = Intent(context, AliasAlarmReceiver::class.java).apply {
                            action = AliasAlarmReceiver.ACTION_ALIAS
                            putExtra(AliasAlarmReceiver.EXTRA_OPERATION, AliasAlarmReceiver.OP_REVERT)
                            putExtra(AliasAlarmReceiver.EXTRA_CONTACT_ID, e.contactId)
                            putExtra(AliasAlarmReceiver.EXTRA_NAME, e.originalName)
                            putExtra(AliasAlarmReceiver.EXTRA_SCHEDULE_ID, e.scheduleId)
                            putExtra(AliasAlarmReceiver.EXTRA_DAY_OF_WEEK, dayOfWeek)
                            putExtra(AliasAlarmReceiver.EXTRA_SCHEDULE_TYPE, e.scheduleType) // 0 = ONE_TIME, 1 = REPEAT
                        }
                        
                        val revertPending = PendingIntent.getBroadcast(
                            context,
                            revertReqCode,
                            revertIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )

                        // Schedule REVERT alarm
                        try {
                            alarmManager.setExactAndAllowWhileIdle(
                                AlarmManager.RTC,
                                revertAt,
                                revertPending
                            )
                            Log.d("RescheduleAlarmsRx", "Scheduled REVERT for contact ${e.contactId}, day $dayOfWeek at $revertAt")
                        } catch (ex: SecurityException) {
                            Log.e("RescheduleAlarmsRx", "Failed to schedule REVERT alarm", ex)
                        }
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}