package com.purnendu.contactly.domain.model

import com.purnendu.contactly.common.ActivationMode

data class ActivationRecord(
    val activationId: Long,
    val contactId: Long,
    val contactLookupKey: String?,
    val originalName: String,
    val temporaryName: String,
    val temporaryImage: String? = null,
    val originalImage: String? = null,
    val startAtMillis: Long?,
    val endAtMillis: Long?,
    val selectedDays: Int?,
    val activatedAlarmsMetadata: String? = null,
    val activationMode: ActivationMode,
    val instantSwitchStatus: Boolean? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val radiusMeters: Float? = null,
    val locationLabel: String? = null
)
