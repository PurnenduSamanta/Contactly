package com.purnendu.contactly.alarm.models


/**
 * Result of alarm scheduling operation.
 */
data class AlarmScheduleResult(
    val success: Boolean,
    val alarmMetadata: List<AlarmMetadata>
)