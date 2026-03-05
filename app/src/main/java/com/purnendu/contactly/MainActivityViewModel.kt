package com.purnendu.contactly

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.purnendu.contactly.manager.ContactlyAlarmManager
import com.purnendu.contactly.manager.ContactlyGeofenceManager
import com.purnendu.contactly.domain.repository.AppPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
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
    private val geofenceManager: ContactlyGeofenceManager,
    private val appPreferences: AppPreferences
) : ViewModel() {

    private val _isAppReady = MutableStateFlow(false)
    val isAppReady: StateFlow<Boolean> = _isAppReady

    // Event flow for triggering add activation action (center FAB click)
    private val _addActivationEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val addActivationEvent: SharedFlow<Unit> = _addActivationEvent.asSharedFlow()

    // Shared location from Google Maps — StateFlow so the value persists until consumed
    // (SharedFlow would drop the event if HomeScreen hasn't composed yet)
    private val _sharedLocationText = MutableStateFlow<String?>(null)
    val sharedLocationText: StateFlow<String?> = _sharedLocationText.asStateFlow()

    // Track whether activations exist (for hiding center FAB when empty)
    private val _hasActivations = MutableStateFlow(false)
    val hasActivations: StateFlow<Boolean> = _hasActivations

    /**
     * Trigger the add activation flow (called when center FAB is clicked)
     */
    fun triggerAddActivation() {
        _addActivationEvent.tryEmit(Unit)
    }

    /**
     * Handle shared location text from Google Maps
     */
    fun handleSharedLocation(sharedText: String) {
        _sharedLocationText.value = sharedText
    }

    /**
     * Clear shared location after it has been consumed by the UI
     */
    fun clearSharedLocation() {
        _sharedLocationText.value = null
    }

    /**
     * Update whether activations exist
     */
    fun setHasActivations(hasActivation: Boolean) {
        _hasActivations.value = hasActivation
    }

    init {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Sync alarms and geofences, but only if it's been more than 12 hours
                if (shouldSyncNow()) {
                    Log.d("MainActivityViewModel", "Syncing alarms and geofences...")
                    syncAlarms()
                    syncGeofences()
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
     * Sync all activated alarms from database to AlarmManager
     * Runs during splash screen to ensure alarms are properly activation
     */
    private suspend fun syncAlarms() {
        try {
            val result = contactlyAlarmManager.syncAllActivations()
            
            Log.d("MainActivityViewModel", "Alarm sync completed: " +
                    "activated=${result.alarmsActivated}, " +
                    "skipped=${result.alarmsSkipped}, " +
                    "errors=${result.errors}, " +
                    "orphaned=${result.orphanedActivationsRemoved}")
        } catch (e: Exception) {
            Log.e("MainActivityViewModel", "Failed to sync alarms", e)
        }
    }

    /**
     * Re-register all NEARBY geofences
     * Geofences can be lost if the system clears them
     */
    private suspend fun syncGeofences() {
        try {
            geofenceManager.syncAllGeofences()
            Log.d("MainActivityViewModel", "Geofence sync completed")
        } catch (e: Exception) {
            Log.e("MainActivityViewModel", "Failed to sync geofences", e)
        }
    }
}