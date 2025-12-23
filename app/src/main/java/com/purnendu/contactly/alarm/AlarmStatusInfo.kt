package com.purnendu.contactly.alarm

import com.purnendu.contactly.data.local.room.ScheduleEntity

data class AlarmStatusInfo(
    val schedule: ScheduleEntity,
    val alarms: List<AlarmCheckResult>
)