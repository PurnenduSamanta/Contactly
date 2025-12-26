package com.purnendu.contactly.data.repository

import com.purnendu.contactly.data.local.room.AppDatabase
import com.purnendu.contactly.data.local.room.ScheduleEntity
import com.purnendu.contactly.model.Schedule
import com.purnendu.contactly.utils.ScheduleType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class SchedulesRepository(private val database: AppDatabase) {
    fun getSchedules(): Flow<List<Schedule>> = database.scheduleDao().getAll().map { list ->
        list.map { e ->
            Schedule(
                id = e.scheduleId.toString(),
                name = e.temporaryName,
                originalName = e.originalName,
                avatarResId = null,
                contactId = e.contactId,
                selectedDays = e.selectedDays,
                startAtMillis = e.startAtMillis,
                endAtMillis = e.endAtMillis,
                scheduleType = if (e.scheduleType == 0) ScheduleType.ONE_TIME else ScheduleType.REPEAT
            )
        }
    }

    suspend fun create(
        scheduleId: Long,
        contactId: Long,
        contactLookupKey: String?,
        originalName: String,
        temporaryName: String,
        startAtMillis: Long,
        endAtMillis: Long,
        selectedDays: Int ,  // Default to all days
        scheduledAlarmsMetadata: String? = null,
        scheduleType: ScheduleType
    ): Long {
        return database.scheduleDao().insert(
            ScheduleEntity(
                scheduleId = scheduleId,
                contactId = contactId,
                contactLookupKey = contactLookupKey,
                originalName = originalName,
                temporaryName = temporaryName,
                startAtMillis = startAtMillis,
                endAtMillis = endAtMillis,
                selectedDays = selectedDays,
                scheduledAlarmsMetadata = scheduledAlarmsMetadata,
                scheduleType = if (scheduleType == ScheduleType.ONE_TIME) 0 else 1
            )
        )
    }

    suspend fun update(entity: ScheduleEntity) = database.scheduleDao().update(entity)

    suspend fun deleteById(id: Long) = database.scheduleDao().deleteById(id)

    suspend fun getById(id: Long): ScheduleEntity? = database.scheduleDao().getById(id)

    suspend fun getAllEntities(): List<ScheduleEntity> = database.scheduleDao().getAll().first()

    suspend fun deleteByContactId(contactId: Long) = database.scheduleDao().deleteByContactId(contactId)


}
