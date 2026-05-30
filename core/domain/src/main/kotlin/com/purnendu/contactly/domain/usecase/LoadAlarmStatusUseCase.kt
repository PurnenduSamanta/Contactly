package com.purnendu.contactly.domain.usecase

import com.purnendu.contactly.common.ActivationMode
import com.purnendu.contactly.common.AlarmOperations
import com.purnendu.contactly.domain.model.alarm.AlarmCheckResult
import com.purnendu.contactly.domain.model.alarm.AlarmStatusInfo
import com.purnendu.contactly.domain.repository.ActivationsRepository
import com.purnendu.contactly.domain.repository.AlarmSchedulerRepository

class LoadAlarmStatusUseCase(
    private val activationsRepository: ActivationsRepository,
    private val alarmSchedulerRepository: AlarmSchedulerRepository
) {
    suspend operator fun invoke(): List<AlarmStatusInfo> {
        return activationsRepository.getAllRecords()
            .filter {
                it.activationMode != ActivationMode.INSTANT &&
                    it.activationMode != ActivationMode.NEARBY
            }
            .map { activation ->
                val metadata = alarmSchedulerRepository.parseAlarmMetadata(activation.activatedAlarmsMetadata)
                val alarmResults = metadata.map { meta ->
                    val displayName = if (meta.operation == AlarmOperations.OP_APPLY) {
                        activation.temporaryName
                    } else {
                        activation.originalName
                    }
                    val isSet = alarmSchedulerRepository.isAlarmActivated(
                        requestCode = meta.requestCode,
                        contactId = activation.contactId,
                        originalName = activation.originalName,
                        temporaryName = activation.temporaryName,
                        tempImage = activation.temporaryImage,
                        originalImage = activation.originalImage,
                        operation = meta.operation,
                        dayOfWeek = meta.dayOfWeek,
                        activationId = activation.activationId,
                        activationMode = ActivationMode.toInt(activation.activationMode)
                    )
                    AlarmCheckResult(
                        metadata = meta,
                        isSetInAlarmManager = isSet,
                        name = displayName
                    )
                }
                AlarmStatusInfo(
                    activationId = activation.activationId,
                    temporaryName = activation.temporaryName,
                    activationMode = ActivationMode.toInt(activation.activationMode),
                    alarms = alarmResults
                )
            }
    }
}
