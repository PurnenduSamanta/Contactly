package com.purnendu.contactly

import android.app.Application
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

    init {
        viewModelScope.launch {
            try {
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

    fun dismissExactAlarmPermissionDialog() {
        _showExactAlarmPermissionDialog.value = false
    }
}