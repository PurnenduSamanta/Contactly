package com.purnendu.contactly.alarm.models

data class SyncResult(
    val totalSchedules: Int,
    val alarmsScheduled: Int,
    val alarmsSkipped: Int,
    val errors: Int,
    val orphanedSchedulesRemoved: Int
)