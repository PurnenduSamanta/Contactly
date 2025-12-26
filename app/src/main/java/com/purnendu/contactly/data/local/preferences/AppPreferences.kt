package com.purnendu.contactly.data.local.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.purnendu.contactly.notification.NotificationHelper
import com.purnendu.contactly.utils.AppThemeMode
import com.purnendu.contactly.utils.ViewMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Unified DataStore preferences for the app
 * All app preferences are managed here to avoid multiple DataStore instances
 */

// Single DataStore instance for all preferences
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

object AppPreferences {
    // Preference Keys
    private val KEY_THEME = intPreferencesKey("theme_mode")
    private val KEY_VIEW_MODE = intPreferencesKey("view_mode")
    private val KEY_NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
    
    // ========== Theme Preferences ==========
    
    /**
     * Get theme mode as a Flow
     * @return Flow of AppThemeMode (LIGHT, DARK, or SYSTEM)
     */
    fun themeFlow(context: Context): Flow<AppThemeMode> =
        context.dataStore.data.map { prefs ->
            when (prefs[KEY_THEME] ?: 2) {
                0 -> AppThemeMode.LIGHT
                1 -> AppThemeMode.DARK
                else -> AppThemeMode.SYSTEM // Default
            }
        }
    
    /**
     * Set theme mode
     * @param mode AppThemeMode to apply
     */
    suspend fun setTheme(context: Context, mode: AppThemeMode) {
        context.dataStore.edit { prefs ->
            prefs[KEY_THEME] = when (mode) {
                AppThemeMode.LIGHT -> 0
                AppThemeMode.DARK -> 1
                AppThemeMode.SYSTEM -> 2
            }
        }
    }
    
    // ========== View Mode Preferences ==========
    
    /**
     * Get view mode as a Flow
     * @return Flow of ViewMode (LIST or GRID)
     */
    fun viewModeFlow(context: Context): Flow<ViewMode> =
        context.dataStore.data.map { prefs ->
            when (prefs[KEY_VIEW_MODE] ?: 0) {
                1 -> ViewMode.GRID
                else -> ViewMode.LIST  // Default
            }
        }
    
    /**
     * Set view mode
     * @param mode ViewMode to apply (LIST or GRID)
     */
    suspend fun setViewMode(context: Context, mode: ViewMode) {
        context.dataStore.edit { prefs ->
            prefs[KEY_VIEW_MODE] = when (mode) {
                ViewMode.LIST -> 0
                ViewMode.GRID -> 1
            }
        }
    }
    
    // ========== Notification Preferences ==========
    
    /**
     * Get notifications enabled state as a Flow
     * @return Flow of Boolean (true = enabled, false = disabled)
     */
    fun notificationsEnabledFlow(context: Context): Flow<Boolean> =
        context.dataStore.data.map { prefs ->
            prefs[KEY_NOTIFICATIONS_ENABLED] ?: NotificationHelper.hasNotificationPermission(context) // Default to enabled
        }
    
    /**
     * Set notifications enabled state
     * @param enabled Boolean to enable/disable notifications
     */
    suspend fun setNotificationsEnabled(context: Context, enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_NOTIFICATIONS_ENABLED] = enabled
        }
    }
}
