package com.purnendu.contactly.domain.model.alarm

data class SyncResult(
    val totalActivations: Int,
    val alarmsActivated: Int,
    val alarmsSkipped: Int,
    val errors: Int,
    val orphanedActivationsRemoved: Int
)
