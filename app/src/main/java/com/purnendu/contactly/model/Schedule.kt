package com.purnendu.contactly.model

import androidx.annotation.DrawableRes

data class Schedule(
    val id: String,
    val name: String,
    val originalName: String,
    @DrawableRes val avatarResId: Int? = null,
    val contactId: Long? = null,
    val selectedDays: Int = 127,  // Bitmask: 127 = all days (Sun-Sat)
    val startAtMillis: Long = 0L,
    val endAtMillis: Long = 0L
)
