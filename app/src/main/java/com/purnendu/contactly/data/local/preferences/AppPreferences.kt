package com.purnendu.contactly.data.local.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.purnendu.contactly.notification.NotificationHelper
import com.purnendu.contactly.common.AppThemeMode
import com.purnendu.contactly.common.ViewMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Single DataStore instance for all preferences (extension property)
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

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

/**
 * Android implementation of AppPreferences using DataStore.
 *
 * This class is a singleton managed by Koin:
 * - Injects Context once during creation
 * - Provides reactive Flows for preference values
 * - All preferences are persisted using DataStore
 *
 * For testing, create a fake implementation of AppPreferences interface.
 */
class AppPreferencesImpl(private val context: Context) : AppPreferences {

    // Preference Keys
    private companion object {
        val KEY_THEME = intPreferencesKey("theme_mode")
        val KEY_VIEW_MODE = intPreferencesKey("view_mode")
        val KEY_NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val KEY_LAST_SYNC_TIMESTAMP = longPreferencesKey("last_sync_timestamp")
        val KEY_BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
    }

    // ========== Theme Preferences ==========

    override val themeFlow: Flow<AppThemeMode> = context.dataStore.data.map { prefs ->
        when (prefs[KEY_THEME] ?: 2) {
            0 -> AppThemeMode.LIGHT
            1 -> AppThemeMode.DARK
            else -> AppThemeMode.SYSTEM // Default
        }
    }

    override suspend fun setTheme(mode: AppThemeMode) {
        context.dataStore.edit { prefs ->
            prefs[KEY_THEME] = when (mode) {
                AppThemeMode.LIGHT -> 0
                AppThemeMode.DARK -> 1
                AppThemeMode.SYSTEM -> 2
            }
        }
    }

    // ========== View Mode Preferences ==========

    override val viewModeFlow: Flow<ViewMode> = context.dataStore.data.map { prefs ->
        when (prefs[KEY_VIEW_MODE] ?: 0) {
            1 -> ViewMode.GRID
            else -> ViewMode.LIST  // Default
        }
    }

    override suspend fun setViewMode(mode: ViewMode) {
        context.dataStore.edit { prefs ->
            prefs[KEY_VIEW_MODE] = when (mode) {
                ViewMode.LIST -> 0
                ViewMode.GRID -> 1
            }
        }
    }

    // ========== Notification Preferences ==========

    override val notificationsEnabledFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_NOTIFICATIONS_ENABLED] ?: NotificationHelper.hasNotificationPermission(context)
    }

    override suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_NOTIFICATIONS_ENABLED] = enabled
        }
    }

    // ========== Sync Timestamp Preferences ==========

    override val lastSyncTimestampFlow: Flow<Long> = context.dataStore.data.map { prefs ->
        prefs[KEY_LAST_SYNC_TIMESTAMP] ?: 0L
    }

    override suspend fun updateLastSyncTimestamp() {
        context.dataStore.edit { prefs ->
            prefs[KEY_LAST_SYNC_TIMESTAMP] = System.currentTimeMillis()
        }
    }

    // ========== Biometric Preferences ==========

    override val biometricEnabledFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_BIOMETRIC_ENABLED] ?: false
    }

    override suspend fun setBiometricEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_BIOMETRIC_ENABLED] = enabled
        }
    }
}
