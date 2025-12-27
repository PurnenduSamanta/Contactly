package com.purnendu.contactly.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.purnendu.contactly.alarm.models.AlarmMetadata
import com.purnendu.contactly.alarm.models.AlarmScheduleResult
import com.purnendu.contactly.model.Contact
import com.purnendu.contactly.utils.AlarmRequestCodeUtils
import com.purnendu.contactly.utils.DayUtils
import com.purnendu.contactly.utils.ScheduleType

/**
 * Android implementation of AlarmScheduler.
 * Handles all AlarmManager interactions for scheduling contact name changes.
 */
class AndroidAlarmScheduler(
    private val context: Context
) : AlarmScheduler {
    
    private val alarmManager: AlarmManager? = context.getSystemService(AlarmManager::class.java)
    private val alarmSyncManager by lazy { 
        // Lazy initialization to avoid circular dependency
        org.koin.core.context.GlobalContext.get().get<AlarmSyncManager>()
    }
    
    override fun scheduleAlarms(
        contact: Contact,
        scheduleId: Long,
        originalName: String,
        temporaryName: String,
        startAtMillis: Long,
        endAtMillis: Long,
        selectedDays: Int,
        scheduleType: ScheduleType
    ): AlarmScheduleResult {
        var isAlarmSuccessfullyScheduled = true
        val contactId = contact.id ?: return AlarmScheduleResult(false, emptyList())

        // Extract selected days (0=Sun, 1=Mon, ...)
        val daysList = DayUtils.extractDaysFromBitmask(selectedDays)

        // If no days selected, don't schedule anything
        if (daysList.isEmpty()) return AlarmScheduleResult(false, emptyList())

        // Track alarm metadata for database storage
        val alarmMetadataList = mutableListOf<AlarmMetadata>()

        daysList.forEach { dayOfWeek ->
            // Calculate next occurrence for this day
            val applyAt = DayUtils.calculateNextOccurrence(startAtMillis, dayOfWeek)
            val revertAt = DayUtils.calculateNextOccurrence(endAtMillis, dayOfWeek)

            // Generate unique request codes using centralized utility
            val applyReqCode = AlarmRequestCodeUtils.generateApplyRequestCode(contactId, dayOfWeek)
            val revertReqCode = AlarmRequestCodeUtils.generateRevertRequestCode(contactId, dayOfWeek)

            // Store metadata for this alarm
            alarmMetadataList.add(
                AlarmMetadata(
                    requestCode = applyReqCode,
                    dayOfWeek = dayOfWeek,
                    operation = AliasAlarmReceiver.OP_APPLY,
                    triggerTimeMillis = applyAt
                )
            )
            alarmMetadataList.add(
                AlarmMetadata(
                    requestCode = revertReqCode,
                    dayOfWeek = dayOfWeek,
                    operation = AliasAlarmReceiver.OP_REVERT,
                    triggerTimeMillis = revertAt
                )
            )

            val applyIntent = Intent(context, AliasAlarmReceiver::class.java).apply {
                action = AliasAlarmReceiver.ACTION_ALIAS
                putExtra(AliasAlarmReceiver.EXTRA_OPERATION, AliasAlarmReceiver.OP_APPLY)
                putExtra(AliasAlarmReceiver.EXTRA_CONTACT_ID, contact.id)
                putExtra(AliasAlarmReceiver.EXTRA_ORIGINAL_NAME, originalName)
                putExtra(AliasAlarmReceiver.EXTRA_TEMPORARY_NAME, temporaryName)
                putExtra(AliasAlarmReceiver.EXTRA_SCHEDULE_ID, scheduleId)
                putExtra(AliasAlarmReceiver.EXTRA_DAY_OF_WEEK, dayOfWeek)
                putExtra(AliasAlarmReceiver.EXTRA_SCHEDULE_TYPE, if (scheduleType == ScheduleType.ONE_TIME) 0 else 1)
            }
            val revertIntent = Intent(context, AliasAlarmReceiver::class.java).apply {
                action = AliasAlarmReceiver.ACTION_ALIAS
                putExtra(AliasAlarmReceiver.EXTRA_OPERATION, AliasAlarmReceiver.OP_REVERT)
                putExtra(AliasAlarmReceiver.EXTRA_CONTACT_ID, contact.id)
                putExtra(AliasAlarmReceiver.EXTRA_ORIGINAL_NAME, originalName)
                putExtra(AliasAlarmReceiver.EXTRA_TEMPORARY_NAME, temporaryName)
                putExtra(AliasAlarmReceiver.EXTRA_SCHEDULE_ID, scheduleId)
                putExtra(AliasAlarmReceiver.EXTRA_DAY_OF_WEEK, dayOfWeek)
                putExtra(AliasAlarmReceiver.EXTRA_SCHEDULE_TYPE, if (scheduleType == ScheduleType.ONE_TIME) 0 else 1)
            }

            val applyPending = PendingIntent.getBroadcast(
                context,
                applyReqCode,
                applyIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val revertPending = PendingIntent.getBroadcast(
                context,
                revertReqCode,
                revertIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            try {
                alarmManager?.setExactAndAllowWhileIdle(
                    AlarmManager.RTC,
                    applyAt,
                    applyPending
                )
            } catch (e: SecurityException) {
                Log.d("AndroidAlarmScheduler", "Failed to schedule APPLY alarm: ${e.localizedMessage}")
                isAlarmSuccessfullyScheduled = false
            }
            
            try {
                alarmManager?.setExactAndAllowWhileIdle(
                    AlarmManager.RTC,
                    revertAt,
                    revertPending
                )
            } catch (e: SecurityException) {
                Log.d("AndroidAlarmScheduler", "Failed to schedule REVERT alarm: ${e.localizedMessage}")
                isAlarmSuccessfullyScheduled = false
            }
        }

        return AlarmScheduleResult(isAlarmSuccessfullyScheduled, alarmMetadataList)
    }
    
    override suspend fun cancelScheduleAlarms(scheduleId: Long) {
        alarmSyncManager.cancelScheduleAlarms(scheduleId)
    }
    
    override fun toJson(metadata: List<AlarmMetadata>): String {
        return alarmSyncManager.toJson(metadata)
    }
    
    override fun parseAlarmMetadata(json: String?): List<AlarmMetadata> {
        return alarmSyncManager.parseAlarmMetadata(json)
    }
}
