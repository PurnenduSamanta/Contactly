package com.purnendu.contactly

import android.app.AlarmManager
import android.app.Application
import android.content.Context
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.purnendu.contactly.data.SchedulesRepository
import com.purnendu.contactly.data.local.room.AppDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivityViewModel(private val context: Application) : AndroidViewModel(context) {


    private val _isAppReady = MutableStateFlow(false)
    val isAppReady: StateFlow<Boolean> = _isAppReady

    init {
        viewModelScope.launch {
            try {
                checkCriticalPermissions()
                initializeDatabase()
                _isAppReady.value = true
            } catch (e: Exception) {
                _isAppReady.value = true
            }
        }

    }

    private suspend fun initializeDatabase() {

        val database = AppDatabase.getDataBase(context)
        val repo = SchedulesRepository(database)
        repo.getAllEntities()
        repo.getSchedules().first()
    }

    private fun checkCriticalPermissions() {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val canScheduleExactAlarms = if (Build.VERSION.SDK_INT >= 31) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true // Exact alarms permission not required on older Android versions
        }

        if (!canScheduleExactAlarms) {
            // In a real app, you might want to direct users to enable exact alarms in settings
        }
    }

}