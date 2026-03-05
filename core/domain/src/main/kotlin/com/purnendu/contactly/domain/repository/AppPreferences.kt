package com.purnendu.contactly.domain.repository

import com.purnendu.contactly.common.AppThemeMode
import com.purnendu.contactly.common.ViewMode
import kotlinx.coroutines.flow.Flow

/**
 * Interface for app preferences.
 * Abstracts DataStore operations to make ViewModels testable.
 */
interface AppPreferences {
    val themeFlow: Flow<AppThemeMode>
    val viewModeFlow: Flow<ViewMode>
    val notificationsEnabledFlow: Flow<Boolean>
    val lastSyncTimestampFlow: Flow<Long>

    suspend fun setTheme(mode: AppThemeMode)
    suspend fun setViewMode(mode: ViewMode)
    suspend fun setNotificationsEnabled(enabled: Boolean)
    suspend fun updateLastSyncTimestamp()

    val biometricEnabledFlow: Flow<Boolean>
    suspend fun setBiometricEnabled(enabled: Boolean)
}
