package com.purnendu.contactly.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import com.purnendu.contactly.utils.AppThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

object ThemePreferences {
    private val KEY_THEME = intPreferencesKey("theme_mode")

    fun themeFlow(context: Context): Flow<AppThemeMode> =
        context.settingsDataStore.data.map { prefs ->
            when (prefs[KEY_THEME] ?: 2) {
                0 -> AppThemeMode.LIGHT
                1 -> AppThemeMode.DARK
                else -> AppThemeMode.SYSTEM
            }
        }

    suspend fun setTheme(context: Context, mode: AppThemeMode) {
        context.settingsDataStore.edit { prefs ->
            prefs[KEY_THEME] = when (mode) {
                AppThemeMode.LIGHT -> 0
                AppThemeMode.DARK -> 1
                AppThemeMode.SYSTEM -> 2
            }
        }
    }
}
