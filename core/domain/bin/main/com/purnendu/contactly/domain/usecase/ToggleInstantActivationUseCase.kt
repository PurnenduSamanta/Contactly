package com.purnendu.contactly.domain.usecase

import com.purnendu.contactly.common.ActivationMode
import com.purnendu.contactly.domain.repository.ActivationsRepository
import com.purnendu.contactly.domain.repository.ContactsRepository

class ToggleInstantActivationUseCase(
    private val activationsRepository: ActivationsRepository,
    private val contactsRepository: ContactsRepository
) {
    suspend operator fun invoke(activationId: Long): Boolean {
        val activation = activationsRepository.getById(activationId) ?: return false
        if (activation.activationMode != ActivationMode.INSTANT) return false

        val shouldApply = activation.instantSwitchStatus != true
        if (shouldApply) {
            contactsRepository.applyContact(
                contactId = activation.contactId,
                name = activation.temporaryName,
                filePath = activation.temporaryImage,
                shouldRemovePhoto = activation.temporaryImage == null
            )
        } else {
            contactsRepository.applyContact(
                contactId = activation.contactId,
                name = activation.originalName,
                filePath = activation.originalImage,
                shouldRemovePhoto = activation.originalImage == null
            )
        }

        activationsRepository.update(activation.copy(instantSwitchStatus = shouldApply))
        return true
    }
}
