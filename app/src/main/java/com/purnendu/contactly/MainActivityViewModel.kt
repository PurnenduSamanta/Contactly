package com.purnendu.contactly

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.purnendu.contactly.alarm.AlarmSyncManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MainActivityViewModel(private val context: Application) : AndroidViewModel(context) {

    private val _isAppReady = MutableStateFlow(false)
    val isAppReady: StateFlow<Boolean> = _isAppReady

    init {
        viewModelScope.launch {
            try {
                // Sync alarms with AlarmManager (keeps splash screen visible)
                syncAlarms()
                
                // Mark app as ready (dismisses splash screen)
                _isAppReady.value = true
            } catch (e: Exception) {
                Log.e("MainActivityViewModel", "Error during initialization", e)
                _isAppReady.value = true
            }
        }
    }

    /**
     * Sync all scheduled alarms from database to AlarmManager
     * Runs during splash screen to ensure alarms are properly scheduled
     */
    private suspend fun syncAlarms() {
        try {
            val syncManager = AlarmSyncManager(context)
            val result = syncManager.syncAllSchedules()
            
            Log.d("MainActivityViewModel", "Alarm sync completed: " +
                    "scheduled=${result.alarmsScheduled}, " +
                    "skipped=${result.alarmsSkipped}, " +
                    "errors=${result.errors}, " +
                    "orphaned=${result.orphanedSchedulesRemoved}")
        } catch (e: Exception) {
            Log.e("MainActivityViewModel", "Failed to sync alarms", e)
        }
    }
}