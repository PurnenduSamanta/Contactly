package com.purnendu.contactly.data.repository

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.purnendu.contactly.alarm.AliasAlarmReceiver.Companion.OP_APPLY
import com.purnendu.contactly.alarm.AliasAlarmReceiver.Companion.OP_REVERT
import com.purnendu.contactly.alarm.models.AlarmMetadata
import com.purnendu.contactly.data.local.room.AppDatabase
import com.purnendu.contactly.data.local.room.ScheduleEntity
import com.purnendu.contactly.model.Schedule
import com.purnendu.contactly.utils.ScheduleType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class SchedulesRepository(private val database: AppDatabase) {
    private val gson = Gson()
    
    fun getSchedules(): Flow<List<Schedule>> = database.scheduleDao().getAll().map { list ->
        val currentTime = System.currentTimeMillis()
        list.map { e ->
            // Check if schedule is currently active (between APPLY and REVERT)
            val isActive = checkIfCurrentlyActive(e.scheduledAlarmsMetadata, currentTime)
            
            Schedule(
                id = e.scheduleId.toString(),
                name = e.temporaryName,
                originalName = e.originalName,
                avatarResId = null,
                contactId = e.contactId,
                selectedDays = e.selectedDays,
                startAtMillis = e.startAtMillis,
                endAtMillis = e.endAtMillis,
                scheduleType = if (e.scheduleType == 0) ScheduleType.ONE_TIME else ScheduleType.REPEAT,
                isCurrentlyActive = isActive
            )
        }
    }
    
    /**
     * Check if a schedule is currently active (between APPLY and REVERT operations).
     * A schedule is active when an APPLY alarm has fired but the corresponding REVERT hasn't.
     * 
     * @param metadataJson JSON string of AlarmMetadata list
     * @param currentTime Current time in milliseconds
     * @return True if the schedule is currently active
     */
    private fun checkIfCurrentlyActive(metadataJson: String?, currentTime: Long): Boolean {
        if (metadataJson.isNullOrBlank()) return false
        
        return try {
            val type = object : TypeToken<List<AlarmMetadata>>() {}.type
            val alarmList: List<AlarmMetadata> = gson.fromJson(metadataJson, type) ?: emptyList()
            
            // Separate APPLY and REVERT alarms
            val applyAlarms = alarmList.filter { it.operation == OP_APPLY }
            val revertAlarms = alarmList.filter { it.operation == OP_REVERT }
            
            // Check if any APPLY has fired but its corresponding REVERT hasn't
            // REVERT requestCode = APPLY requestCode + 1 (based on AlarmRequestCodeUtils scheme)
            applyAlarms.any { apply ->
                val correspondingRevert = revertAlarms.find { it.requestCode == apply.requestCode + 1 }
                if (correspondingRevert != null) {
                    // APPLY has fired (past) AND REVERT hasn't fired yet (future)
                    apply.triggerTimeMillis <= currentTime && correspondingRevert.triggerTimeMillis > currentTime
                } else {
                    false
                }
            }
        } catch (e: Exception) {
            false
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
