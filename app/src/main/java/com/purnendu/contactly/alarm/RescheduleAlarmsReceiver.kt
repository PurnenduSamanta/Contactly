package com.purnendu.contactly.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.purnendu.contactly.data.SchedulesRepository
import com.purnendu.contactly.data.local.room.AppDatabase
import kotlinx.coroutines.runBlocking

class RescheduleAlarmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val now = System.currentTimeMillis()

        runBlocking {
            val repo = SchedulesRepository(AppDatabase.getDataBase(context))
            val entities = repo.getAllEntities()
            entities.forEach { e ->
                if (e.startAtMillis > now) {
                    val applyIntent = Intent(context, AliasAlarmReceiver::class.java).apply {
                        action = AliasAlarmReceiver.ACTION_ALIAS
                        putExtra(AliasAlarmReceiver.EXTRA_OPERATION, AliasAlarmReceiver.OP_APPLY)
                        putExtra(AliasAlarmReceiver.EXTRA_CONTACT_ID, e.contactId)
                        putExtra(AliasAlarmReceiver.EXTRA_NAME, e.temporaryName)
                    }
                    val applyReqCode = (e.contactId % Int.MAX_VALUE).toInt()
                    val applyPending = PendingIntent.getBroadcast(
                        context,
                        applyReqCode,
                        applyIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    try {
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            e.startAtMillis,
                            applyPending
                        )
                    } catch (_: SecurityException) {
                        alarmManager.set(AlarmManager.RTC_WAKEUP, e.startAtMillis, applyPending)
                    }
                }

                if (e.endAtMillis > now) {
                    val revertIntent = Intent(context, AliasAlarmReceiver::class.java).apply {
                        action = AliasAlarmReceiver.ACTION_ALIAS
                        putExtra(AliasAlarmReceiver.EXTRA_OPERATION, AliasAlarmReceiver.OP_REVERT)
                        putExtra(AliasAlarmReceiver.EXTRA_CONTACT_ID, e.contactId)
                        putExtra(AliasAlarmReceiver.EXTRA_NAME, e.originalName)
                    }
                    val revertReqCode = ((e.contactId % Int.MAX_VALUE).toInt() + 1)
                    val revertPending = PendingIntent.getBroadcast(
                        context,
                        revertReqCode,
                        revertIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    try {
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            e.endAtMillis,
                            revertPending
                        )
                    } catch (_: SecurityException) {
                        alarmManager.set(AlarmManager.RTC_WAKEUP, e.endAtMillis, revertPending)
                    }
                }
            }
        }
    }
}