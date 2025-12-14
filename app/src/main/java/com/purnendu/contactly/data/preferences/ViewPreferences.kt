package com.purnendu.contactly.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import com.purnendu.contactly.utils.ViewMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

object ViewPreferences {
    private val KEY_VIEW_MODE = intPreferencesKey("view_mode")

    fun viewModeFlow(context: Context): Flow<ViewMode> =
        context.settingsDataStore.data.map { prefs ->
            when (prefs[KEY_VIEW_MODE] ?: 0) {
                1 -> ViewMode.GRID
                else -> ViewMode.LIST  // Default to LIST
            }
        }

    suspend fun setViewMode(context: Context, mode: ViewMode) {
        context.settingsDataStore.edit { prefs ->
            prefs[KEY_VIEW_MODE] = when (mode) {
                ViewMode.LIST -> 0
                ViewMode.GRID -> 1
            }
        }
    }
}
