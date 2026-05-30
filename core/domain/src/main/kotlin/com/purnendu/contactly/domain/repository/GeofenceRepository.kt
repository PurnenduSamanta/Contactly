package com.purnendu.contactly.domain.repository

interface GeofenceRepository {
    fun hasBackgroundLocationPermission(): Boolean
    suspend fun registerGeofence(
        activationId: Long,
        latitude: Double,
        longitude: Double,
        radiusMeters: Float
    ): Boolean

    suspend fun unregisterGeofence(activationId: Long): Boolean
    suspend fun syncAllGeofences()
}
