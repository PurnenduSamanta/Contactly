package com.purnendu.contactly.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import com.purnendu.contactly.common.StatusEventBus
import com.purnendu.contactly.domain.repository.AppPreferences
import com.purnendu.contactly.data.repository.ContactsRepository
import com.purnendu.contactly.data.repository.ActivationsRepository
import com.purnendu.contactly.notification.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * BroadcastReceiver for geofence transition events.
 *
 * Handles ENTER/EXIT transitions for NEARBY activations:
 * - ENTER: Apply temporary name/photo, notify UI via EventBus
 * - EXIT: Revert original name/photo, notify UI via EventBus
 *
 * Active status is determined by checking the contact's current name
 * in the phone's contacts DB (single source of truth).
 */
class GeofenceBroadcastReceiver : BroadcastReceiver(), KoinComponent {

    private val activationRepo: ActivationsRepository by inject()
    private val contactsRepo: ContactsRepository by inject()
    private val appPreferences: AppPreferences by inject()

    companion object {
        private const val TAG = "GeofenceReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val geofencingEvent = GeofencingEvent.fromIntent(intent)
        if (geofencingEvent == null) {
            Log.e(TAG, "GeofencingEvent is null")
            return
        }
        if (geofencingEvent.hasError()) {
            Log.e(TAG, "Geofence error code: ${geofencingEvent.errorCode}")
            return
        }

        val transitionType = geofencingEvent.geofenceTransition
        if (transitionType != Geofence.GEOFENCE_TRANSITION_ENTER &&
            transitionType != Geofence.GEOFENCE_TRANSITION_EXIT
        ) {
            Log.w(TAG, "Unexpected transition type: $transitionType")
            return
        }

        val triggeringGeofences = geofencingEvent.triggeringGeofences
        if (triggeringGeofences.isNullOrEmpty()) {
            Log.w(TAG, "No triggering geofences")
            return
        }

        val isEnter = transitionType == Geofence.GEOFENCE_TRANSITION_ENTER
        val op = if (isEnter) AliasAlarmReceiver.Companion.OP_APPLY else AliasAlarmReceiver.Companion.OP_REVERT
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                triggeringGeofences.forEach { geofence ->
                    val activationId = geofence.requestId.toLongOrNull()
                    if (activationId == null) {
                        Log.w(TAG, "Invalid geofence requestId: ${geofence.requestId}")
                        return@forEach
                    }

                    val activation = activationRepo.getById(activationId)
                    if (activation == null) {
                        Log.w(TAG, "Activation not found: $activationId")
                        return@forEach
                    }

                    Log.d(TAG, "Geofence ${if (isEnter) "ENTER" else "EXIT"} for activation $activationId")

                    // 1. Apply or revert contact identity
                    try {
                        contactsRepo.applyContact(
                            contactId = activation.contactId,
                            name = if (isEnter) activation.temporaryName else activation.originalName,
                            filePath = if (isEnter) activation.temporaryImage else activation.originalImage,
                            shouldRemovePhoto = if (isEnter) activation.temporaryImage == null
                                                else activation.originalImage == null
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to apply contact for activation $activationId", e)
                    }

                    // 2. Show notification
                    val notificationsEnabled = try {
                        appPreferences.notificationsEnabledFlow.first()
                    } catch (e: Exception) { false }

                    if (notificationsEnabled) {
                        withContext(Dispatchers.Main) {
                            try {
                                NotificationHelper.showAlarmNotification(
                                    context = context,
                                    originalName = activation.originalName,
                                    temporaryName = activation.temporaryName,
                                    isApply = isEnter,
                                    activationMode = activation.activationMode,
                                    contactImage = if (isEnter) activation.temporaryImage
                                                   else activation.originalImage
                                )
                            } catch (e: Exception) {
                                Log.e(TAG, "Failed to show notification", e)
                            }
                        }
                    }

                    // 3. Notify UI via EventBus (triggers re-fetch → contact name check → active status)
                    StatusEventBus.notifyAlarmFired(activationId, op)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing geofence event", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}

