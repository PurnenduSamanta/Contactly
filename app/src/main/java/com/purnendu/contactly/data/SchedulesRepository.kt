package com.purnendu.contactly.data

import android.content.Context
import androidx.room.Room
import com.purnendu.contactly.data.local.room.AppDatabase
import com.purnendu.contactly.data.local.room.ScheduleDao
import com.purnendu.contactly.data.local.room.ScheduleEntity
import com.purnendu.contactly.model.Schedule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SchedulesRepository private constructor(
    private val dao: ScheduleDao
) {
    fun getSchedules(): Flow<List<Schedule>> = dao.getAll().map { list ->
        list.map { e ->
            Schedule(
                id = e.scheduleId.toString(),
                name = e.temporaryName,
                originalName = e.originalName,
                avatarResId = null,
                contactId = e.contactId
            )
        }
    }

    suspend fun create(
        contactId: Long,
        contactLookupKey: String?,
        originalName: String,
        temporaryName: String,
        startAtMillis: Long,
        endAtMillis: Long
    ): Long {
        return dao.insert(
            ScheduleEntity(
                contactId = contactId,
                contactLookupKey = contactLookupKey,
                originalName = originalName,
                temporaryName = temporaryName,
                startAtMillis = startAtMillis,
                endAtMillis = endAtMillis
            )
        )
    }

    suspend fun update(entity: ScheduleEntity) = dao.update(entity)

    suspend fun deleteById(id: Long) = dao.deleteById(id)

    suspend fun getById(id: Long): ScheduleEntity? = dao.getById(id)

    companion object {
        @Volatile private var INSTANCE: SchedulesRepository? = null

        fun get(context: Context): SchedulesRepository =
            INSTANCE ?: synchronized(this) {
                val db = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "contactly.db"
                ).build()
                val repo = SchedulesRepository(db.scheduleDao())
                INSTANCE = repo
                repo
            }
    }
}
