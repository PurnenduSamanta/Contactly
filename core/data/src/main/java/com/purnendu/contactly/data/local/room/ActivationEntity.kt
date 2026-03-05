package com.purnendu.contactly.data.local.room

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "activation")
data class ActivationEntity(
    @PrimaryKey(autoGenerate = false) val activationId: Long,
    val contactId: Long,
    val contactLookupKey: String?,
    val originalName: String,
    val temporaryName: String,
    val temporaryImage: String? = null,
    val originalImage: String? = null,
    val startAtMillis: Long?,
    val endAtMillis: Long?,
    val selectedDays: Int?,
    val activatedAlarmsMetadata: String? = null,  // JSON array of AlarmMetadata
    val activationMode: Int, // 0 = ONE_TIME, 1 = REPEAT, 2 = INSTANT, 3 = NEARBY
    val instantSwitchStatus: Boolean? = null, // Only for INSTANT: true = applied, false = not applied, null = N/A
    // Nearby (geofence) fields
    val latitude: Double? = null,
    val longitude: Double? = null,
    val radiusMeters: Float? = null,
    val locationLabel: String? = null
)
