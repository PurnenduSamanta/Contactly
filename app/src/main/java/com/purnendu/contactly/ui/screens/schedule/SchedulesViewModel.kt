package com.purnendu.contactly.ui.screens.schedule

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.purnendu.contactly.data.repository.ContactsRepository
import com.purnendu.contactly.data.repository.SchedulesRepository
import com.purnendu.contactly.data.local.room.ScheduleEntity
import com.purnendu.contactly.data.local.preferences.AppPreferences
import com.purnendu.contactly.model.Contact
import com.purnendu.contactly.model.Schedule
import com.purnendu.contactly.alarm.ContactlyAlarmManager
import com.purnendu.contactly.utils.PermissionChecker
import com.purnendu.contactly.utils.ScheduleType
import com.purnendu.contactly.utils.ViewMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel for Schedules screen.
 * 
 * Manages schedule CRUD operations, contact loading, and alarm scheduling.
 * 
 * All dependencies are injected via Koin using interfaces:
 * - PermissionChecker: Abstracts Android permission checks
 * - ContactlyAlarmManager: Handles all alarm-related operations
 * - AppPreferences: Abstracts DataStore preferences
 * 
 * This design makes the ViewModel fully testable without needing Android mocks.
 */
class SchedulesViewModel(
    private val permissionChecker: PermissionChecker,
    private val schedulesRepo: SchedulesRepository,
    private val contactsRepo: ContactsRepository,
    private val contactlyAlarmManager: ContactlyAlarmManager,
    appPreferences: AppPreferences
) : ViewModel() {

    private val _showContactPermissionDialog = MutableStateFlow(false)
    val showContactPermissionDialog: StateFlow<Boolean> = _showContactPermissionDialog

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    val schedules: StateFlow<List<Schedule>> = schedulesRepo.getSchedules().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // View mode preference from DataStore
    val viewMode: StateFlow<ViewMode> = appPreferences.viewModeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ViewMode.LIST)

    private val _contacts = MutableStateFlow<List<Contact>>(emptyList())
    val contacts: StateFlow<List<Contact>> = _contacts
    private val _isContactsLoading = MutableStateFlow(false)
    val isContactsLoading: StateFlow<Boolean> = _isContactsLoading

    init {
        checkCriticalPermissions()
        if (!_showContactPermissionDialog.value && _contacts.value.isEmpty()) {
            loadContacts()
        }
    }

    fun loadContacts() {
        checkCriticalPermissions()
        if (_showContactPermissionDialog.value) return
        _isContactsLoading.value = true

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val fetchedContacts = contactsRepo.fetchContacts()
                _contacts.value = fetchedContacts
            } catch (e: SecurityException) {
                // Handle the case where contacts permissions are not granted
                _contacts.value = emptyList()
            } finally {
                _isContactsLoading.value = false
            }
        }
    }

    fun addSchedule(
        contact: Contact,
        scheduleId: Long,
        temporaryName: String,
        startAtMillis: Long,
        endAtMillis: Long,
        selectedDays: Int,
        scheduleType: ScheduleType
    ) {
        scheduleAlarms(
            contact = contact,
            scheduleId = scheduleId,
            originalName = contact.name,
            temporaryName = temporaryName,
            startAtMillis = startAtMillis,
            endAtMillis = endAtMillis,
            selectedDays = selectedDays,
            scheduleType = scheduleType,
            isUpdating = false
        )
    }

    fun deleteSchedule(schedule: Schedule) {
        val id = schedule.id.toLongOrNull() ?: return
        viewModelScope.launch {
            contactlyAlarmManager.cancelScheduleAlarms(id)
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
        selectedDays: Int,
        scheduleType: ScheduleType
    ) {
        viewModelScope.launch {
            // First, cancel all existing alarms
            contactlyAlarmManager.cancelScheduleAlarms(scheduleId)
            
            // Then schedule new alarms
            scheduleAlarms(
                scheduleId = scheduleId,
                contact = contact,
                originalName = contact.name,
                temporaryName = temporaryName,
                startAtMillis = startAtMillis,
                endAtMillis = endAtMillis,
                selectedDays = selectedDays,
                scheduleType = scheduleType,
                isUpdating = true
            )
        }
    }

    fun checkCriticalPermissions() {
        val hasContactPermissions = permissionChecker.hasContactsPermission()
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
        return permissionChecker.canScheduleExactAlarms()
    }

    private fun scheduleAlarms(
        contact: Contact,
        scheduleId: Long,
        originalName: String,
        temporaryName: String,
        startAtMillis: Long,
        endAtMillis: Long,
        selectedDays: Int,
        scheduleType: ScheduleType,
        isUpdating: Boolean
    ) {
        val result = contactlyAlarmManager.scheduleAlarms(
            contact = contact,
            scheduleId = scheduleId,
            originalName = originalName,
            temporaryName = temporaryName,
            startAtMillis = startAtMillis,
            endAtMillis = endAtMillis,
            selectedDays = selectedDays,
            scheduleType = scheduleType
        )

        if (result.success) {
            if (isUpdating) {
                updateAlarmToDatabase(
                    scheduleId = scheduleId,
                    originalName = originalName,
                    temporaryName = temporaryName,
                    startAtMillis = startAtMillis,
                    endAtMillis = endAtMillis,
                    selectedDays = selectedDays,
                    scheduleType = scheduleType,
                    alarmMetadataJson = contactlyAlarmManager.toJson(result.alarmMetadata)
                )
            } else {
                addAlarmToDatabase(
                    scheduleId = scheduleId,
                    contact = contact,
                    temporaryName = temporaryName,
                    startAtMillis = startAtMillis,
                    endAtMillis = endAtMillis,
                    selectedDays = selectedDays,
                    scheduleType = scheduleType,
                    alarmMetadataJson = contactlyAlarmManager.toJson(result.alarmMetadata)
                )
            }
        } else {
            Log.e("SchedulesViewModel", "Failed to schedule alarms")
        }
    }

    private fun addAlarmToDatabase(
        scheduleId: Long,
        contact: Contact,
        temporaryName: String,
        startAtMillis: Long,
        endAtMillis: Long,
        selectedDays: Int,
        scheduleType: ScheduleType,
        alarmMetadataJson: String
    ) {
        val id = contact.id ?: return
        viewModelScope.launch(Dispatchers.IO) {
            schedulesRepo.create(
                scheduleId = scheduleId,
                contactId = id,
                contactLookupKey = contact.lookupKey,
                originalName = contact.name,
                temporaryName = temporaryName,
                startAtMillis = startAtMillis,
                endAtMillis = endAtMillis,
                selectedDays = selectedDays,
                scheduledAlarmsMetadata = alarmMetadataJson,
                scheduleType = scheduleType
            )
        }
    }

    private fun updateAlarmToDatabase(
        scheduleId: Long,
        originalName: String,
        temporaryName: String,
        startAtMillis: Long,
        endAtMillis: Long,
        selectedDays: Int,
        scheduleType: ScheduleType,
        alarmMetadataJson: String
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val current = schedulesRepo.getById(scheduleId) ?: return@launch
            
            val updated = current.copy(
                originalName = originalName,
                temporaryName = temporaryName,
                startAtMillis = startAtMillis,
                endAtMillis = endAtMillis,
                selectedDays = selectedDays,
                scheduleType = if (scheduleType == ScheduleType.ONE_TIME) 0 else 1,
                scheduledAlarmsMetadata = alarmMetadataJson
            )
            schedulesRepo.update(updated)
        }
    }
}
