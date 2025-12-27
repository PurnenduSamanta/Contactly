package com.purnendu.contactly.alarm

import com.purnendu.contactly.alarm.models.AlarmMetadata
import com.purnendu.contactly.alarm.models.AlarmScheduleResult
import com.purnendu.contactly.model.Contact
import com.purnendu.contactly.utils.ScheduleType

/**
 * Interface for alarm scheduling operations.
 * Abstracts AlarmManager interactions to make ViewModels testable.
 */
interface AlarmScheduler {
    
    /**
     * Schedule alarms for a contact with the given parameters.
     * @return true if scheduling was successful, false otherwise
     */
    fun scheduleAlarms(
        contact: Contact,
        scheduleId: Long,
        originalName: String,
        temporaryName: String,
        startAtMillis: Long,
        endAtMillis: Long,
        selectedDays: Int,
        scheduleType: ScheduleType
    ): AlarmScheduleResult
    
    /**
     * Cancel all alarms for a specific schedule.
     */
    suspend fun cancelScheduleAlarms(scheduleId: Long)
    
    /**
     * Convert alarm metadata list to JSON string.
     */
    fun toJson(metadata: List<AlarmMetadata>): String
    
    /**
     * Parse alarm metadata from JSON string.
     */
    fun parseAlarmMetadata(json: String?): List<AlarmMetadata>
}


