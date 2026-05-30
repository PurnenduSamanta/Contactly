package com.purnendu.contactly.domain.usecase

import com.purnendu.contactly.domain.model.Contact
import com.purnendu.contactly.domain.repository.ContactsRepository

class FetchContactsUseCase(
    private val contactsRepository: ContactsRepository
) {
    operator fun invoke(): List<Contact> = contactsRepository.fetchContacts()

    fun byId(contactId: Long): Contact? = contactsRepository.fetchContactById(contactId)
}
