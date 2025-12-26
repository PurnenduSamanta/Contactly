package com.purnendu.contactly.alarm.models

data class AlarmCheckResult(
    val metadata: AlarmMetadata,
    val isSetInAlarmManager: Boolean,
    val name: String
)