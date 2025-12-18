package com.purnendu.contactly.alarm

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.ContactsContract
import android.util.Log
import androidx.core.content.ContextCompat
import com.purnendu.contactly.utils.AlarmRequestCodeUtils
import java.util.Calendar

class AliasAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val op = intent.getStringExtra(EXTRA_OPERATION) ?: return
        val contactId = intent.getLongExtra(EXTRA_CONTACT_ID, -1L)
        val newName = intent.getStringExtra(EXTRA_NAME) ?: return
        val scheduleId = intent.getLongExtra(EXTRA_SCHEDULE_ID, -1L)
        val dayOfWeek = intent.getIntExtra(EXTRA_DAY_OF_WEEK, -1)
        if (contactId <= 0) return

        if (!hasWriteContactsPermission(context)) return

        try {
            when (op) {
                OP_APPLY -> applyName(context, contactId, newName)
                OP_REVERT -> applyName(context, contactId, newName)
            }
            
            // Reschedule for next week if this is a recurring alarm
            if (dayOfWeek >= 0) {
                rescheduleForNextWeek(context, intent, dayOfWeek)
            }
        } catch (e: Throwable) {
            Log.e("AliasAlarmReceiver", "Error processing alarm", e)
        }
    }

    private fun hasWriteContactsPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.WRITE_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun rescheduleForNextWeek(context: Context, originalIntent: Intent, dayOfWeek: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        
        // Calculate next occurrence (same day, next week)
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.WEEK_OF_YEAR, 1)
        
        // Adjust to exact day of week
        val currentDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1 // 0=Sunday
        val daysToAdd = (dayOfWeek - currentDayOfWeek + 7) % 7
        if (daysToAdd != 0) {
            calendar.add(Calendar.DAY_OF_YEAR, daysToAdd)
        }
        
        val nextTriggerTime = calendar.timeInMillis

        // Create a new intent with same extras
        val newIntent = Intent(context, AliasAlarmReceiver::class.java).apply {
            action = originalIntent.action
            putExtra(EXTRA_OPERATION, originalIntent.getStringExtra(EXTRA_OPERATION))
            putExtra(EXTRA_CONTACT_ID, originalIntent.getLongExtra(EXTRA_CONTACT_ID, -1L))
            putExtra(EXTRA_NAME, originalIntent.getStringExtra(EXTRA_NAME))
            putExtra(EXTRA_SCHEDULE_ID, originalIntent.getLongExtra(EXTRA_SCHEDULE_ID, -1L))
            putExtra(EXTRA_DAY_OF_WEEK, dayOfWeek)
        }

        // Generate request code using centralized utility
        val contactId = originalIntent.getLongExtra(EXTRA_CONTACT_ID, -1L)
        val op = originalIntent.getStringExtra(EXTRA_OPERATION)
        val reqCode = if (op == OP_APPLY) {
            AlarmRequestCodeUtils.generateApplyRequestCode(contactId, dayOfWeek)
        } else {
            AlarmRequestCodeUtils.generateRevertRequestCode(contactId, dayOfWeek)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reqCode,
            newIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC,
                nextTriggerTime,
                pendingIntent
            )
            Log.d("AliasAlarmReceiver", "Rescheduled alarm for next week: day=$dayOfWeek, time=$nextTriggerTime")
        } catch (e: SecurityException) {
            Log.e("AliasAlarmReceiver", "Failed to reschedule alarm", e)
        }
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
        const val EXTRA_SCHEDULE_ID = "scheduleId"
        const val EXTRA_DAY_OF_WEEK = "dayOfWeek"
        const val OP_APPLY = "apply"
        const val OP_REVERT = "revert"
    }
}