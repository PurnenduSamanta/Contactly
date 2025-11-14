package com.purnendu.contactly.work

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class AliasWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val op = inputData.getString(KEY_OPERATION) ?: return Result.failure()
        val contactId = inputData.getLong(KEY_CONTACT_ID, -1L)
        val newName = inputData.getString(KEY_NAME) ?: return Result.failure()
        if (contactId <= 0) return Result.failure()

        if (!hasWriteContactsPermission()) return Result.success()

        return try {
            when (op) {
                OP_APPLY -> applyName(contactId, newName)
                OP_REVERT -> applyName(contactId, newName)
                else -> {}
            }
            Result.success()
        } catch (t: Throwable) {
            Result.retry()
        }
    }

    private fun hasWriteContactsPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            applicationContext,
            Manifest.permission.WRITE_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun applyName(contactId: Long, name: String) {
        val resolver = applicationContext.contentResolver
        val dataUri = ContactsContract.Data.CONTENT_URI
        val selection = ContactsContract.Data.CONTACT_ID.toString() + "=? AND " +
                ContactsContract.Data.MIMETYPE + "=?"
        val args = arrayOf(
            contactId.toString(),
            ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE
        )
        val values = android.content.ContentValues().apply {
            put(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, name)
            put(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME, name)
        }
        resolver.update(dataUri, values, selection, args)
    }

    companion object {
        const val KEY_OPERATION = "operation"
        const val KEY_CONTACT_ID = "contactId"
        const val KEY_NAME = "name"
        const val OP_APPLY = "apply"
        const val OP_REVERT = "revert"
    }
}
