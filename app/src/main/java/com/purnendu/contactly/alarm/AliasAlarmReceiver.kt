package com.purnendu.contactly.alarm

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.ContactsContract
import android.util.Log
import androidx.core.content.ContextCompat
import android.content.ContentValues
import com.purnendu.contactly.data.repository.SchedulesRepository
import com.purnendu.contactly.data.local.preferences.AppPreferences
import com.purnendu.contactly.notification.NotificationHelper
import com.purnendu.contactly.utils.AlarmRequestCodeUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.Calendar

/**
 * BroadcastReceiver that handles alias (contact name change) alarms.
 * 
 * Uses Koin for dependency injection via KoinComponent interface.
 */
class AliasAlarmReceiver : BroadcastReceiver(), KoinComponent {
    
    private val schedulesRepo: SchedulesRepository by inject()
    private val contactlyAlarmManager: ContactlyAlarmManager by inject()
    private val appPreferences: AppPreferences by inject()
    
    @OptIn(DelicateCoroutinesApi::class)
    override fun onReceive(context: Context, intent: Intent) {
        val op = intent.getStringExtra(EXTRA_OPERATION) ?: return
        val contactId = intent.getLongExtra(EXTRA_CONTACT_ID, -1L)
        val originalName = intent.getStringExtra(EXTRA_ORIGINAL_NAME) ?: return
        val temporaryName = intent.getStringExtra(EXTRA_TEMPORARY_NAME) ?: return
        val scheduleId = intent.getLongExtra(EXTRA_SCHEDULE_ID, -1L)
        val dayOfWeek = intent.getIntExtra(EXTRA_DAY_OF_WEEK, -1)
        val scheduleType = intent.getIntExtra(EXTRA_SCHEDULE_TYPE, 0) // 0 = ONE_TIME, 1 = REPEAT
        if (contactId <= 0) return

        val pendingResult = goAsync()

        if (!hasWriteContactsPermission(context)) return

        CoroutineScope(Dispatchers.IO).launch{
            try {
                // Create a Clean up for the PendingIntent that triggered this alarm
                // This ensures that after alarm firing, the "Active" status clears immediately
                contactlyAlarmManager.cancelSpecificAlarm(contactId, dayOfWeek, op)

                // Derive the name to apply based on operation
                val isApply = op == OP_APPLY
                val nameToApply = if (isApply) temporaryName else originalName

                    applyName(context, contactId, nameToApply)

                    val notificationsEnabled = try {
                        appPreferences.notificationsEnabledFlow.first()
                    } catch (e: Exception) {
                        Log.e("AliasAlarmReceiver", "Failed to read notification preference", e)
                        false // Default to disabled if read fails
                    }

                    // Show notification on Main thread (best practice for system APIs)
                    if (notificationsEnabled) {
                        withContext(Dispatchers.Main) {
                            try {
                                NotificationHelper.showAlarmNotification(
                                    context = context,
                                    originalName = originalName,
                                    temporaryName = temporaryName,
                                    isApply = isApply,
                                    scheduleType = scheduleType
                                )
                            } catch (e: Exception) {
                                Log.e("AliasAlarmReceiver", "Failed to show notification", e)
                            }
                        }
                    }

                    // For one-time schedules: delete from database (IO thread)
                    if (scheduleType == 0 && op == OP_REVERT && scheduleId > 0) {
                        try {
                            schedulesRepo.deleteById(scheduleId)
                            Log.d("AliasAlarmReceiver", "Deleted one-time schedule: $scheduleId")
                        } catch (e: Exception) {
                            Log.e("AliasAlarmReceiver", "Failed to delete one-time schedule: $scheduleId", e)
                        }
                    }


                    // Only reschedule for next week if this is a REPEAT schedule (scheduleType == 1)
                    if (scheduleType == 1 && dayOfWeek >= 0) {
                        rescheduleForNextWeek(context, intent, dayOfWeek)
                    }
            }
            catch (e: Throwable) {
                Log.e("AliasAlarmReceiver", "Error processing alarm", e)
            }
            finally {
                pendingResult.finish()
            }
        }
    }

    private fun hasWriteContactsPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.WRITE_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
    }

    private suspend fun rescheduleForNextWeek(context: Context, originalIntent: Intent, dayOfWeek: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        
        // Calculate next occurrence (same day, next week)
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.WEEK_OF_YEAR, 1)
        
        // Adjust to exact day of week
        val currentDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1 // 0=Sunday
        val daysToAdd = (dayOfWeek - currentDayOfWeek + 7) % 7
        if (daysToAdd != 0) {
            calendar.add(Calendar.DAY_OF_YEAR, daysToAdd)
        }
        
        val nextTriggerTime = calendar.timeInMillis

        // Create a new intent with same extras
        val newIntent = Intent(context, AliasAlarmReceiver::class.java).apply {
            action = originalIntent.action
            putExtra(EXTRA_OPERATION, originalIntent.getStringExtra(EXTRA_OPERATION))
            putExtra(EXTRA_CONTACT_ID, originalIntent.getLongExtra(EXTRA_CONTACT_ID, -1L))
            putExtra(EXTRA_ORIGINAL_NAME, originalIntent.getStringExtra(EXTRA_ORIGINAL_NAME))
            putExtra(EXTRA_TEMPORARY_NAME, originalIntent.getStringExtra(EXTRA_TEMPORARY_NAME))
            putExtra(EXTRA_SCHEDULE_ID, originalIntent.getLongExtra(EXTRA_SCHEDULE_ID, -1L))
            putExtra(EXTRA_DAY_OF_WEEK, dayOfWeek)
            putExtra(EXTRA_SCHEDULE_TYPE, originalIntent.getIntExtra(EXTRA_SCHEDULE_TYPE, 0)) // Default to ONE-TIME
        }

        // Generate request code using centralized utility
        val contactId = originalIntent.getLongExtra(EXTRA_CONTACT_ID, -1L)
        val scheduleId = originalIntent.getLongExtra(EXTRA_SCHEDULE_ID, -1L)
        val op = originalIntent.getStringExtra(EXTRA_OPERATION) ?: return
        val reqCode = if (op == OP_APPLY) {
            AlarmRequestCodeUtils.generateApplyRequestCode(contactId, dayOfWeek)
        } else {
            AlarmRequestCodeUtils.generateRevertRequestCode(contactId, dayOfWeek)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reqCode,
            newIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC,
                nextTriggerTime,
                pendingIntent
            )
            Log.d("AliasAlarmReceiver", "Rescheduled alarm for next week: day=$dayOfWeek, time=$nextTriggerTime")
            
            // Update the database with new metadata and nearest trigger times
            updateSchedule(scheduleId, reqCode, op, nextTriggerTime)
            
        } catch (e: SecurityException) {
            Log.e("AliasAlarmReceiver", "Failed to reschedule alarm", e)
        }
    }
    
    /**
     * Updates the schedule's metadata in the database after rescheduling.
     * 1. Updates the specific alarm's trigger time in metadata
     * 2. Finds the nearest APPLY time and updates startAtMillis
     * 3. Finds the nearest REVERT time and updates endAtMillis
     */
    private suspend fun updateSchedule(
        scheduleId: Long,
        requestCode: Int,
        operation: String,
        newTriggerTime: Long
    ) {
        try {
            val schedule = schedulesRepo.getById(scheduleId) ?: run {
                Log.w("AliasAlarmReceiver", "Schedule not found for update: $scheduleId")
                return
            }
            
            // Parse existing metadata
            val existingMetadata = contactlyAlarmManager.parseAlarmMetadata(schedule.scheduledAlarmsMetadata)
            if (existingMetadata.isEmpty()) {
                Log.w("AliasAlarmReceiver", "No metadata found for schedule: $scheduleId")
                return
            }
            
            // Update the specific alarm's trigger time
            val updatedMetadata = existingMetadata.map { metadata ->
                if (metadata.requestCode == requestCode) {
                    metadata.copy(triggerTimeMillis = newTriggerTime)
                } else {
                    metadata
                }
            }
            
            // Find the nearest (soonest) APPLY time among all metadata
            val nearestApplyTime = updatedMetadata
                .filter { it.operation == OP_APPLY }
                .minByOrNull { it.triggerTimeMillis }
                ?.triggerTimeMillis ?: schedule.startAtMillis
            
            // Find the nearest (soonest) REVERT time among all metadata
            val nearestRevertTime = updatedMetadata
                .filter { it.operation == OP_REVERT }
                .minByOrNull { it.triggerTimeMillis }
                ?.triggerTimeMillis ?: schedule.endAtMillis
            
            // Update the schedule with new metadata and nearest times
            val updatedSchedule = schedule.copy(
                scheduledAlarmsMetadata = contactlyAlarmManager.toJson(updatedMetadata),
                startAtMillis = nearestApplyTime,
                endAtMillis = nearestRevertTime
            )
            
            schedulesRepo.update(updatedSchedule)
            Log.d("AliasAlarmReceiver", "Updated schedule metadata: id=$scheduleId, " +
                    "nearestApply=$nearestApplyTime, nearestRevert=$nearestRevertTime")
            
        } catch (e: Exception) {
            Log.e("AliasAlarmReceiver", "Failed to update schedule metadata: $scheduleId", e)
        }
    }

    private fun applyName(context: Context, contactId: Long, name: String) {
        val resolver = context.contentResolver
        val rawId = resolver.query(
            ContactsContract.RawContacts.CONTENT_URI,
            arrayOf(ContactsContract.RawContacts._ID),
            ContactsContract.RawContacts.CONTACT_ID + "=?",
            arrayOf(contactId.toString()),
            null
        )?.use { c ->
            if (c.moveToFirst()) c.getLong(0) else null
        }

        val values = ContentValues().apply {
            put(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, name)
            put(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME, name)
        }

        val updated = resolver.update(
            ContactsContract.Data.CONTENT_URI,
            values,
            ContactsContract.Data.CONTACT_ID + "=? AND " +
                ContactsContract.Data.MIMETYPE + "=?",
            arrayOf(
                contactId.toString(),
                ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE
            )
        )

        if (updated == 0 && rawId != null) {
            val insertValues = ContentValues().apply {
                put(ContactsContract.Data.RAW_CONTACT_ID, rawId)
                put(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                put(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, name)
                put(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME, name)
            }
            resolver.insert(ContactsContract.Data.CONTENT_URI, insertValues)
        }
    }



    companion object {
        const val ACTION_ALIAS = "com.purnendu.contactly.action.ALIAS"
        const val EXTRA_OPERATION = "operation"
        const val EXTRA_CONTACT_ID = "contactId"
        const val EXTRA_ORIGINAL_NAME = "originalName"
        const val EXTRA_TEMPORARY_NAME = "temporaryName"
        const val EXTRA_SCHEDULE_ID = "scheduleId"
        const val EXTRA_DAY_OF_WEEK = "dayOfWeek"
        const val EXTRA_SCHEDULE_TYPE = "scheduleType"
        const val OP_APPLY = "apply"
        const val OP_REVERT = "revert"
    }
}