package com.purnendu.contactly.domain.repository

interface NotificationPermissionRepository {
    fun hasNotificationPermission(): Boolean
}
