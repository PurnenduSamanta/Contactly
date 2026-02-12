package com.purnendu.contactly.data.repository

import android.content.ContentResolver
import android.provider.ContactsContract
import com.purnendu.contactly.model.Contact

/**
 * Repository for accessing device contacts.
 * 
 * Instance is managed by Koin DI as a singleton.
 */
class ContactsRepository(
    private val resolver: ContentResolver
) {
    fun fetchContacts(): List<Contact> {
        val contacts = mutableListOf<Contact>()
        val projection = arrayOf(
            ContactsContract.Contacts._ID,
            ContactsContract.Contacts.LOOKUP_KEY,
            ContactsContract.Contacts.PHOTO_URI
        )

        resolver.query(
            ContactsContract.Contacts.CONTENT_URI,
            projection,
            null,
            null,
            null
        )?.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(ContactsContract.Contacts._ID)
            val lookupIndex = cursor.getColumnIndexOrThrow(ContactsContract.Contacts.LOOKUP_KEY)
            val photoIndex = cursor.getColumnIndexOrThrow(ContactsContract.Contacts.PHOTO_URI)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idIndex)
                val lookup = cursor.getString(lookupIndex)
                val photo = cursor.getString(photoIndex)
                
                // Query first phone number
                val phone: String? = resolver.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID + "=?",
                    arrayOf(id.toString()),
                    null
                )?.use { phoneCursor ->
                    if (phoneCursor.moveToFirst()) {
                        phoneCursor.getString(
                            phoneCursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)
                        )
                    } else null
                }
                
                // Query structured name (returns null if no actual name exists)
                val structuredName: String? = resolver.query(
                    ContactsContract.Data.CONTENT_URI,
                    arrayOf(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME),
                    "${ContactsContract.Data.CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?",
                    arrayOf(id.toString(), ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE),
                    null
                )?.use { nameCursor ->
                    if (nameCursor.moveToFirst()) {
                        nameCursor.getString(
                            nameCursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME)
                        )?.takeIf { it.isNotBlank() }
                    } else null
                }
                
                contacts.add(
                    Contact(
                        name = structuredName,
                        phone = phone ?: "",
                        image = photo,
                        id = id,
                        lookupKey = lookup
                    )
                )
            }
        }
        
        // Sort by name (nulls last), then by phone
        return contacts.sortedWith(compareBy(nullsLast()) { it.name?.lowercase() })
    }

    fun fetchContactById(contactId: Long): Contact? {
        val projection = arrayOf(
            ContactsContract.Contacts._ID,
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
            if (cursor.moveToFirst()) {
                val idIndex = cursor.getColumnIndexOrThrow(ContactsContract.Contacts._ID)
                val lookupIndex = cursor.getColumnIndexOrThrow(ContactsContract.Contacts.LOOKUP_KEY)
                val photoIndex = cursor.getColumnIndexOrThrow(ContactsContract.Contacts.PHOTO_URI)

                val id = cursor.getLong(idIndex)
                val lookup = cursor.getString(lookupIndex)
                val photo = cursor.getString(photoIndex)
                
                // Query first phone number inline
                val phone: String? = resolver.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID + "=?",
                    arrayOf(id.toString()),
                    null
                )?.use { phoneCursor ->
                    if (phoneCursor.moveToFirst()) {
                        phoneCursor.getString(
                            phoneCursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)
                        )
                    } else null
                }

                // Query structured name from Data table (returns null if no actual name exists)
                val structuredName: String? = resolver.query(
                    ContactsContract.Data.CONTENT_URI,
                    arrayOf(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME),
                    "${ContactsContract.Data.CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?",
                    arrayOf(id.toString(), ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE),
                    null
                )?.use { nameCursor ->
                    if (nameCursor.moveToFirst()) {
                        nameCursor.getString(
                            nameCursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME)
                        )?.takeIf { it.isNotBlank() }
                    } else null
                }

                // Return Contact with name as null if no actual name exists
                // This allows caller to differentiate between:
                // - null Contact: contact doesn't exist
                // - Contact with null name: contact exists but has no name
                return Contact(
                    name = structuredName,
                    phone = phone ?: "",
                    image = photo,
                    id = id,
                    lookupKey = lookup
                )
            }
        }
        return null
    }
}
