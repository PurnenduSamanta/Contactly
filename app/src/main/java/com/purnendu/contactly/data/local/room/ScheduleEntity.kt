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
    val selectedDays: Int,
    val scheduledAlarmsMetadata: String? = null,  // JSON array of AlarmMetadata
    val scheduleType: Int // 0 = ONE_TIME, 1 = REPEAT (default for backward compatibility)
)

