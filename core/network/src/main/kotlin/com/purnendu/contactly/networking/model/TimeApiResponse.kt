package com.purnendu.contactly.networking.model

import kotlinx.serialization.Serializable

@Serializable
data class TimeApiResponse(
    val dateTime: String,
    val timeZone: String,
    val date: String,
    val time: String,
    val dayOfWeek: String,
    val dstActive: Boolean,
    val year: Int,
    val month: Int,
    val day: Int,
    val hour: Int,
    val minute: Int,
    val seconds: Int,
    val milliSeconds: Int
)
