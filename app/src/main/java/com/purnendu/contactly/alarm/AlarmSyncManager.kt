package com.purnendu.contactly.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.purnendu.contactly.data.ContactsRepository
import com.purnendu.contactly.data.SchedulesRepository
import com.purnendu.contactly.data.local.room.AppDatabase
import com.purnendu.contactly.data.local.room.ScheduleEntity
import com.purnendu.contactly.utils.DayUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Manages synchronization between AlarmManager and Room Database
 * Ensures database is the single source of truth for scheduled alarms
 */
class AlarmSyncManager(private val context: Context) {

    val database = AppDatabase.getDataBase(context)
    val schedulesRepo = SchedulesRepository(database)
    val contactsRepo = ContactsRepository.get(context)
    val alarmManager: AlarmManager? = context.getSystemService(AlarmManager::class.java)
    private val gson = Gson()
    private val TAG = "AlarmSyncManager"

    /**
     * Check if a specific alarm is scheduled in AlarmManager
     */
    fun isAlarmScheduled(
        requestCode: Int,
        contactId: Long,
        name: String,
        operation: String,
        dayOfWeek: Int,
        scheduleId: Long
    ): Boolean {
        val intent = buildAlarmIntent(
            context = context,
            contactId = contactId,
            name = name,
            operation = operation,
            dayOfWeek = dayOfWeek,
            scheduleId = scheduleId
        )

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )

        val exists = pendingIntent != null
        Log.d(TAG, "Alarm check: reqCode=$requestCode, exists=$exists")
        return exists
    }

    /**
     * Build alarm intent with consistent parameter order
     * This ensures FLAG_NO_CREATE can find existing alarms
     */
    private fun buildAlarmIntent(
        context: Context,
        contactId: Long,
        name: String,
        operation: String,
        dayOfWeek: Int,
        scheduleId: Long
    ): Intent {
        return Intent(context, AliasAlarmReceiver::class.java).apply {
            action = AliasAlarmReceiver.ACTION_ALIAS
            putExtra(AliasAlarmReceiver.EXTRA_OPERATION, operation)
            putExtra(AliasAlarmReceiver.EXTRA_CONTACT_ID, contactId)
            putExtra(AliasAlarmReceiver.EXTRA_NAME, name)
            putExtra(AliasAlarmReceiver.EXTRA_SCHEDULE_ID, scheduleId)
            putExtra(AliasAlarmReceiver.EXTRA_DAY_OF_WEEK, dayOfWeek)
        }
    }

    /**
     * Parse alarm metadata from JSON string
     */
    fun parseAlarmMetadata(json: String?): List<AlarmMetadata> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            val type = object : TypeToken<List<AlarmMetadata>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse alarm metadata: $json", e)
            emptyList()
        }
    }

    /**
     * Convert alarm metadata list to JSON string
     */
    fun toJson(metadata: List<AlarmMetadata>): String {
        return gson.toJson(metadata)
    }

    /**
     * Generate alarm metadata for a schedule
     */
    fun generateAlarmMetadata(
        schedule: ScheduleEntity,
        selectedDays: List<Int>
    ): List<AlarmMetadata> {
        val metadata = mutableListOf<AlarmMetadata>()
        val baseReqCode = (schedule.contactId % 1000000).toInt() * 100

        selectedDays.forEach { dayOfWeek ->
            val applyAt = DayUtils.calculateNextOccurrence(schedule.startAtMillis, dayOfWeek)
            val revertAt = DayUtils.calculateNextOccurrence(schedule.endAtMillis, dayOfWeek)

            val applyReqCode = baseReqCode + (dayOfWeek * 2)
            val revertReqCode = baseReqCode + (dayOfWeek * 2) + 1

            metadata.add(
                AlarmMetadata(
                    requestCode = applyReqCode,
                    dayOfWeek = dayOfWeek,
                    operation = AliasAlarmReceiver.OP_APPLY,
                    triggerTimeMillis = applyAt
                )
            )
            metadata.add(
                AlarmMetadata(
                    requestCode = revertReqCode,
                    dayOfWeek = dayOfWeek,
                    operation = AliasAlarmReceiver.OP_REVERT,
                    triggerTimeMillis = revertAt
                )
            )
        }

        return metadata
    }

    /**
     * Schedule a single alarm based on metadata
     */
    private fun scheduleAlarm(
        schedule: ScheduleEntity,
        metadata: AlarmMetadata
    ) {
        val name = if (metadata.operation == AliasAlarmReceiver.OP_APPLY) {
            schedule.temporaryName
        } else {
            schedule.originalName
        }

        val intent = buildAlarmIntent(
            context = context,
            contactId = schedule.contactId,
            name = name,
            operation = metadata.operation,
            dayOfWeek = metadata.dayOfWeek,
            scheduleId = schedule.scheduleId
        )

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            metadata.requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            alarmManager?.setExactAndAllowWhileIdle(
                AlarmManager.RTC,
                metadata.triggerTimeMillis,
                pendingIntent
            )
            Log.d(TAG, "Scheduled alarm: reqCode=${metadata.requestCode}, day=${metadata.dayOfWeek}, op=${metadata.operation}")
        } catch (e: SecurityException) {
            Log.e(TAG, "Failed to schedule alarm: reqCode=${metadata.requestCode}", e)
        }
    }

    /**
     * Sync all schedules from database to AlarmManager
     * This is the main sync function called on app startup
     */
    suspend fun syncAllSchedules(): SyncResult = withContext(Dispatchers.IO) {
        Log.d(TAG, "Starting alarm sync...")
        val allSchedules = schedulesRepo.getAllEntities()
        var scheduledCount = 0
        var skippedCount = 0
        var errorCount = 0
        var orphanedCount = 0

        allSchedules.forEach { schedule ->
            try {
                // Verify contact still exists (only if we have permission)
                var hasContactPermission = true
                val contact = try {
                    contactsRepo.fetchContactById(schedule.contactId)
                } catch (ex: SecurityException) {
                    Log.w(TAG, "No permission to verify contact existence, skipping validation")
                    hasContactPermission = false
                    null
                }

                // Only delete if we have permission and contact is genuinely missing
                // If no permission, skip validation and proceed to sync
                if (hasContactPermission && contact == null) {
                    Log.d(TAG, "Contact ${schedule.contactId} not found, removing schedule")
                    cancelScheduleAlarms(schedule.scheduleId)
                    schedulesRepo.deleteByContactId(schedule.contactId)
                    orphanedCount++
                    return@forEach
                }

                // Parse stored metadata
                val storedMetadata = parseAlarmMetadata(schedule.scheduledAlarmsMetadata)

                if (storedMetadata.isEmpty()) {
                    // No metadata stored, need to regenerate and schedule all
                    Log.w(TAG, "No metadata for schedule ${schedule.scheduleId}, regenerating")
                    val daysList = DayUtils.extractDaysFromBitmask(schedule.selectedDays)
                    val newMetadata = generateAlarmMetadata(schedule, daysList)
                    
                    // Schedule all alarms
                    newMetadata.forEach { metadata ->
                        scheduleAlarm( schedule, metadata)
                        scheduledCount++
                    }
                    
                    // Update database with metadata
                    val updated = schedule.copy(scheduledAlarmsMetadata = toJson(newMetadata))
                    schedulesRepo.update(updated)
                } else {
                    // Check each alarm and reschedule if missing
                    storedMetadata.forEach { metadata ->
                        val name = if (metadata.operation == AliasAlarmReceiver.OP_APPLY) {
                            schedule.temporaryName
                        } else {
                            schedule.originalName
                        }

                        val exists = isAlarmScheduled(
                            requestCode = metadata.requestCode,
                            contactId = schedule.contactId,
                            name = name,
                            operation = metadata.operation,
                            dayOfWeek = metadata.dayOfWeek,
                            scheduleId = schedule.scheduleId
                        )

                        if (!exists) {
                            Log.d(TAG, "Alarm missing, rescheduling: reqCode=${metadata.requestCode}")
                            scheduleAlarm(schedule, metadata)
                            scheduledCount++
                        } else {
                            skippedCount++
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing schedule ${schedule.scheduleId}", e)
                errorCount++
            }
        }

        val result = SyncResult(
            totalSchedules = allSchedules.size,
            alarmsScheduled = scheduledCount,
            alarmsSkipped = skippedCount,
            errors = errorCount,
            orphanedSchedulesRemoved = orphanedCount
        )
        
        Log.d(TAG, "Sync complete: $result")
        result
    }

     suspend fun cancelScheduleAlarms(scheduleId: Long) {
        withContext(Dispatchers.IO) {
            val existingSchedule = schedulesRepo.getById(scheduleId) ?: return@withContext
            val metadataJson = existingSchedule.scheduledAlarmsMetadata

            // If no metadata exists, nothing to cancel
            if (metadataJson.isNullOrEmpty()) return@withContext
            val oldAlarms = parseAlarmMetadata(metadataJson)

            // Cancel all old alarms
            oldAlarms.forEach { oldAlarm ->
                val oldIntent = Intent(context, AliasAlarmReceiver::class.java).apply {
                    action = AliasAlarmReceiver.ACTION_ALIAS
                    putExtra(AliasAlarmReceiver.EXTRA_OPERATION, oldAlarm.operation)
                    putExtra(AliasAlarmReceiver.EXTRA_CONTACT_ID, existingSchedule.contactId)
                    putExtra(AliasAlarmReceiver.EXTRA_SCHEDULE_ID, scheduleId)
                    putExtra(AliasAlarmReceiver.EXTRA_DAY_OF_WEEK, oldAlarm.dayOfWeek)
                }

                val oldPending = PendingIntent.getBroadcast(
                    context,
                    oldAlarm.requestCode,
                    oldIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                alarmManager?.cancel(oldPending)
                oldPending.cancel()
            }
        }
    }

    data class SyncResult(
        val totalSchedules: Int,
        val alarmsScheduled: Int,
        val alarmsSkipped: Int,
        val errors: Int,
        val orphanedSchedulesRemoved: Int
    )
}
