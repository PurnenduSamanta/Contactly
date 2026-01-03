package com.purnendu.contactly.alarm

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Event bus for alarm events.
 * Used to notify the UI when an alarm fires so it can refresh the active status.
 * This is more efficient than periodic polling.
 */
object AlarmEventBus {
    
    private val _alarmFired = MutableSharedFlow<AlarmEvent>(extraBufferCapacity = 1)
    val alarmFired: SharedFlow<AlarmEvent> = _alarmFired.asSharedFlow()
    
    /**
     * Call this when an alarm fires to notify observers
     */
    fun notifyAlarmFired(scheduleId: Long, operation: String) {
        _alarmFired.tryEmit(AlarmEvent(scheduleId, operation))
    }
}

data class AlarmEvent(
    val scheduleId: Long,
    val operation: String  // "APPLY" or "REVERT"
)
