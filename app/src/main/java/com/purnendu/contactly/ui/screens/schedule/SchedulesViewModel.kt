package com.purnendu.contactly.ui.screens.schedule

import android.app.Application
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.purnendu.contactly.data.ContactsRepository
import com.purnendu.contactly.data.SchedulesRepository
import com.purnendu.contactly.data.local.room.ScheduleEntity
import com.purnendu.contactly.model.Contact
import com.purnendu.contactly.model.Schedule
import com.purnendu.contactly.alarm.AliasAlarmReceiver
import com.purnendu.contactly.data.local.room.AppDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SchedulesViewModel(app: Application) : AndroidViewModel(app) {
    private val schedulesRepo = SchedulesRepository(AppDatabase.getDataBase(app))
    private val contactsRepo = ContactsRepository.get(app)

    val schedules: StateFlow<List<Schedule>> = schedulesRepo
        .getSchedules()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _contacts = MutableStateFlow<List<Contact>>(emptyList())
    val contacts: StateFlow<List<Contact>> = _contacts

    fun loadContacts() {
        viewModelScope.launch {
            _contacts.value = contactsRepo.fetchContacts()
        }
    }

    fun addSchedule(
        contact: Contact,
        temporaryName: String,
        startAtMillis: Long,
        endAtMillis: Long
    ) {
        val id = contact.id ?: return
        viewModelScope.launch {
            schedulesRepo.create(
                contactId = id,
                contactLookupKey = contact.lookupKey,
                originalName = contact.name,
                temporaryName = temporaryName,
                startAtMillis = startAtMillis,
                endAtMillis = endAtMillis
            )
            scheduleAlarms(
                contactId = id,
                originalName = contact.name,
                temporaryName = temporaryName,
                startAtMillis = startAtMillis,
                endAtMillis = endAtMillis
            )
        }
    }

    fun deleteSchedule(schedule: Schedule) {
        val id = schedule.id.toLongOrNull() ?: return
        viewModelScope.launch { schedulesRepo.deleteById(id) }
    }

    suspend fun loadScheduleEntity(id: Long): ScheduleEntity? = schedulesRepo.getById(id)

    fun contactForId(id: Long): Contact? = contactsRepo.fetchContactById(id)

    fun updateSchedule(
        scheduleId: Long,
        contact: Contact,
        temporaryName: String,
        startAtMillis: Long,
        endAtMillis: Long
    ) {
        viewModelScope.launch {
            val current = schedulesRepo.getById(scheduleId) ?: return@launch
            val updated = current.copy(
                temporaryName = temporaryName,
                startAtMillis = startAtMillis,
                endAtMillis = endAtMillis
            )
            schedulesRepo.update(updated)
            scheduleAlarms(
                contactId = contact.id ?: return@launch,
                originalName = contact.name,
                temporaryName = temporaryName,
                startAtMillis = startAtMillis,
                endAtMillis = endAtMillis
            )
        }
    }
}

private fun SchedulesViewModel.scheduleAlarms(
    contactId: Long,
    originalName: String,
    temporaryName: String,
    startAtMillis: Long,
    endAtMillis: Long
) {
    val context = getApplication<Application>()
    val alarmManager = context.getSystemService(AlarmManager::class.java)

    val now = System.currentTimeMillis()
    val applyAt = startAtMillis.coerceAtLeast(now)
    val revertAt = endAtMillis.coerceAtLeast(now)

    val applyIntent = Intent(context, AliasAlarmReceiver::class.java).apply {
        action = AliasAlarmReceiver.ACTION_ALIAS
        putExtra(AliasAlarmReceiver.EXTRA_OPERATION, AliasAlarmReceiver.OP_APPLY)
        putExtra(AliasAlarmReceiver.EXTRA_CONTACT_ID, contactId)
        putExtra(AliasAlarmReceiver.EXTRA_NAME, temporaryName)
    }
    val revertIntent = Intent(context, AliasAlarmReceiver::class.java).apply {
        action = AliasAlarmReceiver.ACTION_ALIAS
        putExtra(AliasAlarmReceiver.EXTRA_OPERATION, AliasAlarmReceiver.OP_REVERT)
        putExtra(AliasAlarmReceiver.EXTRA_CONTACT_ID, contactId)
        putExtra(AliasAlarmReceiver.EXTRA_NAME, originalName)
    }

    val applyReqCode = (contactId % Int.MAX_VALUE).toInt()
    val revertReqCode = ((contactId % Int.MAX_VALUE).toInt() + 1)

    val applyPending = PendingIntent.getBroadcast(
        context,
        applyReqCode,
        applyIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    val revertPending = PendingIntent.getBroadcast(
        context,
        revertReqCode,
        revertIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    try {
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            applyAt,
            applyPending
        )
    } catch (_: SecurityException) {
        alarmManager.set(AlarmManager.RTC_WAKEUP, applyAt, applyPending)
    }
    try {
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            revertAt,
            revertPending
        )
    } catch (_: SecurityException) {
        alarmManager.set(AlarmManager.RTC_WAKEUP, revertAt, revertPending)
    }
}
