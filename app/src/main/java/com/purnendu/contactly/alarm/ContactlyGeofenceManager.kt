package com.purnendu.contactly.alarm

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.purnendu.contactly.data.repository.SchedulesRepository
import com.purnendu.contactly.utils.ActivationMode

/**
 * Manages geofence registration/unregistration for NEARBY schedules.
 *
 * Uses Google Play Services GeofencingClient — no Maps SDK or API key needed.
 * Geofences trigger GeofenceBroadcastReceiver on ENTER/EXIT transitions.
 */
class ContactlyGeofenceManager(
    private val context: Context,
    private val schedulesRepo: SchedulesRepository
) {
    private val geofencingClient = LocationServices.getGeofencingClient(context)

    companion object {
        private const val TAG = "GeofenceManager"
        private const val GEOFENCE_PENDING_INTENT_CODE = 9999
    }

    /**
     * Register a geofence for a NEARBY schedule.
     * Caller must ensure location permissions are granted.
     */
    @SuppressLint("MissingPermission")
    fun registerGeofence(
        scheduleId: Long,
        latitude: Double,
        longitude: Double,
        radiusMeters: Float
    ) {
        val geofence = Geofence.Builder()
            .setRequestId(scheduleId.toString())
            .setCircularRegion(latitude, longitude, radiusMeters)
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(
                Geofence.GEOFENCE_TRANSITION_ENTER or
                Geofence.GEOFENCE_TRANSITION_EXIT
            )
            .build()

        val request = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofence(geofence)
            .build()

        geofencingClient.addGeofences(request, getGeofencePendingIntent())
            .addOnSuccessListener {
                Log.d(TAG, "Geofence registered: $scheduleId (lat=$latitude, lng=$longitude, r=$radiusMeters)")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to register geofence: $scheduleId", e)
            }
    }

    /**
     * Unregister a geofence for a NEARBY schedule.
     */
    fun unregisterGeofence(scheduleId: Long) {
        geofencingClient.removeGeofences(listOf(scheduleId.toString()))
            .addOnSuccessListener {
                Log.d(TAG, "Geofence unregistered: $scheduleId")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to unregister geofence: $scheduleId", e)
            }
    }

    /**
     * Re-register all NEARBY geofences.
     * Called after boot or app update since geofences don't survive restarts.
     */
    @SuppressLint("MissingPermission")
    suspend fun syncAllGeofences() {
        val nearbySchedules = schedulesRepo.getAllEntities().filter {
            ActivationMode.fromInt(it.activationMode) == ActivationMode.NEARBY
        }

        var registeredCount = 0
        nearbySchedules.forEach { schedule ->
            if (schedule.latitude != null && schedule.longitude != null
                && schedule.radiusMeters != null
            ) {
                try {
                    registerGeofence(
                        scheduleId = schedule.scheduleId,
                        latitude = schedule.latitude,
                        longitude = schedule.longitude,
                        radiusMeters = schedule.radiusMeters
                    )
                    registeredCount++
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to sync geofence: ${schedule.scheduleId}", e)
                }
            }
        }
        Log.d(TAG, "Synced $registeredCount / ${nearbySchedules.size} NEARBY geofences")
    }

    private fun getGeofencePendingIntent(): PendingIntent {
        val intent = Intent(context, GeofenceBroadcastReceiver::class.java)
        return PendingIntent.getBroadcast(
            context,
            GEOFENCE_PENDING_INTENT_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
    }
}
