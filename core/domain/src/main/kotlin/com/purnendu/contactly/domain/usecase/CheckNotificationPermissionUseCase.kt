package com.purnendu.contactly.domain.usecase

import com.purnendu.contactly.domain.repository.NotificationPermissionRepository

class CheckNotificationPermissionUseCase(
    private val notificationPermissionRepository: NotificationPermissionRepository
) {
    operator fun invoke(): Boolean = notificationPermissionRepository.hasNotificationPermission()
}
