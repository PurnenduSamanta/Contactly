package com.purnendu.contactly.ui.screens.setting

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.purnendu.contactly.alarm.AlarmMetadata
import com.purnendu.contactly.alarm.AlarmSyncManager
import com.purnendu.contactly.alarm.AliasAlarmReceiver
import com.purnendu.contactly.data.SchedulesRepository
import com.purnendu.contactly.data.local.room.AppDatabase
import com.purnendu.contactly.data.local.room.ScheduleEntity
import com.purnendu.contactly.data.preferences.AppPreferences
import com.purnendu.contactly.utils.AppThemeMode
import com.purnendu.contactly.utils.ViewMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class AlarmStatusInfo(
    val schedule: ScheduleEntity,
    val alarms: List<AlarmCheckResult>
)

data class AlarmCheckResult(
    val metadata: AlarmMetadata,
    val isSetInAlarmManager: Boolean,
    val name: String
)

class SettingsViewModel(app: Application) : AndroidViewModel(app) {
    private val schedulesRepo = SchedulesRepository(AppDatabase.getDataBase(app))
    private val alarmSyncManager = AlarmSyncManager(app)
    
    val theme: StateFlow<AppThemeMode> = AppPreferences
        .themeFlow(app)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppThemeMode.SYSTEM)
    
    val viewMode: StateFlow<ViewMode> = AppPreferences
        .viewModeFlow(app)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ViewMode.LIST)

    private val _alarmStatusList = MutableStateFlow<List<AlarmStatusInfo>>(emptyList())
    val alarmStatusList: StateFlow<List<AlarmStatusInfo>> = _alarmStatusList.asStateFlow()

    fun setTheme(mode: AppThemeMode) {
        viewModelScope.launch { AppPreferences.setTheme(getApplication(), mode) }
    }
    
    fun setViewMode(mode: ViewMode) {
        viewModelScope.launch { AppPreferences.setViewMode(getApplication(), mode) }
    }
    
    fun loadAlarmStatus() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val schedules = schedulesRepo.getAllEntities()
                val statusList = schedules.map { schedule ->
                    val metadata = alarmSyncManager.parseAlarmMetadata(schedule.scheduledAlarmsMetadata)
                    val alarmResults = metadata.map { meta ->
                        val name = if (meta.operation == AliasAlarmReceiver.OP_APPLY) {
                            schedule.temporaryName
                        } else {
                            schedule.originalName
                        }
                        val isSet = alarmSyncManager.isAlarmScheduled(
                            requestCode = meta.requestCode,
                            contactId = schedule.contactId,
                            name = name,
                            operation = meta.operation,
                            dayOfWeek = meta.dayOfWeek,
                            scheduleId = schedule.scheduleId,
                            scheduleType = schedule.scheduleType
                        )
                        AlarmCheckResult(
                            metadata = meta,
                            isSetInAlarmManager = isSet,
                            name = name
                        )
                    }
                    AlarmStatusInfo(schedule = schedule, alarms = alarmResults)
                }
                _alarmStatusList.value = statusList
            }
        }
    }
}

