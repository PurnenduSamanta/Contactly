package com.purnendu.contactly

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.purnendu.contactly.alarm.ContactlyAlarmManager
import com.purnendu.contactly.data.local.preferences.AppPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

/**
 * ViewModel for MainActivity.
 *
 * Handles app initialization including alarm synchronization during splash screen.
 * Dependencies are injected via Koin.
 */
class MainActivityViewModel(
    private val contactlyAlarmManager: ContactlyAlarmManager,
    private val appPreferences: AppPreferences
) : ViewModel() {

    private val _isAppReady = MutableStateFlow(false)
    val isAppReady: StateFlow<Boolean> = _isAppReady

    init {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Sync alarms, but only if it's been more than 12 hours
                if (shouldSyncNow()) {
                    Log.d("MainActivityViewModel", "Syncing alarms...")
                    syncAlarms()
                    appPreferences.updateLastSyncTimestamp()
                } else {
                    Log.d("MainActivityViewModel", "Sync skipped, not enough time has passed.")
                }

                // Mark app as ready (dismisses splash screen)
                _isAppReady.value = true
            } catch (e: Exception) {
                Log.e("MainActivityViewModel", "Error during initialization", e)
                _isAppReady.value = true
            }
        }
    }

    /**
     * Checks if a sync should be performed.
     *
     * @return true if it's been more than 12 hours since the last sync.
     */
    private suspend fun shouldSyncNow(): Boolean {
        val lastSyncTimestamp = appPreferences.lastSyncTimestampFlow.first()
        if (lastSyncTimestamp == 0L) {
            // No sync has ever happened, so sync now
            return true
        }

        val currentTime = System.currentTimeMillis()
        val twelveHoursInMillis = TimeUnit.HOURS.toMillis(12)

        return (currentTime - lastSyncTimestamp) > twelveHoursInMillis
    }

    /**
     * Sync all scheduled alarms from database to AlarmManager
     * Runs during splash screen to ensure alarms are properly scheduled
     */
    private suspend fun syncAlarms() {
        try {
            val result = contactlyAlarmManager.syncAllSchedules()
            
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