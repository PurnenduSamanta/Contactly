package com.purnendu.contactly.data.local.room

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "schedules")
data class ScheduleEntity(
    @PrimaryKey(autoGenerate = true) val scheduleId: Long = 0,
    val contactId: Long,
    val contactLookupKey: String?,
    val originalName: String,
    val temporaryName: String,
    val startAtMillis: Long,
    val endAtMillis: Long,
    val applied: Boolean = false
)

