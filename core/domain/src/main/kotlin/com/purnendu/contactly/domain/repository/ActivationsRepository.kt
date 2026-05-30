package com.purnendu.contactly.domain.repository

import com.purnendu.contactly.domain.model.Activation
import com.purnendu.contactly.domain.model.ActivationRecord
import kotlinx.coroutines.flow.Flow

interface ActivationsRepository {
    fun getActivations(): Flow<List<Activation>>
    suspend fun create(record: ActivationRecord): Long
    suspend fun update(record: ActivationRecord)
    suspend fun deleteById(id: Long)
    suspend fun getById(id: Long): ActivationRecord?
    suspend fun getAllRecords(): List<ActivationRecord>
    suspend fun deleteByContactId(contactId: Long)
}
