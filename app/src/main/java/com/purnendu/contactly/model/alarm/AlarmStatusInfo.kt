package com.purnendu.contactly.model.alarm

import com.purnendu.contactly.data.local.room.ActivationEntity

data class AlarmStatusInfo(
    val activation: ActivationEntity,
    val alarms: List<AlarmCheckResult>
)