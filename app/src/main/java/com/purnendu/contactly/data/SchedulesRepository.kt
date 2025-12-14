package com.purnendu.contactly.data

import com.purnendu.contactly.data.local.room.AppDatabase
import com.purnendu.contactly.data.local.room.ScheduleEntity
import com.purnendu.contactly.model.Schedule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first

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
                endAtMillis = e.endAtMillis
            )
        }
    }

    suspend fun create(
        contactId: Long,
        contactLookupKey: String?,
        originalName: String,
        temporaryName: String,
        startAtMillis: Long,
        endAtMillis: Long,
        selectedDays: Int = 127,  // Default to all days
        scheduledAlarmsMetadata: String? = null
    ): Long {
        return database.scheduleDao().insert(
            ScheduleEntity(
                contactId = contactId,
                contactLookupKey = contactLookupKey,
                originalName = originalName,
                temporaryName = temporaryName,
                startAtMillis = startAtMillis,
                endAtMillis = endAtMillis,
                selectedDays = selectedDays,
                scheduledAlarmsMetadata = scheduledAlarmsMetadata
            )
        )
    }

    suspend fun update(entity: ScheduleEntity) = database.scheduleDao().update(entity)

    suspend fun deleteById(id: Long) = database.scheduleDao().deleteById(id)

    suspend fun getById(id: Long): ScheduleEntity? = database.scheduleDao().getById(id)

    suspend fun getAllEntities(): List<ScheduleEntity> = database.scheduleDao().getAll().first()

    suspend fun deleteByContactId(contactId: Long) = database.scheduleDao().deleteByContactId(contactId)


}
