package com.purnendu.contactly.receiver

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.purnendu.contactly.common.StatusEventBus
import com.purnendu.contactly.data.repository.ContactsRepository
import com.purnendu.contactly.data.repository.ActivationsRepository
import com.purnendu.contactly.domain.repository.AppPreferences
import com.purnendu.contactly.manager.ContactlyAlarmManager
import com.purnendu.contactly.notification.NotificationHelper
import com.purnendu.contactly.common.AlarmRequestCodeUtils
import com.purnendu.contactly.utils.ImageStorageManager
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
    
    private val activationRepo: ActivationsRepository by inject()
    private val contactsRepo: ContactsRepository by inject()
    private val contactlyAlarmManager: ContactlyAlarmManager by inject()
    private val appPreferences: AppPreferences by inject()
    private val imageStorageManager: ImageStorageManager by inject()
    
    @OptIn(DelicateCoroutinesApi::class)
    override fun onReceive(context: Context, intent: Intent) {
        val op = intent.getStringExtra(EXTRA_OPERATION) ?: return
        val contactId = intent.getLongExtra(EXTRA_CONTACT_ID, -1L)
        val originalName = intent.getStringExtra(EXTRA_ORIGINAL_NAME) ?: return
        val temporaryName = intent.getStringExtra(EXTRA_TEMPORARY_NAME) ?: return
        val tempImage = intent.getStringExtra(EXTRA_TEMPORARY_IMAGE)
        val originalImage = intent.getStringExtra(EXTRA_ORIGINAL_IMAGE)
        val activationId = intent.getLongExtra(EXTRA_ACTIVATION_ID, -1L)
        val dayOfWeek = intent.getIntExtra(EXTRA_DAY_OF_WEEK, -1)
        val activationMode = intent.getIntExtra(EXTRA_ACTIVATION_TYPE, 0) // 0 = ONE_TIME, 1 = REPEAT
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

                //apply operation on the contact
                contactsRepo.applyContact(
                    contactId= contactId,
                    name= nameToApply,
                    filePath = if(isApply) tempImage else originalImage,
                    shouldRemovePhoto = if(isApply) tempImage == null else originalImage == null
                )

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
                                activationMode = activationMode,
                                contactImage = if(isApply) tempImage else originalImage
                            )
                        } catch (e: Exception) {
                            Log.e("AliasAlarmReceiver", "Failed to show notification", e)
                        }
                    }
                }

                // For one-time activations: delete from database (IO thread)
                if (activationMode == 0 && op == OP_REVERT && activationId > 0) {
                    try {
                        // Delete associated images first
                        imageStorageManager.deleteImagesFromActivation(activationId)
                        Log.d("AliasAlarmReceiver", "Deleted images for one-time activation: $activationId")
                        
                        // Then delete from database
                        activationRepo.deleteById(activationId)
                        Log.d("AliasAlarmReceiver", "Deleted one-time activation: $activationId")
                    } catch (e: Exception) {
                        Log.e("AliasAlarmReceiver", "Failed to delete one-time activation: $activationId", e)
                    }
                }


                // Only reactivate for next week if this is a REPEAT activation (activationMode == 1)
                if (activationMode == 1 && dayOfWeek >= 0) {
                    reActivateForNextWeek(context, intent, dayOfWeek)
                }
                
                // Notify UI to refresh active status
                StatusEventBus.notifyAlarmFired(activationId, op)
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

    private suspend fun reActivateForNextWeek(context: Context, originalIntent: Intent, dayOfWeek: Int) {
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
            putExtra(EXTRA_ORIGINAL_IMAGE, originalIntent.getStringExtra(EXTRA_ORIGINAL_IMAGE))
            putExtra(EXTRA_TEMPORARY_IMAGE, originalIntent.getStringExtra(EXTRA_TEMPORARY_IMAGE))
            putExtra(EXTRA_ACTIVATION_ID, originalIntent.getLongExtra(EXTRA_ACTIVATION_ID, -1L))
            putExtra(EXTRA_DAY_OF_WEEK, dayOfWeek)
            putExtra(EXTRA_ACTIVATION_TYPE, originalIntent.getIntExtra(EXTRA_ACTIVATION_TYPE, 0)) // Default to ONE-TIME
        }

        // Generate request code using centralized utility
        val contactId = originalIntent.getLongExtra(EXTRA_CONTACT_ID, -1L)
        val activationId = originalIntent.getLongExtra(EXTRA_ACTIVATION_ID, -1L)
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
            Log.d("AliasAlarmReceiver", "Re-activated alarm for next week: day=$dayOfWeek, time=$nextTriggerTime")
            
            // Update the database with new metadata and nearest trigger times
            updateActivation(activationId, reqCode, op, nextTriggerTime)
            
        } catch (e: SecurityException) {
            Log.e("AliasAlarmReceiver", "Failed to re-activate alarm", e)
        }
    }
    
    /**
     * Updates the activation's metadata in the database after re-activating.
     * 1. Updates the specific alarm's trigger time in metadata
     * 2. Finds the nearest APPLY time and updates startAtMillis
     * 3. Finds the nearest REVERT time and updates endAtMillis
     */
    private suspend fun updateActivation(
        activationId: Long,
        requestCode: Int,
        operation: String,
        newTriggerTime: Long
    ) {
        try {
            val activation = activationRepo.getById(activationId) ?: run {
                Log.w("AliasAlarmReceiver", "Activation not found for update: $activationId")
                return
            }
            
            // Parse existing metadata
            val existingMetadata = contactlyAlarmManager.parseAlarmMetadata(activation.activatedAlarmsMetadata)
            if (existingMetadata.isEmpty()) {
                Log.w("AliasAlarmReceiver", "No metadata found for activation: $activationId")
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
                ?.triggerTimeMillis ?: activation.startAtMillis
            
            // Find the nearest (soonest) REVERT time among all metadata
            val nearestRevertTime = updatedMetadata
                .filter { it.operation == OP_REVERT }
                .minByOrNull { it.triggerTimeMillis }
                ?.triggerTimeMillis ?: activation.endAtMillis
            
            // Update the activation with new metadata and nearest times
            val updatedActivation = activation.copy(
                activatedAlarmsMetadata = contactlyAlarmManager.toJson(updatedMetadata),
                startAtMillis = nearestApplyTime,
                endAtMillis = nearestRevertTime
            )
            
            activationRepo.update(updatedActivation)
            Log.d("AliasAlarmReceiver", "Updated activation metadata: id=$activationId, " +
                    "nearestApply=$nearestApplyTime, nearestRevert=$nearestRevertTime")
            
        } catch (e: Exception) {
            Log.e("AliasAlarmReceiver", "Failed to update activation metadata: $activationId", e)
        }
    }





    companion object {
        const val ACTION_ALIAS = "com.purnendu.contactly.action.ALIAS"
        const val EXTRA_OPERATION = "operation"
        const val EXTRA_CONTACT_ID = "contactId"
        const val EXTRA_ORIGINAL_NAME = "originalName"
        const val EXTRA_TEMPORARY_NAME = "temporaryName"
        const val EXTRA_TEMPORARY_IMAGE = "temporaryImage"

        const val EXTRA_ORIGINAL_IMAGE = "originalImage"
        const val EXTRA_ACTIVATION_ID = "scheduleId"
        const val EXTRA_DAY_OF_WEEK = "dayOfWeek"
        const val EXTRA_ACTIVATION_TYPE = "scheduleType"
        const val OP_APPLY = "apply"
        const val OP_REVERT = "revert"
    }
}