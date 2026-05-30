package com.purnendu.contactly.domain.repository

import com.purnendu.contactly.domain.model.TimeValidationResult

interface TimeValidationRepository {
    suspend fun validateDeviceTime(): TimeValidationResult
}
