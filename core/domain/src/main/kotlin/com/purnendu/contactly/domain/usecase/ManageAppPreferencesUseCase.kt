package com.purnendu.contactly.domain.usecase

import com.purnendu.contactly.common.AppThemeMode
import com.purnendu.contactly.common.ViewMode
import com.purnendu.contactly.domain.repository.AppPreferences

class ManageAppPreferencesUseCase(
    private val appPreferences: AppPreferences
) {
    val themeFlow = appPreferences.themeFlow
    val viewModeFlow = appPreferences.viewModeFlow
    val notificationsEnabledFlow = appPreferences.notificationsEnabledFlow
    val lastSyncTimestampFlow = appPreferences.lastSyncTimestampFlow
    val biometricEnabledFlow = appPreferences.biometricEnabledFlow

    suspend fun setTheme(mode: AppThemeMode) = appPreferences.setTheme(mode)
    suspend fun setViewMode(mode: ViewMode) = appPreferences.setViewMode(mode)
    suspend fun setNotificationsEnabled(enabled: Boolean) {
        appPreferences.setNotificationsEnabled(enabled)
    }

    suspend fun setBiometricEnabled(enabled: Boolean) {
        appPreferences.setBiometricEnabled(enabled)
    }

    suspend fun updateLastSyncTimestamp() = appPreferences.updateLastSyncTimestamp()
}
