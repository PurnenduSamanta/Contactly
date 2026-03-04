package com.purnendu.contactly.ui.screens.home

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Used to notify the UI when an receiver fires so it can refresh the active status.
 * This is more efficient than periodic polling.
 */
object StatusEventBus {

    private val _alarmFired = MutableSharedFlow<AlarmEvent>(extraBufferCapacity = 1)
    val alarmFired: SharedFlow<AlarmEvent> = _alarmFired.asSharedFlow()

    /**
     * Call this when an alarm fires to notify observers
     */
    fun notifyAlarmFired(activationId: Long, operation: String) {
        _alarmFired.tryEmit(AlarmEvent(activationId, operation))
    }
}

data class AlarmEvent(
    val activationId: Long,
    val operation: String  // "APPLY" or "REVERT"
)