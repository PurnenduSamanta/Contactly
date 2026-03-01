package com.purnendu.contactly.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import com.purnendu.contactly.alarm.AliasAlarmReceiver.Companion.OP_APPLY
import com.purnendu.contactly.alarm.AliasAlarmReceiver.Companion.OP_REVERT
import com.purnendu.contactly.data.local.preferences.AppPreferences
import com.purnendu.contactly.data.repository.ContactsRepository
import com.purnendu.contactly.data.repository.SchedulesRepository
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
 * Handles ENTER/EXIT transitions for NEARBY schedules:
 * - ENTER: Apply temporary name/photo, notify UI via EventBus
 * - EXIT: Revert original name/photo, notify UI via EventBus
 *
 * Active status is determined by checking the contact's current name
 * in the phone's contacts DB (single source of truth).
 */
class GeofenceBroadcastReceiver : BroadcastReceiver(), KoinComponent {

    private val schedulesRepo: SchedulesRepository by inject()
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
        val op = if (isEnter) OP_APPLY else OP_REVERT
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                triggeringGeofences.forEach { geofence ->
                    val scheduleId = geofence.requestId.toLongOrNull()
                    if (scheduleId == null) {
                        Log.w(TAG, "Invalid geofence requestId: ${geofence.requestId}")
                        return@forEach
                    }

                    val schedule = schedulesRepo.getById(scheduleId)
                    if (schedule == null) {
                        Log.w(TAG, "Schedule not found: $scheduleId")
                        return@forEach
                    }

                    Log.d(TAG, "Geofence ${if (isEnter) "ENTER" else "EXIT"} for schedule $scheduleId")

                    // 1. Apply or revert contact identity
                    try {
                        contactsRepo.applyContact(
                            contactId = schedule.contactId,
                            name = if (isEnter) schedule.temporaryName else schedule.originalName,
                            filePath = if (isEnter) schedule.temporaryImage else schedule.originalImage,
                            shouldRemovePhoto = if (isEnter) schedule.temporaryImage == null
                                                else schedule.originalImage == null
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to apply contact for schedule $scheduleId", e)
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
                                    originalName = schedule.originalName,
                                    temporaryName = schedule.temporaryName,
                                    isApply = isEnter,
                                    activationMode = schedule.activationMode,
                                    contactImage = if (isEnter) schedule.temporaryImage
                                                   else schedule.originalImage
                                )
                            } catch (e: Exception) {
                                Log.e(TAG, "Failed to show notification", e)
                            }
                        }
                    }

                    // 3. Notify UI via EventBus (triggers re-fetch → contact name check → active status)
                    AlarmEventBus.notifyAlarmFired(scheduleId, op)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing geofence event", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}

