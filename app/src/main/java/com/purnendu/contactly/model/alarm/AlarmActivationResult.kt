package com.purnendu.contactly.model.alarm


/**
 * Result of alarm activating operation.
 */
data class AlarmActivationResult(
    val success: Boolean,
    val alarmMetadata: List<AlarmMetadata>
)