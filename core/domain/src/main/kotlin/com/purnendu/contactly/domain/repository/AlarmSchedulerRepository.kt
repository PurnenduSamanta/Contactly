package com.purnendu.contactly.domain.repository

import com.purnendu.contactly.domain.model.Contact
import com.purnendu.contactly.domain.model.alarm.AlarmActivationResult
import com.purnendu.contactly.domain.model.alarm.AlarmMetadata
import com.purnendu.contactly.domain.model.alarm.SyncResult
import com.purnendu.contactly.common.ActivationMode

interface AlarmSchedulerRepository {
    fun activateAlarms(
        contact: Contact,
        activationId: Long,
        originalName: String,
        temporaryName: String,
        tempImage: String?,
        originalImage: String?,
        startAtMillis: Long,
        endAtMillis: Long,
        selectedDays: Int,
        activationMode: ActivationMode
    ): AlarmActivationResult

    fun isAlarmActivated(
        requestCode: Int,
        contactId: Long,
        originalName: String,
        temporaryName: String,
        tempImage: String?,
        originalImage: String?,
        operation: String,
        dayOfWeek: Int,
        activationId: Long,
        activationMode: Int
    ): Boolean

    fun parseAlarmMetadata(json: String?): List<AlarmMetadata>
    fun toJson(metadata: List<AlarmMetadata>): String
    suspend fun syncAllActivations(): SyncResult
    suspend fun cancelActivatedAlarms(activationId: Long)
}
