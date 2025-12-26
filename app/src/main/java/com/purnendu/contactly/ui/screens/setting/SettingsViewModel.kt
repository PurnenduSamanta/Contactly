package com.purnendu.contactly.ui.screens.setting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.purnendu.contactly.alarm.models.AlarmCheckResult
import com.purnendu.contactly.alarm.models.AlarmStatusInfo
import com.purnendu.contactly.alarm.AlarmSyncManager
import com.purnendu.contactly.alarm.AliasAlarmReceiver
import com.purnendu.contactly.data.repository.SchedulesRepository
import com.purnendu.contactly.data.local.preferences.AppPreferences
import com.purnendu.contactly.utils.AppThemeMode
import com.purnendu.contactly.utils.ViewMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel for Settings screen.
 * 
 * Manages app preferences (theme, view mode, notifications) and alarm status debugging.
 * 
 * Dependencies are injected via Koin using interfaces for testability.
 */
class SettingsViewModel(
    private val schedulesRepo: SchedulesRepository,
    private val alarmSyncManager: AlarmSyncManager,
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
    
    fun loadAlarmStatus(isInDebugMode: Boolean) {
        if(!isInDebugMode)
            return
        viewModelScope.launch(Dispatchers.IO) {
                val schedules = schedulesRepo.getAllEntities()
                val statusList = schedules.map { schedule ->
                    val metadata = alarmSyncManager.parseAlarmMetadata(schedule.scheduledAlarmsMetadata)
                    val alarmResults = metadata.map { meta ->
                        val displayName = if (meta.operation == AliasAlarmReceiver.OP_APPLY) {
                            schedule.temporaryName
                        } else {
                            schedule.originalName
                        }
                        val isSet = alarmSyncManager.isAlarmScheduled(
                            requestCode = meta.requestCode,
                            contactId = schedule.contactId,
                            originalName = schedule.originalName,
                            temporaryName = schedule.temporaryName,
                            operation = meta.operation,
                            dayOfWeek = meta.dayOfWeek,
                            scheduleId = schedule.scheduleId,
                            scheduleType = schedule.scheduleType
                        )
                        AlarmCheckResult(
                            metadata = meta,
                            isSetInAlarmManager = isSet,
                            name = displayName
                        )
                    }
                    AlarmStatusInfo(schedule = schedule, alarms = alarmResults)
                }
                _alarmStatusList.value = statusList
        }
    }
}
