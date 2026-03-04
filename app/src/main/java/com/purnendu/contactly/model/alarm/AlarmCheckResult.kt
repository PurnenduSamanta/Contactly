package com.purnendu.contactly.model.alarm

data class AlarmCheckResult(
    val metadata: AlarmMetadata,
    val isSetInAlarmManager: Boolean,
    val name: String
)