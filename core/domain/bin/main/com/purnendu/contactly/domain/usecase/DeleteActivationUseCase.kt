package com.purnendu.contactly.domain.usecase

import com.purnendu.contactly.common.ActivationMode
import com.purnendu.contactly.domain.model.Activation
import com.purnendu.contactly.domain.repository.ActivationsRepository
import com.purnendu.contactly.domain.repository.AlarmSchedulerRepository
import com.purnendu.contactly.domain.repository.ContactsRepository
import com.purnendu.contactly.domain.repository.GeofenceRepository
import com.purnendu.contactly.domain.repository.ImageStorageRepository

class DeleteActivationUseCase(
    private val activationsRepository: ActivationsRepository,
    private val contactsRepository: ContactsRepository,
    private val alarmSchedulerRepository: AlarmSchedulerRepository,
    private val geofenceRepository: GeofenceRepository,
    private val imageStorageRepository: ImageStorageRepository
) {
    suspend operator fun invoke(activation: Activation): Boolean {
        val id = activation.id.toLongOrNull() ?: return false

        return runCatching {
            val existing = activationsRepository.getById(id)

            when (activation.activationMode) {
                ActivationMode.INSTANT -> Unit
                ActivationMode.NEARBY -> geofenceRepository.unregisterGeofence(id)
                else -> alarmSchedulerRepository.cancelActivatedAlarms(id)
            }

            if (existing != null) {
                contactsRepository.applyContact(
                    contactId = existing.contactId,
                    name = existing.originalName,
                    filePath = existing.originalImage,
                    shouldRemovePhoto = existing.originalImage == null
                )
            }

            activationsRepository.deleteById(id)
            imageStorageRepository.deleteImagesFromActivation(id)
            true
        }.getOrElse { false }
    }
}
