package com.purnendu.contactly.data.repository

import android.content.ContentResolver
import android.content.Context
import android.provider.ContactsContract
import com.purnendu.contactly.model.Contact

class ContactsRepository private constructor(
    private val resolver: ContentResolver
) {
    fun fetchContacts(): List<Contact> {
        val contacts = mutableListOf<Contact>()
        val projection = arrayOf(
            ContactsContract.Contacts._ID,
            ContactsContract.Contacts.DISPLAY_NAME_PRIMARY,
            ContactsContract.Contacts.LOOKUP_KEY,
            ContactsContract.Contacts.PHOTO_URI
        )

        resolver.query(
            ContactsContract.Contacts.CONTENT_URI,
            projection,
            null,
            null,
            ContactsContract.Contacts.DISPLAY_NAME_PRIMARY + " COLLATE NOCASE ASC"
        )?.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(ContactsContract.Contacts._ID)
            val nameIndex = cursor.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY)
            val lookupIndex = cursor.getColumnIndexOrThrow(ContactsContract.Contacts.LOOKUP_KEY)
            val photoIndex = cursor.getColumnIndexOrThrow(ContactsContract.Contacts.PHOTO_URI)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idIndex)
                val name = cursor.getString(nameIndex) ?: ""
                val lookup = cursor.getString(lookupIndex)
                val photo = cursor.getString(photoIndex)
                val phone = firstPhoneFor(id)
                contacts.add(
                    Contact(
                        name = name,
                        phone = phone ?: "",
                        image = photo,
                        id = id,
                        lookupKey = lookup
                    )
                )
            }
        }
        return contacts
    }

    fun fetchContactById(contactId: Long): Contact? {
        val projection = arrayOf(
            ContactsContract.Contacts._ID,
            ContactsContract.Contacts.DISPLAY_NAME_PRIMARY,
            ContactsContract.Contacts.LOOKUP_KEY,
            ContactsContract.Contacts.PHOTO_URI
        )
        resolver.query(
            ContactsContract.Contacts.CONTENT_URI,
            projection,
            ContactsContract.Contacts._ID + "=?",
            arrayOf(contactId.toString()),
            null
        )?.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(ContactsContract.Contacts._ID)
            val nameIndex = cursor.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY)
            val lookupIndex = cursor.getColumnIndexOrThrow(ContactsContract.Contacts.LOOKUP_KEY)
            val photoIndex = cursor.getColumnIndexOrThrow(ContactsContract.Contacts.PHOTO_URI)
            if (cursor.moveToFirst()) {
                val id = cursor.getLong(idIndex)
                val name = cursor.getString(nameIndex) ?: ""
                val lookup = cursor.getString(lookupIndex)
                val photo = cursor.getString(photoIndex)
                val phone = firstPhoneFor(id)
                return Contact(name = name, phone = phone ?: "", image = photo, id = id, lookupKey = lookup)
            }
        }
        return null
    }

    private fun firstPhoneFor(contactId: Long): String? {
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )
        resolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            projection,
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID + "=?",
            arrayOf(contactId.toString()),
            null
        )?.use { cursor ->
            val numberIndex = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)
            if (cursor.moveToFirst()) {
                return cursor.getString(numberIndex)
            }
        }
        return null
    }

    companion object {
        @Volatile private var INSTANCE: ContactsRepository? = null
        fun get(context: Context): ContactsRepository =
            INSTANCE ?: synchronized(this) {
                val repo = ContactsRepository(context.contentResolver)
                INSTANCE = repo
                repo
            }
    }
}
