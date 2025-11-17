package com.purnendu.contactly.alarm

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.ContactsContract
import androidx.core.content.ContextCompat

class AliasAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val op = intent.getStringExtra(EXTRA_OPERATION) ?: return
        val contactId = intent.getLongExtra(EXTRA_CONTACT_ID, -1L)
        val newName = intent.getStringExtra(EXTRA_NAME) ?: return
        if (contactId <= 0) return

        if (!hasWriteContactsPermission(context)) return

        try {
            when (op) {
                OP_APPLY -> applyName(context, contactId, newName)
                OP_REVERT -> applyName(context, contactId, newName)
            }
        } catch (_: Throwable) {
        }
    }

    private fun hasWriteContactsPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.WRITE_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun applyName(context: Context, contactId: Long, name: String) {
        val resolver = context.contentResolver
        val rawId = resolver.query(
            ContactsContract.RawContacts.CONTENT_URI,
            arrayOf(ContactsContract.RawContacts._ID),
            ContactsContract.RawContacts.CONTACT_ID + "=?",
            arrayOf(contactId.toString()),
            null
        )?.use { c ->
            if (c.moveToFirst()) c.getLong(0) else null
        }

        val values = android.content.ContentValues().apply {
            put(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, name)
            put(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME, name)
        }

        val updated = resolver.update(
            ContactsContract.Data.CONTENT_URI,
            values,
            ContactsContract.Data.CONTACT_ID + "=? AND " +
                ContactsContract.Data.MIMETYPE + "=?",
            arrayOf(
                contactId.toString(),
                ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE
            )
        )

        if (updated == 0 && rawId != null) {
            val insertValues = android.content.ContentValues().apply {
                put(ContactsContract.Data.RAW_CONTACT_ID, rawId)
                put(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                put(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, name)
                put(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME, name)
            }
            resolver.insert(ContactsContract.Data.CONTENT_URI, insertValues)
        }
    }

    companion object {
        const val ACTION_ALIAS = "com.purnendu.contactly.action.ALIAS"
        const val EXTRA_OPERATION = "operation"
        const val EXTRA_CONTACT_ID = "contactId"
        const val EXTRA_NAME = "name"
        const val OP_APPLY = "apply"
        const val OP_REVERT = "revert"
    }
}