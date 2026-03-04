package com.purnendu.contactly.manager

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.purnendu.contactly.receiver.AliasAlarmReceiver
import com.purnendu.contactly.model.alarm.AlarmMetadata
import com.purnendu.contactly.model.alarm.AlarmActivationResult
import com.purnendu.contactly.model.alarm.SyncResult
import com.purnendu.contactly.data.repository.ContactsRepository
import com.purnendu.contactly.data.repository.ActivationsRepository
import com.purnendu.contactly.data.local.room.ActivationEntity
import com.purnendu.contactly.model.Contact
import com.purnendu.contactly.utils.AlarmRequestCodeUtils
import com.purnendu.contactly.utils.DayUtils
import com.purnendu.contactly.utils.ActivationMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Manages synchronization between AlarmManager and Room Database
 * Ensures database is the single source of truth for activated alarms
 * 
 * Dependencies are injected via Koin.
 */
class ContactlyAlarmManager(
    private val context: Context,
    val activationsRepo: ActivationsRepository,
    val contactsRepo: ContactsRepository
) {
    val alarmManager: AlarmManager? = context.getSystemService(AlarmManager::class.java)
    private val gson = Gson()
    private val TAG = "ContactlyAlarmManager"

    /**
     * Activate alarms for a contact with the given parameters.
     * @return AlarmActivationResult containing success status and alarm metadata
     */
    fun activateAlarms(
        contact: Contact,
        activationId: Long,
        originalName: String,
        temporaryName: String,
        tempImage: String?,
        originalImage:String?,
        startAtMillis: Long,
        endAtMillis: Long,
        selectedDays: Int,
        activationMode: ActivationMode
    ): AlarmActivationResult {
        var isAlarmSuccessfullyActivated = true
        val contactId = contact.id ?: return AlarmActivationResult(false, emptyList())

        // Extract selected days (0=Sun, 1=Mon, ...)
        val daysList = DayUtils.extractDaysFromBitmask(selectedDays)

        // If no days selected, don't activate anything
        if (daysList.isEmpty()) return AlarmActivationResult(false, emptyList())

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
                    operation = AliasAlarmReceiver.Companion.OP_APPLY,
                    triggerTimeMillis = applyAt
                )
            )
            alarmMetadataList.add(
                AlarmMetadata(
                    requestCode = revertReqCode,
                    dayOfWeek = dayOfWeek,
                    operation = AliasAlarmReceiver.Companion.OP_REVERT,
                    triggerTimeMillis = revertAt
                )
            )

            val applyIntent = Intent(context, AliasAlarmReceiver::class.java).apply {
                action = AliasAlarmReceiver.Companion.ACTION_ALIAS
                putExtra(AliasAlarmReceiver.Companion.EXTRA_OPERATION, AliasAlarmReceiver.Companion.OP_APPLY)
                putExtra(AliasAlarmReceiver.Companion.EXTRA_CONTACT_ID, contact.id)
                putExtra(AliasAlarmReceiver.Companion.EXTRA_ORIGINAL_NAME, originalName)
                putExtra(AliasAlarmReceiver.Companion.EXTRA_TEMPORARY_NAME, temporaryName)
                putExtra(AliasAlarmReceiver.Companion.EXTRA_TEMPORARY_IMAGE, tempImage)
                putExtra(AliasAlarmReceiver.Companion.EXTRA_ORIGINAL_IMAGE, originalImage)
                putExtra(AliasAlarmReceiver.Companion.EXTRA_ACTIVATION_ID, activationId)
                putExtra(AliasAlarmReceiver.Companion.EXTRA_DAY_OF_WEEK, dayOfWeek)
                putExtra(AliasAlarmReceiver.Companion.EXTRA_ACTIVATION_TYPE, ActivationMode.toInt(activationMode))
            }
            val revertIntent = Intent(context, AliasAlarmReceiver::class.java).apply {
                action = AliasAlarmReceiver.Companion.ACTION_ALIAS
                putExtra(AliasAlarmReceiver.Companion.EXTRA_OPERATION, AliasAlarmReceiver.Companion.OP_REVERT)
                putExtra(AliasAlarmReceiver.Companion.EXTRA_CONTACT_ID, contact.id)
                putExtra(AliasAlarmReceiver.Companion.EXTRA_ORIGINAL_NAME, originalName)
                putExtra(AliasAlarmReceiver.Companion.EXTRA_TEMPORARY_NAME, temporaryName)
                putExtra(AliasAlarmReceiver.Companion.EXTRA_TEMPORARY_IMAGE, tempImage)
                putExtra(AliasAlarmReceiver.Companion.EXTRA_ORIGINAL_IMAGE, originalImage)
                putExtra(AliasAlarmReceiver.Companion.EXTRA_ACTIVATION_ID, activationId)
                putExtra(AliasAlarmReceiver.Companion.EXTRA_DAY_OF_WEEK, dayOfWeek)
                putExtra(AliasAlarmReceiver.Companion.EXTRA_ACTIVATION_TYPE, ActivationMode.toInt(activationMode))
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
                Log.d(TAG, "Failed to activate APPLY alarm: ${e.localizedMessage}")
                isAlarmSuccessfullyActivated = false
            }
            
            try {
                alarmManager?.setExactAndAllowWhileIdle(
                    AlarmManager.RTC,
                    revertAt,
                    revertPending
                )
            } catch (e: SecurityException) {
                Log.d(TAG, "Failed to activate REVERT alarm: ${e.localizedMessage}")
                isAlarmSuccessfullyActivated = false
            }
        }

        return AlarmActivationResult(isAlarmSuccessfullyActivated, alarmMetadataList)
    }

    /**
     * Check if a specific alarm is activated in AlarmManager
     */
    fun isAlarmActivated(
        requestCode: Int,
        contactId: Long,
        originalName: String,
        temporaryName: String,
        tempImage: String?,
        originalImage:String?,
        operation: String,
        dayOfWeek: Int,
        activationId: Long,
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
            activationId = activationId,
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
        activationId: Long,
        activationMode: Int
    ): Intent {
        return Intent(context, AliasAlarmReceiver::class.java).apply {
            action = AliasAlarmReceiver.Companion.ACTION_ALIAS
            putExtra(AliasAlarmReceiver.Companion.EXTRA_OPERATION, operation)
            putExtra(AliasAlarmReceiver.Companion.EXTRA_CONTACT_ID, contactId)
            putExtra(AliasAlarmReceiver.Companion.EXTRA_ORIGINAL_NAME, originalName)
            putExtra(AliasAlarmReceiver.Companion.EXTRA_TEMPORARY_NAME, temporaryName)
            putExtra(AliasAlarmReceiver.Companion.EXTRA_TEMPORARY_IMAGE, tempImage)
            putExtra(AliasAlarmReceiver.Companion.EXTRA_ORIGINAL_IMAGE, originalImage)
            putExtra(AliasAlarmReceiver.Companion.EXTRA_ACTIVATION_ID, activationId)
            putExtra(AliasAlarmReceiver.Companion.EXTRA_DAY_OF_WEEK, dayOfWeek)
            putExtra(AliasAlarmReceiver.Companion.EXTRA_ACTIVATION_TYPE, activationMode)
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
     * Generate alarm metadata for an activation
     */
    fun generateAlarmMetadata(
        activation: ActivationEntity,
        selectedDays: List<Int>
    ): List<AlarmMetadata> {
        val startAt = activation.startAtMillis ?: return emptyList()
        val endAt = activation.endAtMillis ?: return emptyList()
        val metadata = mutableListOf<AlarmMetadata>()

        selectedDays.forEach { dayOfWeek ->
            // Calculate next occurrence for both times as a pair (ensures consistency)
            val (applyAt, revertAt) = DayUtils.calculateNextOccurrencePair(startAt, endAt, dayOfWeek)

            // Use centralized utility for request code generation
            val applyReqCode = AlarmRequestCodeUtils.generateApplyRequestCode(activation.contactId, dayOfWeek)
            val revertReqCode = AlarmRequestCodeUtils.generateRevertRequestCode(activation.contactId, dayOfWeek)

            metadata.add(
                AlarmMetadata(
                    requestCode = applyReqCode,
                    dayOfWeek = dayOfWeek,
                    operation = AliasAlarmReceiver.Companion.OP_APPLY,
                    triggerTimeMillis = applyAt
                )
            )
            metadata.add(
                AlarmMetadata(
                    requestCode = revertReqCode,
                    dayOfWeek = dayOfWeek,
                    operation = AliasAlarmReceiver.Companion.OP_REVERT,
                    triggerTimeMillis = revertAt
                )
            )
        }

        return metadata
    }

    /**
     * Activate a single alarm based on metadata
     */
    private fun activateAlarm(
        activation: ActivationEntity,
        metadata: AlarmMetadata
    ) {
        val intent = buildAlarmIntent(
            context = context,
            contactId = activation.contactId,
            originalName = activation.originalName,
            temporaryName = activation.temporaryName,
            originalImage = activation.originalImage,
            tempImage = activation.temporaryImage,
            operation = metadata.operation,
            dayOfWeek = metadata.dayOfWeek,
            activationId = activation.activationId,
            activationMode = activation.activationMode
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
            Log.d(TAG, "Activated alarm: reqCode=${metadata.requestCode}, day=${metadata.dayOfWeek}, op=${metadata.operation}")
        } catch (e: SecurityException) {
            Log.e(TAG, "Failed to activate alarm: reqCode=${metadata.requestCode}", e)
        }
    }

    /**
     * Sync all activations from database to AlarmManager
     * This is the main sync function called on app startup
     */
    suspend fun syncAllActivations(): SyncResult = withContext(Dispatchers.IO) {
        Log.d(TAG, "Starting alarm sync...")
        val allActivations = activationsRepo.getAllEntities().filter {
            val mode = ActivationMode.fromInt(it.activationMode)
            mode != ActivationMode.INSTANT && mode != ActivationMode.NEARBY
        }
        var activatedCount = 0
        var skippedCount = 0
        var errorCount = 0
        var orphanedCount = 0

        allActivations.forEach { activation ->
            try {
                // Verify contact still exists (only if we have permission)
                var hasContactPermission = true
                val contact = try {
                    contactsRepo.fetchContactById(activation.contactId)
                } catch (ex: SecurityException) {
                    Log.w(TAG, "No permission to verify contact existence, skipping validation")
                    hasContactPermission = false
                    null
                }

                // Only delete if we have permission and contact is genuinely missing
                // If no permission, skip validation and proceed to sync
                if (hasContactPermission && contact == null) {
                    Log.d(TAG, "Contact ${activation.contactId} not found, removing activation")
                    cancelActivatedAlarms(activation.activationId)
                    activationsRepo.deleteByContactId(activation.contactId)
                    orphanedCount++
                    return@forEach
                }

                // Parse stored metadata
                val storedMetadata = parseAlarmMetadata(activation.activatedAlarmsMetadata)

                if (storedMetadata.isEmpty()) {
                    // No metadata stored, need to regenerate and activate all
                    Log.w(TAG, "No metadata for activation ${activation.activationId}, regenerating")
                    val daysList = DayUtils.extractDaysFromBitmask(activation.selectedDays ?: return@forEach)
                    val newMetadata = generateAlarmMetadata(activation, daysList)
                    
                    // Activate all alarms
                    newMetadata.forEach { metadata ->
                        activateAlarm( activation, metadata)
                        activatedCount++
                    }
                    
                    // Update database with metadata
                    val updated = activation.copy(activatedAlarmsMetadata = toJson(newMetadata))
                    activationsRepo.update(updated)
                } else {
                    // Check each alarm and reactivate if missing
                    storedMetadata.forEach { metadata ->
                        // Skip any expired alarms - they have already been executed.
                        // For ONE_TIME: alarm already fired and activation will be deleted after REVERT.
                        // For REPEAT: metadata is updated with next week's time after each execution,
                        // so expired time means the alarm was already processed.
                        val isExpired = metadata.triggerTimeMillis < System.currentTimeMillis()

                        if (isExpired) {
                            Log.d(TAG, "Skipping expired alarm: op=${metadata.operation}, time=${metadata.triggerTimeMillis}")
                            return@forEach
                        }

                        val exists = isAlarmActivated(
                            requestCode = metadata.requestCode,
                            contactId = activation.contactId,
                            originalName = activation.originalName,
                            temporaryName = activation.temporaryName,
                            tempImage = activation.temporaryImage,
                            originalImage = activation.originalImage,
                            operation = metadata.operation,
                            dayOfWeek = metadata.dayOfWeek,
                            activationId = activation.activationId,
                            activationMode = activation.activationMode
                        )

                        if (!exists) {
                            Log.d(TAG, "Alarm missing, reactivating: reqCode=${metadata.requestCode}")
                            activateAlarm(activation, metadata)
                            activatedCount++
                        } else {
                            skippedCount++
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing activation ${activation.activationId}", e)
                errorCount++
            }
        }

        val result = SyncResult(
            totalActivations = allActivations.size,
            alarmsActivated = activatedCount,
            alarmsSkipped = skippedCount,
            errors = errorCount,
            orphanedActivationsRemoved = orphanedCount
        )
        
        Log.d(TAG, "Sync complete: $result")
        result
    }

     suspend fun cancelActivatedAlarms(activationId: Long) {
        withContext(Dispatchers.IO) {
            val existingActivation = activationsRepo.getById(activationId) ?: return@withContext
            val metadataJson = existingActivation.activatedAlarmsMetadata

            // If no metadata exists, nothing to cancel
            if (metadataJson.isNullOrEmpty()) return@withContext
            val oldAlarms = parseAlarmMetadata(metadataJson)

            // Cancel all old alarms
            oldAlarms.forEach { oldAlarm ->
                val oldIntent = Intent(context, AliasAlarmReceiver::class.java).apply {
                    action = AliasAlarmReceiver.Companion.ACTION_ALIAS
                    putExtra(AliasAlarmReceiver.Companion.EXTRA_OPERATION, oldAlarm.operation)
                    putExtra(AliasAlarmReceiver.Companion.EXTRA_CONTACT_ID, existingActivation.contactId)
                    putExtra(AliasAlarmReceiver.Companion.EXTRA_ORIGINAL_NAME, existingActivation.originalName)
                    putExtra(AliasAlarmReceiver.Companion.EXTRA_TEMPORARY_NAME, existingActivation.temporaryName)
                    putExtra(AliasAlarmReceiver.Companion.EXTRA_ORIGINAL_IMAGE, existingActivation.originalImage)
                    putExtra(AliasAlarmReceiver.Companion.EXTRA_TEMPORARY_IMAGE, existingActivation.temporaryImage)
                    putExtra(AliasAlarmReceiver.Companion.EXTRA_ACTIVATION_ID, activationId)
                    putExtra(AliasAlarmReceiver.Companion.EXTRA_DAY_OF_WEEK, oldAlarm.dayOfWeek)
                    putExtra(AliasAlarmReceiver.Companion.EXTRA_ACTIVATION_TYPE, existingActivation.activationMode)
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
        val reqCode = if (operation == AliasAlarmReceiver.Companion.OP_APPLY) {
            AlarmRequestCodeUtils.generateApplyRequestCode(contactId, dayOfWeek)
        } else {
            AlarmRequestCodeUtils.generateRevertRequestCode(contactId, dayOfWeek)
        }

        val intent = Intent(context, AliasAlarmReceiver::class.java).apply {
            action = AliasAlarmReceiver.Companion.ACTION_ALIAS
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
