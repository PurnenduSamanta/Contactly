package com.purnendu.contactly.domain.model.alarm

/**
 * Fix 2 applied: Replaced ActivationEntity (Room) with domain-level fields.
 * This keeps the domain layer free of Android/Room dependencies.
 */
data class AlarmStatusInfo(
    val activationId: Long,
    val temporaryName: String,
    val activationMode: Int,
    val alarms: List<AlarmCheckResult>
)
