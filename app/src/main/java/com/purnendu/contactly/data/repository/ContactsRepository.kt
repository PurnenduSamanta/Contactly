package com.purnendu.contactly.data.repository

import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.net.Uri
import android.provider.ContactsContract
import android.util.Log
import com.purnendu.contactly.model.Contact

/**
 * Repository for accessing and modifying device contacts.
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

    /**
     * Applies a name and optional photo to a contact.
     *
     * This is used for both APPLY (set temporary name/photo) and REVERT (restore original)
     * operations. It updates the contact's structured name and handles photo changes
     * (set from file, or remove).
     *
     * @param contactId The contact's ID in the system contacts database
     * @param name The name to set on the contact
     * @param filePath Absolute path to the image file to set, or null to skip photo update
     * @param shouldRemovePhoto If true and filePath is null, explicitly removes the contact's photo
     */
    fun applyContact(
        contactId: Long,
        name: String,
        filePath: String? = null,
        shouldRemovePhoto: Boolean = false
    ) {
        // 1️⃣ Get RAW_CONTACT_ID
        val rawId = resolver.query(
            ContactsContract.RawContacts.CONTENT_URI,
            arrayOf(ContactsContract.RawContacts._ID),
            "${ContactsContract.RawContacts.CONTACT_ID}=?",
            arrayOf(contactId.toString()),
            null
        )?.use { c ->
            if (c.moveToFirst()) c.getLong(0) else null
        } ?: return

        // 2️⃣ Update or insert name (clear all component fields to prevent duplication)
        val nameValues = ContentValues().apply {
            put(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, name)
            put(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME, name)
            put(ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME, "")
            put(ContactsContract.CommonDataKinds.StructuredName.MIDDLE_NAME, "")
            put(ContactsContract.CommonDataKinds.StructuredName.PREFIX, "")
            put(ContactsContract.CommonDataKinds.StructuredName.SUFFIX, "")
        }

        val nameUpdated = resolver.update(
            ContactsContract.Data.CONTENT_URI,
            nameValues,
            "${ContactsContract.Data.CONTACT_ID}=? AND ${ContactsContract.Data.MIMETYPE}=?",
            arrayOf(
                contactId.toString(),
                ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE
            )
        )

        if (nameUpdated == 0) {
            val insertName = ContentValues().apply {
                put(ContactsContract.Data.RAW_CONTACT_ID, rawId)
                put(ContactsContract.Data.MIMETYPE,
                    ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                put(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, name)
                put(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME, name)
                put(ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME, "")
                put(ContactsContract.CommonDataKinds.StructuredName.MIDDLE_NAME, "")
                put(ContactsContract.CommonDataKinds.StructuredName.PREFIX, "")
                put(ContactsContract.CommonDataKinds.StructuredName.SUFFIX, "")
            }
            resolver.insert(ContactsContract.Data.CONTENT_URI, insertName)
        }

        // 3️⃣ Handle photo
        if (filePath == null) {
            // If shouldRemovePhoto is true, explicitly remove the photo
            if (shouldRemovePhoto) {
                try {
                    // Delete the photo data row - the proper Android way
                    val rowsDeleted = resolver.delete(
                        ContactsContract.Data.CONTENT_URI,
                        "${ContactsContract.Data.RAW_CONTACT_ID}=? AND ${ContactsContract.Data.MIMETYPE}=?",
                        arrayOf(
                            rawId.toString(),
                            ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE
                        )
                    )

                    Log.d("ContactsRepository", "Removed photo for contact: $contactId (rows deleted: $rowsDeleted)")
                } catch (e: Exception) {
                    Log.e("ContactsRepository", "Failed to remove photo", e)
                }
            }
            return
        }

        // 4️⃣ Apply photo from file
        try {
            // Read bytes from internal storage file
            val file = java.io.File(filePath)
            if (!file.exists()) {
                Log.e("ContactsRepository", "Image file does not exist: $filePath")
                return
            }

            val photoBytes = file.readBytes()

            if (photoBytes.isEmpty()) {
                Log.e("ContactsRepository", "Image file is empty: $filePath")
                return
            }

            // First, delete existing photo to ensure clean update
            try {
                val rowsDeleted = resolver.delete(
                    ContactsContract.Data.CONTENT_URI,
                    "${ContactsContract.Data.RAW_CONTACT_ID}=? AND ${ContactsContract.Data.MIMETYPE}=?",
                    arrayOf(
                        rawId.toString(),
                        ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE
                    )
                )
                Log.d("ContactsRepository", "Deleted old photo entries: $rowsDeleted")
            } catch (e: Exception) {
                Log.w("ContactsRepository", "Could not delete old photo (may not exist): ${e.message}")
            }

            // Now write the new photo
            val photoUri = Uri.withAppendedPath(
                ContentUris.withAppendedId(
                    ContactsContract.RawContacts.CONTENT_URI,
                    rawId
                ),
                ContactsContract.RawContacts.DisplayPhoto.CONTENT_DIRECTORY
            )

            resolver.openOutputStream(photoUri, "w")?.use { stream ->
                stream.write(photoBytes)
                stream.flush()
            }

            Log.d("ContactsRepository", "Successfully wrote ${photoBytes.size} bytes from file: $filePath")
        } catch (e: SecurityException) {
            Log.e("ContactsRepository", "SecurityException applying photo - check WRITE_CONTACTS permission", e)
        }
    }
}
