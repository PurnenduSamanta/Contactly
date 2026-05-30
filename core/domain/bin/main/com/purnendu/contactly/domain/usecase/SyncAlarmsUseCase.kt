package com.purnendu.contactly.domain.usecase

import com.purnendu.contactly.domain.model.alarm.SyncResult
import com.purnendu.contactly.domain.repository.AlarmSchedulerRepository
import com.purnendu.contactly.domain.repository.GeofenceRepository

class SyncAlarmsUseCase(
    private val alarmSchedulerRepository: AlarmSchedulerRepository,
    private val geofenceRepository: GeofenceRepository
) {
    suspend operator fun invoke(): SyncResult {
        val result = alarmSchedulerRepository.syncAllActivations()
        geofenceRepository.syncAllGeofences()
        return result
    }
}
