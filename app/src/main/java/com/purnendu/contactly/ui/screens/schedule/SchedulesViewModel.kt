package com.purnendu.contactly.ui.screens.schedule

import android.Manifest
import android.app.Application
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.purnendu.contactly.data.ContactsRepository
import com.purnendu.contactly.data.SchedulesRepository
import com.purnendu.contactly.data.local.room.ScheduleEntity
import com.purnendu.contactly.model.Contact
import com.purnendu.contactly.model.Schedule
import com.purnendu.contactly.alarm.AliasAlarmReceiver
import com.purnendu.contactly.data.local.room.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SchedulesViewModel(private val application: Application) : AndroidViewModel(application) {
    private val schedulesRepo = SchedulesRepository(AppDatabase.getDataBase(application))
    private val contactsRepo by lazy { ContactsRepository.get(application) }

    private val _showContactPermissionDialog = MutableStateFlow(false)
    val showContactPermissionDialog: StateFlow<Boolean> = _showContactPermissionDialog

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    val schedules: StateFlow<List<Schedule>> = schedulesRepo.getSchedules().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _contacts = MutableStateFlow<List<Contact>>(emptyList())
    val contacts: StateFlow<List<Contact>> = _contacts

    init {checkCriticalPermissions()}

    fun loadContacts() {
        viewModelScope.launch {
            try {
                _contacts.value = contactsRepo.fetchContacts()
            } catch (e: SecurityException) {
                // Handle the case where contacts permissions are not granted
                _contacts.value = emptyList()
            }
        }
    }

    fun addSchedule(
        contact: Contact,
        temporaryName: String,
        startAtMillis: Long,
        endAtMillis: Long
    )
    {
        scheduleAlarms(
                contact = contact,
                originalName = contact.name,
                temporaryName = temporaryName,
                startAtMillis = startAtMillis,
                endAtMillis = endAtMillis)
    }

    fun deleteSchedule(schedule: Schedule) {
        val id = schedule.id.toLongOrNull() ?: return
        viewModelScope.launch { schedulesRepo.deleteById(id) }
    }

    suspend fun loadScheduleEntity(id: Long): ScheduleEntity? = schedulesRepo.getById(id)

    fun contactForId(id: Long): Contact? = try {
        contactsRepo.fetchContactById(id)
    } catch (e: SecurityException) {
        null // Return null if contacts permission is not granted
    }

    fun updateSchedule(
        scheduleId: Long,
        contact: Contact,
        temporaryName: String,
        startAtMillis: Long,
        endAtMillis: Long
    ) {
            scheduleAlarms(
                isUpdatingAlarm = true,
                contact = contact,
                originalName = contact.name,
                temporaryName = temporaryName,
                startAtMillis = startAtMillis,
                endAtMillis = endAtMillis
            )
    }

    fun checkCriticalPermissions() {

        val hasContactPermissions = ContextCompat.checkSelfPermission(
            application,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
            application,
            Manifest.permission.WRITE_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED

        // Update dialog states
        _showContactPermissionDialog.value = !hasContactPermissions
    }

    fun dismissContactPermissionDialog() {
        _showContactPermissionDialog.value = false
    }

    fun showError(message: String) {
        _errorMessage.value = message
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun canScheduleExactAlarmPermissions(): Boolean {
        val alarmManager = application.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        return  if (Build.VERSION.SDK_INT >= 31) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true // Exact alarms permission not required on older Android versions
        }
    }

    fun addAlarmToDatabase(
        contact: Contact,
        temporaryName: String,
        startAtMillis: Long,
        endAtMillis: Long
    )
    {
        val id = contact.id ?: return
        viewModelScope.launch(Dispatchers.IO)
        {
            schedulesRepo.create(
                contactId = id,
                contactLookupKey = contact.lookupKey,
                originalName = contact.name,
                temporaryName = temporaryName,
                startAtMillis = startAtMillis,
                endAtMillis = endAtMillis
            )
        }
    }

    fun updateAlarmToDatabase(
        scheduleId: Long,
        temporaryName: String,
        startAtMillis: Long,
        endAtMillis: Long
    )
    {
        viewModelScope.launch(Dispatchers.IO) {
            val current = schedulesRepo.getById(scheduleId) ?: return@launch
            val updated = current.copy(
                temporaryName = temporaryName,
                startAtMillis = startAtMillis,
                endAtMillis = endAtMillis
            )
            schedulesRepo.update(updated)
        }
    }

}

private fun SchedulesViewModel.scheduleAlarms(
    contact: Contact,
    originalName: String,
    temporaryName: String,
    startAtMillis: Long,
    endAtMillis: Long,
    scheduleId: Long?=null,
    isUpdatingAlarm: Boolean = false
)
{
    var isAlarmSuccessfullyScheduled: Boolean
    val contactId = contact.id.toString().toLongOrNull() ?: return

    val context = getApplication<Application>()
    val alarmManager = context.getSystemService(AlarmManager::class.java)

    val now = System.currentTimeMillis()
    val applyAt = startAtMillis.coerceAtLeast(now)
    val revertAt = endAtMillis.coerceAtLeast(now)

    val applyIntent = Intent(context, AliasAlarmReceiver::class.java).apply {
        action = AliasAlarmReceiver.ACTION_ALIAS
        putExtra(AliasAlarmReceiver.EXTRA_OPERATION, AliasAlarmReceiver.OP_APPLY)
        putExtra(AliasAlarmReceiver.EXTRA_CONTACT_ID, contact.id)
        putExtra(AliasAlarmReceiver.EXTRA_NAME, temporaryName)
    }
    val revertIntent = Intent(context, AliasAlarmReceiver::class.java).apply {
        action = AliasAlarmReceiver.ACTION_ALIAS
        putExtra(AliasAlarmReceiver.EXTRA_OPERATION, AliasAlarmReceiver.OP_REVERT)
        putExtra(AliasAlarmReceiver.EXTRA_CONTACT_ID, contact.id)
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
            AlarmManager.RTC,
            applyAt,
            applyPending
        )
        isAlarmSuccessfullyScheduled = true
    }
    catch (e: SecurityException) {
        Log.d("alarm_error", e.localizedMessage.toString())
        isAlarmSuccessfullyScheduled = false
    }
    try {
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC,
            revertAt,
            revertPending
        )
        isAlarmSuccessfullyScheduled = true
    }
    catch (e: SecurityException) {
        Log.d("alarm_error", e.localizedMessage.toString())
        isAlarmSuccessfullyScheduled = false
    }

    if(isAlarmSuccessfullyScheduled)
    {
        if(isUpdatingAlarm)
        {
            if(scheduleId==null)
                return

            updateAlarmToDatabase(
                scheduleId = scheduleId,
                temporaryName = temporaryName,
                startAtMillis = startAtMillis,
                endAtMillis
            )
        }
        else
        {
            addAlarmToDatabase(
                contact = contact,
                temporaryName = temporaryName,
                startAtMillis = startAtMillis,
                endAtMillis = endAtMillis
            )
        }
    }
}
