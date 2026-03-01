package com.purnendu.contactly.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * BroadcastReceiver that reschedules all alarms and geofences after device boot.
 *
 * Uses Koin for dependency injection via KoinComponent interface.
 *
 * After a reboot, both AlarmManager alarms and geofences are cleared by the system.
 * This receiver re-registers them using the same sync logic used during app startup.
 */
class RescheduleAlarmsReceiver : BroadcastReceiver(), KoinComponent {

    private val contactlyAlarmManager: ContactlyAlarmManager by inject()
    private val geofenceManager: ContactlyGeofenceManager by inject()

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Re-register time-based alarms (ONE_TIME / REPEAT)
                val syncResult = contactlyAlarmManager.syncAllSchedules()
                Log.d(TAG, "Boot alarm sync: $syncResult")

                // Re-register NEARBY geofences
                geofenceManager.syncAllGeofences()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to sync on boot", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val TAG = "RescheduleAlarmsRx"
    }
}