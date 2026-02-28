package com.purnendu.contactly.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.purnendu.contactly.alarm.models.AlarmMetadata
import com.purnendu.contactly.alarm.models.AlarmScheduleResult
import com.purnendu.contactly.alarm.models.SyncResult
import com.purnendu.contactly.data.repository.ContactsRepository
import com.purnendu.contactly.data.repository.SchedulesRepository
import com.purnendu.contactly.data.local.room.ScheduleEntity
import com.purnendu.contactly.model.Contact
import com.purnendu.contactly.utils.AlarmRequestCodeUtils
import com.purnendu.contactly.utils.DayUtils
import com.purnendu.contactly.utils.ActivationMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Manages synchronization between AlarmManager and Room Database
 * Ensures database is the single source of truth for scheduled alarms
 * 
 * Dependencies are injected via Koin.
 */
class ContactlyAlarmManager(
    private val context: Context,
    val schedulesRepo: SchedulesRepository,
    val contactsRepo: ContactsRepository
) {
    val alarmManager: AlarmManager? = context.getSystemService(AlarmManager::class.java)
    private val gson = Gson()
    private val TAG = "ContactlyAlarmManager"

    /**
     * Schedule alarms for a contact with the given parameters.
     * @return AlarmScheduleResult containing success status and alarm metadata
     */
    fun scheduleAlarms(
        contact: Contact,
        scheduleId: Long,
        originalName: String,
        temporaryName: String,
        tempImage: String?,
        originalImage:String?,
        startAtMillis: Long,
        endAtMillis: Long,
        selectedDays: Int,
        activationMode: ActivationMode
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
            // Calculate next occurrence for both times as a pair (ensures consistency)
            val (applyAt, revertAt) = DayUtils.calculateNextOccurrencePair(startAtMillis, endAtMillis, dayOfWeek)

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
                putExtra(AliasAlarmReceiver.EXTRA_TEMPORARY_IMAGE, tempImage)
                putExtra(AliasAlarmReceiver.EXTRA_ORIGINAL_IMAGE, originalImage)
                putExtra(AliasAlarmReceiver.EXTRA_SCHEDULE_ID, scheduleId)
                putExtra(AliasAlarmReceiver.EXTRA_DAY_OF_WEEK, dayOfWeek)
                putExtra(AliasAlarmReceiver.EXTRA_SCHEDULE_TYPE, ActivationMode.toInt(activationMode))
            }
            val revertIntent = Intent(context, AliasAlarmReceiver::class.java).apply {
                action = AliasAlarmReceiver.ACTION_ALIAS
                putExtra(AliasAlarmReceiver.EXTRA_OPERATION, AliasAlarmReceiver.OP_REVERT)
                putExtra(AliasAlarmReceiver.EXTRA_CONTACT_ID, contact.id)
                putExtra(AliasAlarmReceiver.EXTRA_ORIGINAL_NAME, originalName)
                putExtra(AliasAlarmReceiver.EXTRA_TEMPORARY_NAME, temporaryName)
                putExtra(AliasAlarmReceiver.EXTRA_TEMPORARY_IMAGE, tempImage)
                putExtra(AliasAlarmReceiver.EXTRA_ORIGINAL_IMAGE, originalImage)
                putExtra(AliasAlarmReceiver.EXTRA_SCHEDULE_ID, scheduleId)
                putExtra(AliasAlarmReceiver.EXTRA_DAY_OF_WEEK, dayOfWeek)
                putExtra(AliasAlarmReceiver.EXTRA_SCHEDULE_TYPE, ActivationMode.toInt(activationMode))
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
                Log.d(TAG, "Failed to schedule APPLY alarm: ${e.localizedMessage}")
                isAlarmSuccessfullyScheduled = false
            }
            
            try {
                alarmManager?.setExactAndAllowWhileIdle(
                    AlarmManager.RTC,
                    revertAt,
                    revertPending
                )
            } catch (e: SecurityException) {
                Log.d(TAG, "Failed to schedule REVERT alarm: ${e.localizedMessage}")
                isAlarmSuccessfullyScheduled = false
            }
        }

        return AlarmScheduleResult(isAlarmSuccessfullyScheduled, alarmMetadataList)
    }

    /**
     * Check if a specific alarm is scheduled in AlarmManager
     */
    fun isAlarmScheduled(
        requestCode: Int,
        contactId: Long,
        originalName: String,
        temporaryName: String,
        tempImage: String?,
        originalImage:String?,
        operation: String,
        dayOfWeek: Int,
        scheduleId: Long,
        activationMode: Int
    ): Boolean {
        val intent = buildAlarmIntent(
            context = context,
            contactId = contactId,
            originalName = originalName,
            temporaryName = temporaryName,
            tempImage = tempImage,
            originalImage = originalImage,
            operation = operation,
            dayOfWeek = dayOfWeek,
            scheduleId = scheduleId,
            activationMode = activationMode
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
        originalName: String,
        temporaryName: String,
        tempImage: String?,
        originalImage: String?,
        operation: String,
        dayOfWeek: Int,
        scheduleId: Long,
        activationMode: Int
    ): Intent {
        return Intent(context, AliasAlarmReceiver::class.java).apply {
            action = AliasAlarmReceiver.ACTION_ALIAS
            putExtra(AliasAlarmReceiver.EXTRA_OPERATION, operation)
            putExtra(AliasAlarmReceiver.EXTRA_CONTACT_ID, contactId)
            putExtra(AliasAlarmReceiver.EXTRA_ORIGINAL_NAME, originalName)
            putExtra(AliasAlarmReceiver.EXTRA_TEMPORARY_NAME, temporaryName)
            putExtra(AliasAlarmReceiver.EXTRA_TEMPORARY_IMAGE, tempImage)
            putExtra(AliasAlarmReceiver.EXTRA_ORIGINAL_IMAGE, originalImage)
            putExtra(AliasAlarmReceiver.EXTRA_SCHEDULE_ID, scheduleId)
            putExtra(AliasAlarmReceiver.EXTRA_DAY_OF_WEEK, dayOfWeek)
            putExtra(AliasAlarmReceiver.EXTRA_SCHEDULE_TYPE, activationMode)
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
        val startAt = schedule.startAtMillis ?: return emptyList()
        val endAt = schedule.endAtMillis ?: return emptyList()
        val metadata = mutableListOf<AlarmMetadata>()

        selectedDays.forEach { dayOfWeek ->
            // Calculate next occurrence for both times as a pair (ensures consistency)
            val (applyAt, revertAt) = DayUtils.calculateNextOccurrencePair(startAt, endAt, dayOfWeek)

            // Use centralized utility for request code generation
            val applyReqCode = AlarmRequestCodeUtils.generateApplyRequestCode(schedule.contactId, dayOfWeek)
            val revertReqCode = AlarmRequestCodeUtils.generateRevertRequestCode(schedule.contactId, dayOfWeek)

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
        val intent = buildAlarmIntent(
            context = context,
            contactId = schedule.contactId,
            originalName = schedule.originalName,
            temporaryName = schedule.temporaryName,
            originalImage = schedule.originalImage,
            tempImage = schedule.temporaryImage,
            operation = metadata.operation,
            dayOfWeek = metadata.dayOfWeek,
            scheduleId = schedule.scheduleId,
            activationMode = schedule.activationMode
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
        val allSchedules = schedulesRepo.getAllEntities().filter {
            ActivationMode.fromInt(it.activationMode) != ActivationMode.INSTANT
        }
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
                    val daysList = DayUtils.extractDaysFromBitmask(schedule.selectedDays ?: return@forEach)
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
                        // Skip any expired alarms - they have already been executed.
                        // For ONE_TIME: alarm already fired and schedule will be deleted after REVERT.
                        // For REPEAT: metadata is updated with next week's time after each execution,
                        // so expired time means the alarm was already processed.
                        val isExpired = metadata.triggerTimeMillis < System.currentTimeMillis()

                        if (isExpired) {
                            Log.d(TAG, "Skipping expired alarm: op=${metadata.operation}, time=${metadata.triggerTimeMillis}")
                            return@forEach
                        }

                        val exists = isAlarmScheduled(
                            requestCode = metadata.requestCode,
                            contactId = schedule.contactId,
                            originalName = schedule.originalName,
                            temporaryName = schedule.temporaryName,
                            tempImage = schedule.temporaryImage,
                            originalImage = schedule.originalImage,
                            operation = metadata.operation,
                            dayOfWeek = metadata.dayOfWeek,
                            scheduleId = schedule.scheduleId,
                            activationMode = schedule.activationMode
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
                    putExtra(AliasAlarmReceiver.EXTRA_ORIGINAL_NAME, existingSchedule.originalName)
                    putExtra(AliasAlarmReceiver.EXTRA_TEMPORARY_NAME, existingSchedule.temporaryName)
                    putExtra(AliasAlarmReceiver.EXTRA_ORIGINAL_IMAGE, existingSchedule.originalImage)
                    putExtra(AliasAlarmReceiver.EXTRA_TEMPORARY_IMAGE, existingSchedule.temporaryImage)
                    putExtra(AliasAlarmReceiver.EXTRA_SCHEDULE_ID, scheduleId)
                    putExtra(AliasAlarmReceiver.EXTRA_DAY_OF_WEEK, oldAlarm.dayOfWeek)
                    putExtra(AliasAlarmReceiver.EXTRA_SCHEDULE_TYPE, existingSchedule.activationMode)
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

    /**
     * Cancel a specific alarm PendingIntent.
     * This is used to clean up "Apply" alarms after they fire, so they don't show as active.
     */
    fun cancelSpecificAlarm(
        contactId: Long,
        dayOfWeek: Int,
        operation: String
    ) {
        val reqCode = if (operation == AliasAlarmReceiver.OP_APPLY) {
            AlarmRequestCodeUtils.generateApplyRequestCode(contactId, dayOfWeek)
        } else {
            AlarmRequestCodeUtils.generateRevertRequestCode(contactId, dayOfWeek)
        }

        val intent = Intent(context, AliasAlarmReceiver::class.java).apply {
            action = AliasAlarmReceiver.ACTION_ALIAS
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reqCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )

        pendingIntent?.cancel()
        Log.d(TAG, "Cancelled specific alarm PendingIntent: reqCode=$reqCode, op=$operation")
    }
}
