package com.purnendu.contactly.domain.usecase

import com.purnendu.contactly.domain.model.Activation
import com.purnendu.contactly.domain.model.ActivationRecord
import com.purnendu.contactly.domain.repository.ActivationsRepository
import kotlinx.coroutines.flow.Flow

class GetActivationsUseCase(
    private val activationsRepository: ActivationsRepository
) {
    operator fun invoke(): Flow<List<Activation>> = activationsRepository.getActivations()

    suspend fun records(): List<ActivationRecord> = activationsRepository.getAllRecords()
}
