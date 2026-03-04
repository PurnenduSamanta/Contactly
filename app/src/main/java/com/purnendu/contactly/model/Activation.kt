package com.purnendu.contactly.model

import androidx.annotation.DrawableRes
import com.purnendu.contactly.utils.ActivationMode

data class Activation(
    val id: String,
    val name: String,
    val originalName: String,
    @DrawableRes val avatarResId: Int?,
    val contactId: Long?,
    val selectedDays: Int?,
    val startAtMillis: Long?,
    val endAtMillis: Long?,
    val activationMode: ActivationMode,
    val isCurrentlyActive: Boolean = false,  // For ONE_TIME/REPEAT: between APPLY and REVERT; For INSTANT: manually toggled
    val temporaryImageUri: String?,
    val originalImageUri: String?,
    // Nearby (geofence) fields
    val latitude: Double? = null,
    val longitude: Double? = null,
    val radiusMeters: Float? = null,
    val locationLabel: String? = null
)
