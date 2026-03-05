package com.purnendu.contactly.domain.model.alarm

/**
 * Metadata for a single activated alarm
 * Used to track and verify alarms in AlarmManager
 */
data class AlarmMetadata(
    val requestCode: Int,           // Unique request code for the PendingIntent
    val dayOfWeek: Int,              // 0=Sunday, 1=Monday, ..., 6=Saturday
    val operation: String,           // "APPLY" or "REVERT"
    val triggerTimeMillis: Long      // When this alarm should fire
)
