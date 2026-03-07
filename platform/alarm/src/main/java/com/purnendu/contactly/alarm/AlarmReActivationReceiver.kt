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
 * BroadcastReceiver that re-registers all alarms and geofences after device boot.
 *
 * Uses Koin for dependency injection via KoinComponent interface.
 *
 * After a reboot, AlarmManager alarms are cleared by the system.
 * This receiver re-registers them using the same synchronization logic used during app startup.
 */
class AlarmReActivationReceiver : BroadcastReceiver(), KoinComponent {

    private val contactlyAlarmManager: ContactlyAlarmManager by inject()


    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Re-register time-based alarms (ONE_TIME / REPEAT)
                val syncResult = contactlyAlarmManager.syncAllActivations()
                Log.d(TAG, "Boot alarm sync: $syncResult")


            } catch (e: Exception) {
                Log.e(TAG, "Failed to sync on boot", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val TAG = "ReActivationReceiver"
    }
}