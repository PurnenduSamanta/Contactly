package com.purnendu.contactly.domain.usecase

import com.purnendu.contactly.domain.repository.GeofenceRepository

class CheckBackgroundLocationPermissionUseCase(
    private val geofenceRepository: GeofenceRepository
) {
    operator fun invoke(): Boolean = geofenceRepository.hasBackgroundLocationPermission()
}
