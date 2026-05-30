package com.purnendu.contactly.domain.usecase

import com.purnendu.contactly.common.ActivationMode
import com.purnendu.contactly.common.AlarmOperations.OP_APPLY
import com.purnendu.contactly.common.AlarmOperations.OP_REVERT
import com.purnendu.contactly.domain.model.ActivationRecord
import com.purnendu.contactly.domain.repository.ActivationsRepository
import com.purnendu.contactly.domain.repository.AlarmSchedulerRepository
import com.purnendu.contactly.domain.repository.ContactsRepository
import com.purnendu.contactly.domain.repository.GeofenceRepository
import com.purnendu.contactly.domain.repository.ImageStorageRepository

class CreateActivationUseCase(
    private val activationsRepository: ActivationsRepository,
    private val contactsRepository: ContactsRepository,
    private val alarmSchedulerRepository: AlarmSchedulerRepository,
    private val imageStorageRepository: ImageStorageRepository,
    private val geofenceRepository: GeofenceRepository
) {
    suspend operator fun invoke(command: ActivationCommand): ActivationUseCaseResult {
        val contactId = command.contact.id
            ?: return ActivationUseCaseResult(success = false, errorMessage = "Contact not found")

        val tempImagePath = command.tempImageUri?.let {
            imageStorageRepository.saveTemporaryImage(command.activationId, it)
        }
        val originalImagePath = imageStorageRepository.saveOriginalImage(command.activationId, contactId)

        return when (command.activationMode) {
            ActivationMode.INSTANT -> createInstantActivation(
                command = command,
                contactId = contactId,
                tempImagePath = tempImagePath,
                originalImagePath = originalImagePath
            )

            ActivationMode.NEARBY -> createNearbyActivation(
                command = command,
                contactId = contactId,
                tempImagePath = tempImagePath,
                originalImagePath = originalImagePath
            )

            ActivationMode.ONE_TIME,
            ActivationMode.REPEAT -> createTimeBasedActivation(
                command = command,
                contactId = contactId,
                tempImagePath = tempImagePath,
                originalImagePath = originalImagePath
            )
        }
    }

    private suspend fun createInstantActivation(
        command: ActivationCommand,
        contactId: Long,
        tempImagePath: String?,
        originalImagePath: String?
    ): ActivationUseCaseResult {
        activationsRepository.create(
            ActivationRecord(
                activationId = command.activationId,
                contactId = contactId,
                contactLookupKey = command.contact.lookupKey,
                originalName = command.contact.name.orEmpty(),
                temporaryName = command.temporaryName,
                temporaryImage = tempImagePath,
                originalImage = originalImagePath,
                startAtMillis = null,
                endAtMillis = null,
                selectedDays = null,
                activatedAlarmsMetadata = null,
                activationMode = ActivationMode.INSTANT,
                instantSwitchStatus = true
            )
        )

        contactsRepository.applyContact(
            contactId = contactId,
            name = command.temporaryName,
            filePath = tempImagePath,
            shouldRemovePhoto = tempImagePath == null
        )

        return ActivationUseCaseResult(success = true)
    }

    private suspend fun createNearbyActivation(
        command: ActivationCommand,
        contactId: Long,
        tempImagePath: String?,
        originalImagePath: String?
    ): ActivationUseCaseResult {
        val latitude = command.latitude
            ?: return ActivationUseCaseResult(success = false, errorMessage = "Latitude is required")
        val longitude = command.longitude
            ?: return ActivationUseCaseResult(success = false, errorMessage = "Longitude is required")
        val radiusMeters = command.radiusMeters
            ?: return ActivationUseCaseResult(success = false, errorMessage = "Radius is required")

        val geofenceRegistered = geofenceRepository.registerGeofence(
            activationId = command.activationId,
            latitude = latitude,
            longitude = longitude,
            radiusMeters = radiusMeters
        )

        if (!geofenceRegistered) {
            return ActivationUseCaseResult(
                success = false,
                errorMessage = "Location permission is required for Nearby activations. Please grant location permission from Settings."
            )
        }

        activationsRepository.create(
            ActivationRecord(
                activationId = command.activationId,
                contactId = contactId,
                contactLookupKey = command.contact.lookupKey,
                originalName = command.contact.name.orEmpty(),
                temporaryName = command.temporaryName,
                temporaryImage = tempImagePath,
                originalImage = originalImagePath,
                startAtMillis = null,
                endAtMillis = null,
                selectedDays = null,
                activatedAlarmsMetadata = null,
                activationMode = ActivationMode.NEARBY,
                instantSwitchStatus = null,
                latitude = latitude,
                longitude = longitude,
                radiusMeters = radiusMeters,
                locationLabel = command.locationLabel
            )
        )

        return ActivationUseCaseResult(success = true)
    }

    private suspend fun createTimeBasedActivation(
        command: ActivationCommand,
        contactId: Long,
        tempImagePath: String?,
        originalImagePath: String?
    ): ActivationUseCaseResult {
        val startAtMillis = command.startAtMillis
            ?: return ActivationUseCaseResult(success = false, errorMessage = "Start time is required")
        val endAtMillis = command.endAtMillis
            ?: return ActivationUseCaseResult(success = false, errorMessage = "End time is required")
        val selectedDays = command.selectedDays
            ?: return ActivationUseCaseResult(success = false, errorMessage = "Selected days are required")

        val alarmResult = alarmSchedulerRepository.activateAlarms(
            contact = command.contact,
            activationId = command.activationId,
            originalName = command.contact.name.orEmpty(),
            temporaryName = command.temporaryName,
            tempImage = tempImagePath,
            originalImage = originalImagePath,
            startAtMillis = startAtMillis,
            endAtMillis = endAtMillis,
            selectedDays = selectedDays,
            activationMode = command.activationMode
        )

        if (!alarmResult.success) {
            return ActivationUseCaseResult(success = false, errorMessage = "Failed to activate alarms")
        }

        val nearestStartAt = alarmResult.alarmMetadata
            .filter { it.operation == OP_APPLY }
            .minByOrNull { it.triggerTimeMillis }
            ?.triggerTimeMillis ?: startAtMillis
        val nearestEndAt = alarmResult.alarmMetadata
            .filter { it.operation == OP_REVERT }
            .minByOrNull { it.triggerTimeMillis }
            ?.triggerTimeMillis ?: endAtMillis

        activationsRepository.create(
            ActivationRecord(
                activationId = command.activationId,
                contactId = contactId,
                contactLookupKey = command.contact.lookupKey,
                originalName = command.contact.name.orEmpty(),
                temporaryName = command.temporaryName,
                temporaryImage = tempImagePath,
                originalImage = originalImagePath,
                startAtMillis = nearestStartAt,
                endAtMillis = nearestEndAt,
                selectedDays = selectedDays,
                activatedAlarmsMetadata = alarmSchedulerRepository.toJson(alarmResult.alarmMetadata),
                activationMode = command.activationMode,
                instantSwitchStatus = null
            )
        )

        return ActivationUseCaseResult(success = true)
    }
}
