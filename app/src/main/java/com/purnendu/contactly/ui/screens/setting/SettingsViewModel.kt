package com.purnendu.contactly.ui.screens.setting

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.purnendu.contactly.alarm.models.AlarmCheckResult
import com.purnendu.contactly.alarm.models.AlarmStatusInfo
import com.purnendu.contactly.alarm.AlarmSyncManager
import com.purnendu.contactly.alarm.AliasAlarmReceiver
import com.purnendu.contactly.data.repository.SchedulesRepository
import com.purnendu.contactly.data.local.room.AppDatabase
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


class SettingsViewModel(private val app: Application) : AndroidViewModel(app) {
    private val schedulesRepo = SchedulesRepository(AppDatabase.getDataBase(app))
    private val alarmSyncManager = AlarmSyncManager(app)
    
    val theme: StateFlow<AppThemeMode> = AppPreferences
        .themeFlow(app)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppThemeMode.SYSTEM)
    
    val viewMode: StateFlow<ViewMode> = AppPreferences
        .viewModeFlow(app)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ViewMode.LIST)
    
    val notificationsEnabled: StateFlow<Boolean> = AppPreferences
        .notificationsEnabledFlow(app)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _alarmStatusList = MutableStateFlow<List<AlarmStatusInfo>>(emptyList())
    val alarmStatusList: StateFlow<List<AlarmStatusInfo>> = _alarmStatusList.asStateFlow()

    fun setTheme(mode: AppThemeMode) {
        viewModelScope.launch(Dispatchers.IO) { AppPreferences.setTheme(app, mode) }
    }
    
    fun setViewMode(mode: ViewMode) {
        viewModelScope.launch(Dispatchers.IO) { AppPreferences.setViewMode(app, mode) }
    }
    
    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) { AppPreferences.setNotificationsEnabled(app, enabled) }
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





