package com.purnendu.contactly.domain.usecase

import com.purnendu.contactly.common.ActivationMode
import com.purnendu.contactly.domain.model.Contact

data class ActivationCommand(
    val contact: Contact,
    val activationId: Long,
    val temporaryName: String,
    val tempImageUri: String?,
    val startAtMillis: Long? = null,
    val endAtMillis: Long? = null,
    val selectedDays: Int? = null,
    val activationMode: ActivationMode,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val radiusMeters: Float? = null,
    val locationLabel: String? = null
)

data class ActivationUseCaseResult(
    val success: Boolean,
    val errorMessage: String? = null
)
