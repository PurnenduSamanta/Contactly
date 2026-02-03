package com.purnendu.contactly.networking.model

import kotlinx.serialization.Serializable

@Serializable
data class WorldTimeApiResponse(
    val datetime: String,
    val unixtime: Long
)
