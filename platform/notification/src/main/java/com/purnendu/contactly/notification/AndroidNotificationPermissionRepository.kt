package com.purnendu.contactly.notification

import android.content.Context
import com.purnendu.contactly.domain.repository.NotificationPermissionRepository

class AndroidNotificationPermissionRepository(
    private val context: Context
) : NotificationPermissionRepository {
    override fun hasNotificationPermission(): Boolean {
        return NotificationHelper.hasNotificationPermission(context)
    }
}
