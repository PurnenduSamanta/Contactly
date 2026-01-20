package com.purnendu.contactly.model

import androidx.annotation.DrawableRes
import com.purnendu.contactly.utils.ScheduleType

data class Schedule(
    val id: String,
    val name: String,
    val originalName: String,
    @DrawableRes val avatarResId: Int?,
    val contactId: Long?,
    val selectedDays: Int,
    val startAtMillis: Long,
    val endAtMillis: Long,
    val scheduleType: ScheduleType,
    val isCurrentlyActive: Boolean = false,  // True when between APPLY and REVERT operations
    val temporaryImageUri: String? = null  // Optional URI for temporary contact image
)
