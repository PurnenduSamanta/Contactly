package com.purnendu.contactly.alarm

data class AlarmCheckResult(
    val metadata: AlarmMetadata,
    val isSetInAlarmManager: Boolean,
    val name: String
)