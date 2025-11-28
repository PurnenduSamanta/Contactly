package com.purnendu.contactly

import android.Manifest
import android.app.AlarmManager
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
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

    private val _showExactAlarmPermissionDialog = MutableStateFlow(false)
    val showExactAlarmPermissionDialog: StateFlow<Boolean> = _showExactAlarmPermissionDialog

    private val _showContactPermissionDialog = MutableStateFlow(false)
    val showContactPermissionDialog: StateFlow<Boolean> = _showContactPermissionDialog

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

        val hasContactPermissions = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.WRITE_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED

        // Update dialog states
        _showExactAlarmPermissionDialog.value = !canScheduleExactAlarms
        _showContactPermissionDialog.value = !hasContactPermissions
    }

    fun dismissExactAlarmPermissionDialog() {
        _showExactAlarmPermissionDialog.value = false
    }

    fun dismissContactPermissionDialog() {
        _showContactPermissionDialog.value = false
    }
}