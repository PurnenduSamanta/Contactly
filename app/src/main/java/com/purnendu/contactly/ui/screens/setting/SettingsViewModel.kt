package com.purnendu.contactly.ui.screens.setting

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.purnendu.contactly.data.preferences.ThemePreferences
import com.purnendu.contactly.data.preferences.ViewPreferences
import com.purnendu.contactly.utils.AppThemeMode
import com.purnendu.contactly.utils.ViewMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(app: Application) : AndroidViewModel(app) {
    val theme: StateFlow<AppThemeMode> = ThemePreferences
        .themeFlow(app)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppThemeMode.SYSTEM)
    
    val viewMode: StateFlow<ViewMode> = ViewPreferences
        .viewModeFlow(app)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ViewMode.LIST)

    fun setTheme(mode: AppThemeMode) {
        viewModelScope.launch { ThemePreferences.setTheme(getApplication(), mode) }
    }
    
    fun setViewMode(mode: ViewMode) {
        viewModelScope.launch { ViewPreferences.setViewMode(getApplication(), mode) }
    }
}
