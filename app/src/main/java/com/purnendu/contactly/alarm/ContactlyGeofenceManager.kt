package com.purnendu.contactly.alarm

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.purnendu.contactly.data.repository.SchedulesRepository
import com.purnendu.contactly.utils.ActivationMode
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

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
     * Check if location permissions are granted at runtime.
     * Returns true only if BOTH fine and coarse location are granted.
     */
    fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarse = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        return fine && coarse
    }

    /**
     * Register a geofence for a NEARBY schedule.
     * Returns true if registration succeeds, false otherwise.
     * Dynamically checks location permission — returns false if not granted
     * instead of silently failing.
     */
    suspend fun registerGeofence(
        scheduleId: Long,
        latitude: Double,
        longitude: Double,
        radiusMeters: Float
    ): Boolean {
        // Dynamic permission check — prevents silent failure
        if (!hasLocationPermission()) {
            Log.w(TAG, "Cannot register geofence $scheduleId: location permission not granted")
            return false
        }

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

        return try {
            suspendCancellableCoroutine { continuation ->
                @Suppress("MissingPermission") // Already checked above
                geofencingClient.addGeofences(request, getGeofencePendingIntent())
                    .addOnSuccessListener {
                        Log.d(TAG, "Geofence registered: $scheduleId (lat=$latitude, lng=$longitude, r=$radiusMeters)")
                        continuation.resume(true)
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Failed to register geofence: $scheduleId", e)
                        continuation.resume(false)
                    }
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException: permission revoked during registration for $scheduleId", e)
            false
        }
    }

    /**
     * Unregister a geofence for a NEARBY schedule.
     * Returns true if unregistration succeeds, false otherwise.
     */
    suspend fun unregisterGeofence(scheduleId: Long): Boolean {
        return suspendCancellableCoroutine { continuation ->
            geofencingClient.removeGeofences(listOf(scheduleId.toString()))
                .addOnSuccessListener {
                    Log.d(TAG, "Geofence unregistered: $scheduleId")
                    continuation.resume(true)
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed to unregister geofence: $scheduleId", e)
                    continuation.resume(false)
                }
        }
    }

    /**
     * Re-register all NEARBY geofences.
     * Called after boot or app update since geofences don't survive restarts.
     * Dynamically checks permission — skips ALL geofences if permission is missing.
     */
    suspend fun syncAllGeofences() {
        // Check permission BEFORE iterating — no point trying if permission is gone
        if (!hasLocationPermission()) {
            Log.w(TAG, "Cannot sync geofences: location permission not granted. Skipping all.")
            return
        }

        val nearbySchedules = schedulesRepo.getAllEntities().filter {
            ActivationMode.fromInt(it.activationMode) == ActivationMode.NEARBY
        }

        var registeredCount = 0
        nearbySchedules.forEach { schedule ->
            if (schedule.latitude != null && schedule.longitude != null
                && schedule.radiusMeters != null
            ) {
                try {
                    val success = registerGeofence(
                        scheduleId = schedule.scheduleId,
                        latitude = schedule.latitude,
                        longitude = schedule.longitude,
                        radiusMeters = schedule.radiusMeters
                    )
                    if (success) registeredCount++
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
