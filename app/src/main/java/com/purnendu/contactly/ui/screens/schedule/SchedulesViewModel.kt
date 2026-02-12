package com.purnendu.contactly.ui.screens.schedule

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.purnendu.contactly.alarm.AliasAlarmReceiver.Companion.OP_APPLY
import com.purnendu.contactly.alarm.AliasAlarmReceiver.Companion.OP_REVERT
import com.purnendu.contactly.alarm.AlarmEventBus
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
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
import com.purnendu.contactly.utils.ImageStorageManager

// ...

class SchedulesViewModel(
    private val permissionChecker: PermissionChecker,
    private val schedulesRepo: SchedulesRepository,
    private val contactsRepo: ContactsRepository,
    private val contactlyAlarmManager: ContactlyAlarmManager,
    private val imageStorageManager: ImageStorageManager,
    appPreferences: AppPreferences
) : ViewModel() {

    private val _showContactPermissionDialog = MutableStateFlow(false)
    val showContactPermissionDialog: StateFlow<Boolean> = _showContactPermissionDialog

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    // Refresh trigger - emits when an alarm fires to re-check active status
    private val _refreshTrigger = MutableStateFlow(0L)
    
    // Combine database flow with refresh trigger to update active status when alarm fires
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val schedules: StateFlow<List<Schedule>> = _refreshTrigger
        .flatMapLatest { schedulesRepo.getSchedules() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    // Listen to alarm events and trigger refresh
    init {
        viewModelScope.launch {
            AlarmEventBus.alarmFired.collect { event ->
                // Trigger refresh when any alarm fires
                _refreshTrigger.value = System.currentTimeMillis()
            }
        }
    }

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
    suspend fun loadScheduleEntity(id: Long): ScheduleEntity? = schedulesRepo.getById(id)

    fun contactForId(id: Long): Contact? = try {
        contactsRepo.fetchContactById(id)
    } catch (e: SecurityException) {
        null // Return null if contacts permission is not granted
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

    fun addSchedule(
        contact: Contact,
        scheduleId: Long,
        temporaryName: String,
        tempImage: String? ,
        startAtMillis: Long,
        endAtMillis: Long,
        selectedDays: Int,
        scheduleType: ScheduleType,
        isEditing: Boolean
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            if(isEditing)
            {
                // First, cancel all existing alarms
                contactlyAlarmManager.cancelScheduleAlarms(scheduleId)
                
                // Delete old images to avoid unnecessary storage
                imageStorageManager.deleteImagesForSchedule(scheduleId)
            }

            // Save temporary image to internal storage (if URI provided)
            val tempImagePath: String? = tempImage?.let { uriString -> imageStorageManager.saveTemporaryImage(scheduleId, uriString) }

            // Save original contact's photo to internal storage
            val originalImagePath: String? = contact.id?.let { contactId -> if(tempImage!=null) imageStorageManager.saveOriginalImage( scheduleId, contactId) else null}


            // Then schedule new alarms
            scheduleAlarms(
                contact = contact,
                scheduleId = scheduleId,
                originalName = contact.name.orEmpty(),
                temporaryName = temporaryName,
                tempImage = tempImagePath,
                originalImage = originalImagePath,
                startAtMillis = startAtMillis,
                endAtMillis = endAtMillis,
                selectedDays = selectedDays,
                scheduleType = scheduleType,
                isUpdating = isEditing
            )
        }
    }
    fun deleteSchedule(schedule: Schedule) {
        val id = schedule.id.toLongOrNull() ?: return
        viewModelScope.launch {
            contactlyAlarmManager.cancelScheduleAlarms(id)
            schedulesRepo.deleteById(id)
            // Clean up stored images
            withContext(Dispatchers.IO) {
                imageStorageManager.deleteImagesForSchedule( id)
            }
        }
    }

    private fun scheduleAlarms(
        contact: Contact,
        scheduleId: Long,
        originalName: String,
        temporaryName: String,
        tempImage: String?,
        originalImage: String?,
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
            tempImage = tempImage,
            originalImage = originalImage,
            startAtMillis = startAtMillis,
            endAtMillis = endAtMillis,
            selectedDays = selectedDays,
            scheduleType = scheduleType
        )
        if (result.success) {
            val nearestStartAtMillis = result.alarmMetadata.filter{ it.operation == OP_APPLY }.minByOrNull {it.triggerTimeMillis }?.triggerTimeMillis ?: startAtMillis
            val nearestEndAtMillis   = result.alarmMetadata.filter{ it.operation == OP_REVERT }.minByOrNull {it.triggerTimeMillis }?.triggerTimeMillis ?: endAtMillis
            if (isUpdating) {
                updateAlarmToDatabase(
                    scheduleId = scheduleId,
                    originalName = originalName,
                    temporaryName = temporaryName,
                    tempImage = tempImage,
                    originalImage = originalImage,
                    startAtMillis = nearestStartAtMillis,
                    endAtMillis = nearestEndAtMillis,
                    selectedDays = selectedDays,
                    scheduleType = scheduleType,
                    alarmMetadataJson = contactlyAlarmManager.toJson(result.alarmMetadata)
                )
            } else {
                addAlarmToDatabase(
                    scheduleId = scheduleId,
                    contact = contact,
                    temporaryName = temporaryName,
                    tempImage = tempImage,
                    originalImage = originalImage,
                    startAtMillis = nearestStartAtMillis,
                    endAtMillis = nearestEndAtMillis,
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
        tempImage: String?,
        originalImage: String?,
        startAtMillis: Long,
        endAtMillis: Long,
        selectedDays: Int,
        scheduleType: ScheduleType,
        alarmMetadataJson: String
    ) {
        val id = contact.id ?: return
        // Get original image URI from contact for restoration during REVERT
        viewModelScope.launch(Dispatchers.IO) {
            schedulesRepo.create(
                scheduleId = scheduleId,
                contactId = id,
                contactLookupKey = contact.lookupKey,
                originalName = contact.name.orEmpty(),
                temporaryName = temporaryName,
                startAtMillis = startAtMillis,
                endAtMillis = endAtMillis,
                selectedDays = selectedDays,
                scheduledAlarmsMetadata = alarmMetadataJson,
                scheduleType = scheduleType,
                tempImage = tempImage,
                originalImage = originalImage
            )
        }
    }

    private fun updateAlarmToDatabase(
        scheduleId: Long,
        originalName: String,
        temporaryName: String,
        tempImage: String?,
        originalImage: String?,
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
                temporaryImage = tempImage,
                originalImage = originalImage,
                startAtMillis = startAtMillis,
                endAtMillis = endAtMillis,
                selectedDays = selectedDays,
                scheduleType = if (scheduleType == ScheduleType.ONE_TIME) 0 else 1,
                scheduledAlarmsMetadata = alarmMetadataJson,
            )
            schedulesRepo.update(updated)
        }
    }
}
