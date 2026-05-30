package com.purnendu.contactly.ui.screens.setting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.purnendu.contactly.domain.model.alarm.AlarmStatusInfo
import com.purnendu.contactly.domain.usecase.CheckNotificationPermissionUseCase
import com.purnendu.contactly.domain.usecase.LoadAlarmStatusUseCase
import com.purnendu.contactly.domain.usecase.ManageAppPreferencesUseCase
import com.purnendu.contactly.common.AppThemeMode
import com.purnendu.contactly.common.ViewMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel for Settings screen.
 * 
 * Manages app preferences (theme, view mode, notifications) and activation status debugging.
 */
class SettingsViewModel(
    private val checkNotificationPermissionUseCase: CheckNotificationPermissionUseCase,
    private val loadAlarmStatusUseCase: LoadAlarmStatusUseCase,
    private val manageAppPreferencesUseCase: ManageAppPreferencesUseCase
) : ViewModel() {
    
    val theme: StateFlow<AppThemeMode> = manageAppPreferencesUseCase.themeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppThemeMode.SYSTEM)
    
    val viewMode: StateFlow<ViewMode> = manageAppPreferencesUseCase.viewModeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ViewMode.LIST)
    
    val notificationsEnabled: StateFlow<Boolean> = manageAppPreferencesUseCase.notificationsEnabledFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _alarmStatusList = MutableStateFlow<List<AlarmStatusInfo>>(emptyList())
    val alarmStatusList: StateFlow<List<AlarmStatusInfo>> = _alarmStatusList.asStateFlow()

    fun setTheme(mode: AppThemeMode) {
        viewModelScope.launch(Dispatchers.IO) { manageAppPreferencesUseCase.setTheme(mode) }
    }
    
    fun setViewMode(mode: ViewMode) {
        viewModelScope.launch(Dispatchers.IO) { manageAppPreferencesUseCase.setViewMode(mode) }
    }
    
    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            manageAppPreferencesUseCase.setNotificationsEnabled(enabled)
        }
    }
    
    val biometricEnabled: StateFlow<Boolean?> = manageAppPreferencesUseCase.biometricEnabledFlow
        .map<Boolean, Boolean?> { it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
        
    fun setBiometricEnabled(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            manageAppPreferencesUseCase.setBiometricEnabled(enabled)
        }
    }

    fun hasNotificationPermission(): Boolean = checkNotificationPermissionUseCase()
    
    fun loadAlarmStatus(isInDebugMode: Boolean) {
        if(!isInDebugMode)
            return
        viewModelScope.launch(Dispatchers.IO) {
            _alarmStatusList.value = loadAlarmStatusUseCase()
        }
    }
}
