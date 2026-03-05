package com.purnendu.contactly.ui.screens.setting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.purnendu.contactly.domain.model.alarm.AlarmCheckResult
import com.purnendu.contactly.domain.model.alarm.AlarmStatusInfo
import com.purnendu.contactly.manager.ContactlyAlarmManager
import com.purnendu.contactly.receiver.AliasAlarmReceiver
import com.purnendu.contactly.data.repository.ActivationsRepository
import com.purnendu.contactly.domain.repository.AppPreferences
import com.purnendu.contactly.common.AppThemeMode
import com.purnendu.contactly.common.ActivationMode
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
    private val activationsRepo: ActivationsRepository,
    private val contactlyAlarmManager: ContactlyAlarmManager,
    private val appPreferences: AppPreferences
) : ViewModel() {
    
    val theme: StateFlow<AppThemeMode> = appPreferences.themeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppThemeMode.SYSTEM)
    
    val viewMode: StateFlow<ViewMode> = appPreferences.viewModeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ViewMode.LIST)
    
    val notificationsEnabled: StateFlow<Boolean> = appPreferences.notificationsEnabledFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _alarmStatusList = MutableStateFlow<List<AlarmStatusInfo>>(emptyList())
    val alarmStatusList: StateFlow<List<AlarmStatusInfo>> = _alarmStatusList.asStateFlow()

    fun setTheme(mode: AppThemeMode) {
        viewModelScope.launch(Dispatchers.IO) { appPreferences.setTheme(mode) }
    }
    
    fun setViewMode(mode: ViewMode) {
        viewModelScope.launch(Dispatchers.IO) { appPreferences.setViewMode(mode) }
    }
    
    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) { appPreferences.setNotificationsEnabled(enabled) }
    }
    
    val biometricEnabled: StateFlow<Boolean?> = appPreferences.biometricEnabledFlow
        .map<Boolean, Boolean?> { it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
        
    fun setBiometricEnabled(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) { appPreferences.setBiometricEnabled(enabled) }
    }
    
    fun loadAlarmStatus(isInDebugMode: Boolean) {
        if(!isInDebugMode)
            return
        viewModelScope.launch(Dispatchers.IO) {
                val activations = activationsRepo.getAllEntities().filter {
                    val mode = ActivationMode.fromInt(it.activationMode)
                    mode != ActivationMode.INSTANT && mode != ActivationMode.NEARBY
                }
                val statusList = activations.map { activation ->
                    val metadata = contactlyAlarmManager.parseAlarmMetadata(activation.activatedAlarmsMetadata)
                    val alarmResults = metadata.map { meta ->
                        val displayName = if (meta.operation == AliasAlarmReceiver.OP_APPLY) {
                            activation.temporaryName
                        } else {
                            activation.originalName
                        }
                        val isSet = contactlyAlarmManager.isAlarmActivated(
                            requestCode = meta.requestCode,
                            contactId = activation.contactId,
                            originalName = activation.originalName,
                            temporaryName = activation.temporaryName,
                            tempImage = activation.temporaryImage,
                            originalImage = activation.originalImage,
                            operation = meta.operation,
                            dayOfWeek = meta.dayOfWeek,
                            activationId = activation.activationId,
                            activationMode = activation.activationMode
                        )
                        AlarmCheckResult(
                            metadata = meta,
                            isSetInAlarmManager = isSet,
                            name = displayName
                        )
                    }
                    AlarmStatusInfo(
                        activationId = activation.activationId,
                        temporaryName = activation.temporaryName,
                        activationMode = activation.activationMode,
                        alarms = alarmResults
                    )
                }
                _alarmStatusList.value = statusList
        }
    }
}
