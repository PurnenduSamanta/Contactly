package com.purnendu.contactly.ui.screens.schedule

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.purnendu.contactly.data.ContactsRepository
import com.purnendu.contactly.data.SchedulesRepository
import com.purnendu.contactly.data.local.room.ScheduleEntity
import com.purnendu.contactly.model.Contact
import com.purnendu.contactly.model.Schedule
import com.purnendu.contactly.work.AliasWorker
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SchedulesViewModel(app: Application) : AndroidViewModel(app) {
    private val schedulesRepo = SchedulesRepository.get(app)
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
            scheduleWorkers(contactId = id, originalName = contact.name, temporaryName = temporaryName, startAtMillis = startAtMillis, endAtMillis = endAtMillis)
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
            scheduleWorkers(contactId = contact.id ?: return@launch, originalName = contact.name, temporaryName = temporaryName, startAtMillis = startAtMillis, endAtMillis = endAtMillis)
        }
    }
}

private fun SchedulesViewModel.scheduleWorkers(
    contactId: Long,
    originalName: String,
    temporaryName: String,
    startAtMillis: Long,
    endAtMillis: Long
) {
    val now = System.currentTimeMillis()
    val applyDelay = (startAtMillis - now).coerceAtLeast(0L)
    val revertDelay = (endAtMillis - now).coerceAtLeast(0L)

    val applyData = Data.Builder()
        .putString(AliasWorker.KEY_OPERATION, AliasWorker.OP_APPLY)
        .putLong(AliasWorker.KEY_CONTACT_ID, contactId)
        .putString(AliasWorker.KEY_NAME, temporaryName)
        .build()

    val revertData = Data.Builder()
        .putString(AliasWorker.KEY_OPERATION, AliasWorker.OP_REVERT)
        .putLong(AliasWorker.KEY_CONTACT_ID, contactId)
        .putString(AliasWorker.KEY_NAME, originalName)
        .build()

    val applyReq = OneTimeWorkRequestBuilder<AliasWorker>()
        .setInitialDelay(applyDelay, TimeUnit.MILLISECONDS)
        .setInputData(applyData)
        .build()

    val revertReq = OneTimeWorkRequestBuilder<AliasWorker>()
        .setInitialDelay(revertDelay, TimeUnit.MILLISECONDS)
        .setInputData(revertData)
        .build()

    WorkManager.getInstance(getApplication()).enqueue(applyReq)
    WorkManager.getInstance(getApplication()).enqueue(revertReq)
}
