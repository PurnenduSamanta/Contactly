package com.purnendu.contactly.ui.screens.setting

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.purnendu.contactly.data.preferences.ThemePreferences
import com.purnendu.contactly.utils.AppThemeMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(app: Application) : AndroidViewModel(app) {
    val theme: StateFlow<AppThemeMode> = ThemePreferences
        .themeFlow(app)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppThemeMode.SYSTEM)

    fun setTheme(mode: AppThemeMode) {
        viewModelScope.launch { ThemePreferences.setTheme(getApplication(), mode) }
    }
}

