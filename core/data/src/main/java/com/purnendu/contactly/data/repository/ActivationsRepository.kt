package com.purnendu.contactly.data.repository

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.purnendu.contactly.common.AlarmOperations.OP_APPLY
import com.purnendu.contactly.common.AlarmOperations.OP_REVERT
import com.purnendu.contactly.domain.model.alarm.AlarmMetadata
import com.purnendu.contactly.data.local.room.AppDatabase
import com.purnendu.contactly.data.local.room.ActivationEntity
import com.purnendu.contactly.domain.model.Activation
import com.purnendu.contactly.common.ActivationMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class ActivationsRepository(
    private val database: AppDatabase,
    private val contactsRepo: ContactsRepository
) {
    private val gson = Gson()
    
    fun getActivations(): Flow<List<Activation>> = database.activationDao().getAll().map { list ->
        val currentTime = System.currentTimeMillis()
        list.map { e ->
            val activationMode = ActivationMode.fromInt(e.activationMode)

            // For INSTANT: active state from DB column
            // For NEARBY: check contact's current name (single source of truth)
            // For ONE_TIME/REPEAT: computed from alarm metadata
            val isActive = when (activationMode) {
                ActivationMode.INSTANT -> e.instantSwitchStatus == true
                ActivationMode.NEARBY -> {
                    val currentContact = contactsRepo.fetchContactById(e.contactId)
                    currentContact?.name == e.temporaryName
                }
                else -> checkIfCurrentlyActive(e.activatedAlarmsMetadata, currentTime)
            }
            
            Activation(
                id = e.activationId.toString(),
                name = e.temporaryName,
                originalName = e.originalName,
                avatarResId = null,
                contactId = e.contactId,
                selectedDays = e.selectedDays,
                startAtMillis = e.startAtMillis,
                endAtMillis = e.endAtMillis,
                activationMode = activationMode,
                isCurrentlyActive = isActive,
                temporaryImageUri = e.temporaryImage,
                originalImageUri = e.originalImage,
                latitude = e.latitude,
                longitude = e.longitude,
                radiusMeters = e.radiusMeters,
                locationLabel = e.locationLabel
            )
        }
    }
    
    /**
     * Check if an activation is currently active (between APPLY and REVERT operations).
     * An activation is active when an APPLY alarm has fired but the corresponding REVERT hasn't.
     * 
     * For ONE_TIME activations: APPLY time stays in the past after firing.
     * For REPEAT activations: APPLY time is reset for next week after firing,
     * so APPLY > REVERT indicates the active state (APPLY is next week, REVERT is today).
     * 
     * Note: NEARBY uses contact name check instead (see getActivations above).
     * 
     * @param metadataJson JSON string of AlarmMetadata list
     * @param currentTime Current time in milliseconds
     * @return True if the activation is currently active
     */
    private fun checkIfCurrentlyActive(metadataJson: String?, currentTime: Long): Boolean {
        if (metadataJson.isNullOrBlank()) return false
        
        return try {
            val type = object : TypeToken<List<AlarmMetadata>>() {}.type
            val alarmList: List<AlarmMetadata> = gson.fromJson(metadataJson, type) ?: emptyList()
            
            // ONE_TIME/REPEAT: pair-based logic
            val applyAlarms = alarmList.filter { it.operation == OP_APPLY }
            val revertAlarms = alarmList.filter { it.operation == OP_REVERT }
            
            // Check if any APPLY has fired but its corresponding REVERT hasn't
            // REVERT requestCode = APPLY requestCode + 1 (based on AlarmRequestCodeUtils scheme)
            applyAlarms.any { apply ->
                val correspondingRevert = revertAlarms.find { it.requestCode == apply.requestCode + 1 }
                if (correspondingRevert != null && correspondingRevert.triggerTimeMillis > currentTime) {
                    // REVERT hasn't fired yet
                    // Activation is ACTIVE if:
                    // 1. APPLY has already fired (past) - for ONE_TIME activations
                    // 2. OR APPLY is AFTER REVERT (reset for next week) - for REPEAT activations
                    apply.triggerTimeMillis <= currentTime || 
                        apply.triggerTimeMillis > correspondingRevert.triggerTimeMillis
                } else {
                    false
                }
            }
        } catch (e: Exception) {
            false
        }
    }

    suspend fun create(
        activationId: Long,
        contactId: Long,
        contactLookupKey: String?,
        originalName: String,
        temporaryName: String,
        startAtMillis: Long?,
        endAtMillis: Long?,
        selectedDays: Int?,
        activatedAlarmsMetadata: String? = null,
        activationMode: ActivationMode,
        tempImage: String? = null,
        originalImage: String? = null,
        instantSwitchStatus: Boolean? = null,
        latitude: Double? = null,
        longitude: Double? = null,
        radiusMeters: Float? = null,
        locationLabel: String? = null,
    ): Long {
        return database.activationDao().insert(
            ActivationEntity(
                activationId = activationId,
                contactId = contactId,
                contactLookupKey = contactLookupKey,
                originalName = originalName,
                temporaryName = temporaryName,
                startAtMillis = startAtMillis,
                endAtMillis = endAtMillis,
                selectedDays = selectedDays,
                activatedAlarmsMetadata = activatedAlarmsMetadata,
                activationMode = ActivationMode.toInt(activationMode),
                temporaryImage = tempImage,
                originalImage = originalImage,
                instantSwitchStatus = instantSwitchStatus,
                latitude = latitude,
                longitude = longitude,
                radiusMeters = radiusMeters,
                locationLabel = locationLabel,
            )
        )
    }

    suspend fun update(entity: ActivationEntity) = database.activationDao().update(entity)

    suspend fun deleteById(id: Long) = database.activationDao().deleteById(id)

    suspend fun getById(id: Long): ActivationEntity? = database.activationDao().getById(id)

    suspend fun getAllEntities(): List<ActivationEntity> = database.activationDao().getAll().first()

    suspend fun deleteByContactId(contactId: Long) = database.activationDao().deleteByContactId(contactId)


}
