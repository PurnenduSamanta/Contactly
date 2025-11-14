package com.purnendu.contactly.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.purnendu.contactly.utils.AppThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("settings")

object ThemePreferences {
    private val KEY_THEME = intPreferencesKey("theme_mode")

    fun themeFlow(context: Context): Flow<AppThemeMode> =
        context.dataStore.data.map { prefs ->
            when (prefs[KEY_THEME] ?: 2) {
                0 -> AppThemeMode.LIGHT
                1 -> AppThemeMode.DARK
                else -> AppThemeMode.SYSTEM
            }
        }

    suspend fun setTheme(context: Context, mode: AppThemeMode) {
        context.dataStore.edit { prefs ->
            prefs[KEY_THEME] = when (mode) {
                AppThemeMode.LIGHT -> 0
                AppThemeMode.DARK -> 1
                AppThemeMode.SYSTEM -> 2
            }
        }
    }
}

