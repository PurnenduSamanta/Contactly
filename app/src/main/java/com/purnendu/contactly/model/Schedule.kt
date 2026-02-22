package com.purnendu.contactly.model

import androidx.annotation.DrawableRes
import com.purnendu.contactly.utils.ScheduleType

data class Schedule(
    val id: String,
    val name: String,
    val originalName: String,
    @DrawableRes val avatarResId: Int?,
    val contactId: Long?,
    val selectedDays: Int?,
    val startAtMillis: Long?,
    val endAtMillis: Long?,
    val scheduleType: ScheduleType,
    val isCurrentlyActive: Boolean = false,  // For ONE_TIME/REPEAT: between APPLY and REVERT; For INSTANT: manually toggled
    val temporaryImageUri: String?,
    val originalImageUri: String?
)
