package com.purnendu.contactly.domain.repository

import com.purnendu.contactly.domain.model.Contact

interface ContactsRepository {
    fun fetchContacts(): List<Contact>
    fun fetchContactById(contactId: Long): Contact?
    fun applyContact(
        contactId: Long,
        name: String,
        filePath: String? = null,
        shouldRemovePhoto: Boolean = false
    )
}
