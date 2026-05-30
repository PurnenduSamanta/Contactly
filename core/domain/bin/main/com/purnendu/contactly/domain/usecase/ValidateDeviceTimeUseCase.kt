package com.purnendu.contactly.domain.usecase

import com.purnendu.contactly.domain.model.TimeValidationResult
import com.purnendu.contactly.domain.repository.TimeValidationRepository

class ValidateDeviceTimeUseCase(
    private val timeValidationRepository: TimeValidationRepository
) {
    suspend operator fun invoke(): TimeValidationResult {
        return timeValidationRepository.validateDeviceTime()
    }
}
