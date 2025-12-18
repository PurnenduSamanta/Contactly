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
import com.purnendu.contactly.alarm.AlarmMetadata
import com.purnendu.contactly.alarm.AlarmSyncManager
import com.purnendu.contactly.data.local.room.AppDatabase
import com.purnendu.contactly.utils.AlarmRequestCodeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

    init
    {
        checkCriticalPermissions()
        if(!_showContactPermissionDialog.value)
        loadContacts()
    }

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
        endAtMillis: Long,
        selectedDays: Int = 127
    )
    {
        scheduleAlarms(
                contact = contact,
                originalName = contact.name,
                temporaryName = temporaryName,
                startAtMillis = startAtMillis,
                endAtMillis = endAtMillis,
                selectedDays = selectedDays
        )
    }

    fun deleteSchedule(schedule: Schedule) {
        val id = schedule.id.toLongOrNull() ?: return
        val syncManager = AlarmSyncManager(application)
        viewModelScope.launch {
            syncManager.cancelScheduleAlarms(id)
            schedulesRepo.deleteById(id)
        }
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
        endAtMillis: Long,
        selectedDays: Int = 127
    ) {
        val syncManager = AlarmSyncManager(application)
        viewModelScope.launch {
            // First, cancel all existing alarms
            syncManager.cancelScheduleAlarms(scheduleId)
            
            // Then schedule new alarms
            scheduleAlarms(
                isUpdatingAlarm = true,
                contact = contact,
                originalName = contact.name,
                temporaryName = temporaryName,
                startAtMillis = startAtMillis,
                endAtMillis = endAtMillis,
                scheduleId = scheduleId,
                selectedDays = selectedDays
            )
        }
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
        endAtMillis: Long,
        selectedDays: Int,
        alarmMetadata: List<AlarmMetadata>
    )
    {
        val id = contact.id ?: return
        viewModelScope.launch(Dispatchers.IO)
        {
            val syncManager = AlarmSyncManager(application)
            val metadataJson = syncManager.toJson(alarmMetadata)
            
            schedulesRepo.create(
                contactId = id,
                contactLookupKey = contact.lookupKey,
                originalName = contact.name,
                temporaryName = temporaryName,
                startAtMillis = startAtMillis,
                endAtMillis = endAtMillis,
                selectedDays = selectedDays,
                scheduledAlarmsMetadata = metadataJson
            )
        }
    }

    fun updateAlarmToDatabase(
        scheduleId: Long,
        temporaryName: String,
        startAtMillis: Long,
        endAtMillis: Long,
        selectedDays: Int,
        alarmMetadata: List<AlarmMetadata>
    )
    {
        viewModelScope.launch(Dispatchers.IO) {
            val current = schedulesRepo.getById(scheduleId) ?: return@launch
            val syncManager = AlarmSyncManager(application)
            val metadataJson = syncManager.toJson(alarmMetadata)
            
            val updated = current.copy(
                temporaryName = temporaryName,
                startAtMillis = startAtMillis,
                endAtMillis = endAtMillis,
                selectedDays = selectedDays,
                scheduledAlarmsMetadata = metadataJson
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
    isUpdatingAlarm: Boolean = false,
    selectedDays: Int = 127
)
{
    var isAlarmSuccessfullyScheduled = true
    val contactId = contact.id.toString().toLongOrNull() ?: return

    val context = getApplication<Application>()
    val alarmManager = context.getSystemService(AlarmManager::class.java)

    // Extract selected days (0=Sun, 1=Mon, ...)
    val daysList = com.purnendu.contactly.utils.DayUtils.extractDaysFromBitmask(selectedDays)
    
    // If no days selected, don't schedule anything
    if (daysList.isEmpty()) return
    
    // Track alarm metadata for database storage
    val alarmMetadataList = mutableListOf<AlarmMetadata>()

    daysList.forEach { dayOfWeek ->
        // Calculate next occurrence for this day
        val applyAt = com.purnendu.contactly.utils.DayUtils.calculateNextOccurrence(startAtMillis, dayOfWeek)
        val revertAt = com.purnendu.contactly.utils.DayUtils.calculateNextOccurrence(endAtMillis, dayOfWeek)

        // Generate unique request codes using centralized utility
        val applyReqCode = AlarmRequestCodeUtils.generateApplyRequestCode(contactId, dayOfWeek)
        val revertReqCode = AlarmRequestCodeUtils.generateRevertRequestCode(contactId, dayOfWeek)
        
        // Store metadata for this alarm
        alarmMetadataList.add(
            AlarmMetadata(
                requestCode = applyReqCode,
                dayOfWeek = dayOfWeek,
                operation = AliasAlarmReceiver.OP_APPLY,
                triggerTimeMillis = applyAt
            )
        )
        alarmMetadataList.add(
            AlarmMetadata(
                requestCode = revertReqCode,
                dayOfWeek = dayOfWeek,
                operation = AliasAlarmReceiver.OP_REVERT,
                triggerTimeMillis = revertAt
            )
        )

        val applyIntent = Intent(context, AliasAlarmReceiver::class.java).apply {
            action = AliasAlarmReceiver.ACTION_ALIAS
            putExtra(AliasAlarmReceiver.EXTRA_OPERATION, AliasAlarmReceiver.OP_APPLY)
            putExtra(AliasAlarmReceiver.EXTRA_CONTACT_ID, contact.id)
            putExtra(AliasAlarmReceiver.EXTRA_NAME, temporaryName)
            putExtra(AliasAlarmReceiver.EXTRA_SCHEDULE_ID, scheduleId ?: -1L)
            putExtra(AliasAlarmReceiver.EXTRA_DAY_OF_WEEK, dayOfWeek)
        }
        val revertIntent = Intent(context, AliasAlarmReceiver::class.java).apply {
            action = AliasAlarmReceiver.ACTION_ALIAS
            putExtra(AliasAlarmReceiver.EXTRA_OPERATION, AliasAlarmReceiver.OP_REVERT)
            putExtra(AliasAlarmReceiver.EXTRA_CONTACT_ID, contact.id)
            putExtra(AliasAlarmReceiver.EXTRA_NAME, originalName)
            putExtra(AliasAlarmReceiver.EXTRA_SCHEDULE_ID, scheduleId ?: -1L)
            putExtra(AliasAlarmReceiver.EXTRA_DAY_OF_WEEK, dayOfWeek)
        }

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
        }
        catch (e: SecurityException) {
            Log.d("alarm_error", e.localizedMessage.toString())
            isAlarmSuccessfullyScheduled = false
        }
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
                endAtMillis = endAtMillis,
                selectedDays = selectedDays,
                alarmMetadata = alarmMetadataList
            )
        }
        else
        {
            addAlarmToDatabase(
                contact = contact,
                temporaryName = temporaryName,
                startAtMillis = startAtMillis,
                endAtMillis = endAtMillis,
                selectedDays = selectedDays,
                alarmMetadata = alarmMetadataList
            )
        }
    }
}
