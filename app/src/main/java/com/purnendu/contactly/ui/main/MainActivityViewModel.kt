package com.purnendu.contactly.ui.main

import android.app.AlarmManager
import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.purnendu.contactly.data.SchedulesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MainActivityViewModel(private val context: Application) : AndroidViewModel(context) {


    init {
        viewModelScope.launch {
            try {
                checkCriticalPermissions()
                initializeAppData()

                // Mark app as ready after all critical operations are complete
                _isAppReady.value = true
            } catch (e: Exception) {
                // Handle any errors during initialization
                // Still mark as ready to prevent app from hanging on splash screen
                _isAppReady.value = true
            }
        }



    }

    private val _isAppReady = MutableStateFlow(false)
    val isAppReady: StateFlow<Boolean> = _isAppReady

    private fun checkCriticalPermissions() {
        // Check if critical permissions are granted
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val canScheduleExactAlarms = if (android.os.Build.VERSION.SDK_INT >= 31) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true // Exact alarms permission not required on older Android versions
        }

        // For development purposes, we'll note if permissions aren't granted
        // but continue loading the app - in a real app you might want to prompt for permissions
        if (!canScheduleExactAlarms) {
            // In a real app, you might want to direct users to enable exact alarms in settings
        }
    }

    private suspend fun initializeAppData() {
        // Initialize the database by getting the repository instance
        // This ensures the database connection is ready before the UI is displayed
        val schedulesRepository = SchedulesRepository.get(context)

        // Optionally pre-warm the database by accessing it once
        // This can help avoid potential first-access delays later
        try {
            schedulesRepository.getAllEntities()
        } catch (e: Exception) {
            // Database might be empty, which is fine
        }
    }
}