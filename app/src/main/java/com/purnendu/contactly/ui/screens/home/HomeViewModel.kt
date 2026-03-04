package com.purnendu.contactly.ui.screens.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.purnendu.contactly.receiver.AliasAlarmReceiver.Companion.OP_APPLY
import com.purnendu.contactly.receiver.AliasAlarmReceiver.Companion.OP_REVERT
import com.purnendu.contactly.data.repository.ContactsRepository
import com.purnendu.contactly.data.repository.ActivationsRepository
import com.purnendu.contactly.data.local.room.ActivationEntity
import com.purnendu.contactly.data.local.preferences.AppPreferences
import com.purnendu.contactly.model.Contact
import com.purnendu.contactly.model.Activation
import com.purnendu.contactly.manager.ContactlyAlarmManager
import com.purnendu.contactly.manager.ContactlyGeofenceManager
import com.purnendu.contactly.utils.PermissionChecker
import com.purnendu.contactly.utils.ActivationMode
import com.purnendu.contactly.utils.ViewMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel for Activations screen.
 * 
 * Manages activation CRUD operations, contact loading, and alarm scheduling.
 * 
 * All dependencies are injected via Koin using interfaces:
 * - PermissionChecker: Abstracts Android permission checks
 * - ContactlyAlarmManager: Handles all alarm-related operations
 * - ContactlyGeofenceManager: Handles geofence registration for NEARBY activations
 * - AppPreferences: Abstracts DataStore preferences
 */
import com.purnendu.contactly.utils.ImageStorageManager

// ...

class HomeViewModel(
    private val permissionChecker: PermissionChecker,
    private val activationsRepo: ActivationsRepository,
    private val contactsRepo: ContactsRepository,
    private val contactlyAlarmManager: ContactlyAlarmManager,
    private val imageStorageManager: ImageStorageManager,
    private val geofenceManager: ContactlyGeofenceManager,
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
    val activations: StateFlow<List<Activation>> = _refreshTrigger
        .flatMapLatest { activationsRepo.getActivations() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    // Listen to alarm events and trigger refresh
    init {
        viewModelScope.launch {
            StatusEventBus.alarmFired.collect { event ->
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
    suspend fun loadActivationEntity(id: Long): ActivationEntity? = activationsRepo.getById(id)

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

    fun canHaveExactAlarmPermissions(): Boolean {
        return permissionChecker.canActivateExactAlarms()
    }

    fun hasBackgroundLocationPermission(): Boolean {
        return geofenceManager.hasBackgroundLocationPermission()
    }

    fun addActivation(
        contact: Contact,
        activationId: Long,
        temporaryName: String,
        tempImage: String?,
        startAtMillis: Long? = null,
        endAtMillis: Long? = null,
        selectedDays: Int? = null,
        activationMode: ActivationMode,
        isEditing: Boolean,
        // Nearby fields
        latitude: Double? = null,
        longitude: Double? = null,
        radiusMeters: Float? = null,
        locationLabel: String? = null
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            if(isEditing)
            {
                // Cancel existing alarms (only for time-based activations)
                if (activationMode == ActivationMode.ONE_TIME || activationMode == ActivationMode.REPEAT) {
                    contactlyAlarmManager.cancelActivatedAlarms(activationId)
                }
                // Unregister old geofence if editing a NEARBY activation
                if (activationMode == ActivationMode.NEARBY) {
                    geofenceManager.unregisterGeofence(activationId)
                }
                
                // Delete old images to avoid unnecessary storage
                imageStorageManager.deleteImagesFromActivation(activationId)
            }

            // Save temporary image to internal storage (if URI provided)
            val tempImagePath: String? = tempImage?.let { uriString -> imageStorageManager.saveTemporaryImage(activationId, uriString) }

            // Save original contact's photo to internal storage (always, for restoration during REVERT)
            val originalImagePath: String? = contact.id?.let { contactId -> imageStorageManager.saveOriginalImage(activationId, contactId) }

            when (activationMode) {
                ActivationMode.INSTANT -> {
                    // INSTANT: No alarms needed, save to database with switch ON
                    if (isEditing) {
                        updateToDatabase(
                            activationId = activationId,
                            originalName = contact.name.orEmpty(),
                            temporaryName = temporaryName,
                            tempImage = tempImagePath,
                            originalImage = originalImagePath,
                            startAtMillis = null,
                            endAtMillis = null,
                            selectedDays = null,
                            activationMode = activationMode,
                            alarmMetadataJson = null,
                            instantSwitchStatus = true
                        )
                    } else {
                        addToDatabase(
                            activationId = activationId,
                            contact = contact,
                            temporaryName = temporaryName,
                            tempImage = tempImagePath,
                            originalImage = originalImagePath,
                            startAtMillis = null,
                            endAtMillis = null,
                            selectedDays = null,
                            activationMode = activationMode,
                            alarmMetadataJson = null,
                            instantSwitchStatus = true
                        )
                    }

                    // Auto-apply temporary name/photo to contact
                    contact.id?.let { contactId ->
                        contactsRepo.applyContact(
                            contactId = contactId,
                            name = temporaryName,
                            filePath = tempImagePath,
                            shouldRemovePhoto = tempImagePath == null
                        )
                    }
                }
                ActivationMode.NEARBY -> {
                    // NEARBY: Save to DB with location data, then register geofence
                    addNearbyActivation(
                        contact = contact,
                        activationId = activationId,
                        temporaryName = temporaryName,
                        tempImage = tempImagePath,
                        originalImage = originalImagePath,
                        latitude = latitude!!,
                        longitude = longitude!!,
                        radiusMeters = radiusMeters!!,
                        locationLabel = locationLabel,
                        isEditing = isEditing
                    )
                }
                else -> {
                    // ONE_TIME / REPEAT: Activate alarms then save to database
                    activateAlarms(
                        contact = contact,
                        activationId = activationId,
                        originalName = contact.name.orEmpty(),
                        temporaryName = temporaryName,
                        tempImage = tempImagePath,
                        originalImage = originalImagePath,
                        startAtMillis = startAtMillis!!,
                        endAtMillis = endAtMillis!!,
                        selectedDays = selectedDays!!,
                        activationMode = activationMode,
                        isUpdating = isEditing
                    )
                }
            }
        }
    }
    fun deleteActivation(activation: Activation) {
        val id = activation.id.toLongOrNull() ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Fetch activation entity before deletion to get original contact data
                val entity = activationsRepo.getById(id)

                // Cancel pending alarms or unregister geofence
                when (activation.activationMode) {
                    ActivationMode.INSTANT -> { /* No alarms to cancel */ }
                    ActivationMode.NEARBY -> geofenceManager.unregisterGeofence(id)
                    else -> contactlyAlarmManager.cancelActivatedAlarms(id)
                }

                if (entity != null) {
                    try {
                        contactsRepo.applyContact(
                            contactId = entity.contactId,
                            name = entity.originalName,
                            filePath = entity.originalImage,
                            shouldRemovePhoto = entity.originalImage == null
                        )
                        Log.d("ActivationsViewModel", "Restored contact ${entity.contactId} to original state")
                    } catch (e: Exception) {
                        Log.e("ActivationsViewModel", "Failed to restore contact ${entity.contactId}", e)
                    }
                }

                // Delete from database
                activationsRepo.deleteById(id)

                // Clean up stored images
                imageStorageManager.deleteImagesFromActivation(id)
            } catch (e: Exception) {
                Log.e("ActivationsViewModel", "Failed to delete activation: $id", e)
            }
        }
    }

    fun toggleInstantActivation(activation: Activation) {
        val activationId = activation.id.toLongOrNull() ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val entity = activationsRepo.getById(activationId) ?: return@launch
                val newStatus = entity.instantSwitchStatus != true

                if (newStatus) {
                    // Apply temporary name/photo
                    contactsRepo.applyContact(
                        contactId = entity.contactId,
                        name = entity.temporaryName,
                        filePath = entity.temporaryImage,
                        shouldRemovePhoto = entity.temporaryImage == null
                    )
                } else {
                    // Revert to original name/photo
                    contactsRepo.applyContact(
                        contactId = entity.contactId,
                        name = entity.originalName,
                        filePath = entity.originalImage,
                        shouldRemovePhoto = entity.originalImage == null
                    )
                }

                // Update DB column
                activationsRepo.update(entity.copy(instantSwitchStatus = newStatus))
            } catch (e: Exception) {
                Log.e("ActivationsViewModel", "Failed to toggle instant activation: $activationId", e)
            }
        }
    }

    private fun activateAlarms(
        contact: Contact,
        activationId: Long,
        originalName: String,
        temporaryName: String,
        tempImage: String?,
        originalImage: String?,
        startAtMillis: Long,
        endAtMillis: Long,
        selectedDays: Int,
        activationMode: ActivationMode,
        isUpdating: Boolean
    ) {
        val result = contactlyAlarmManager.activateAlarms(
            contact = contact,
            activationId = activationId,
            originalName = originalName,
            temporaryName = temporaryName,
            tempImage = tempImage,
            originalImage = originalImage,
            startAtMillis = startAtMillis,
            endAtMillis = endAtMillis,
            selectedDays = selectedDays,
            activationMode = activationMode
        )
        if (result.success) {
            val nearestStartAtMillis = result.alarmMetadata.filter{ it.operation == OP_APPLY }.minByOrNull {it.triggerTimeMillis }?.triggerTimeMillis ?: startAtMillis
            val nearestEndAtMillis   = result.alarmMetadata.filter{ it.operation == OP_REVERT }.minByOrNull {it.triggerTimeMillis }?.triggerTimeMillis ?: endAtMillis
            if (isUpdating) {
                updateToDatabase(
                    activationId = activationId,
                    originalName = originalName,
                    temporaryName = temporaryName,
                    tempImage = tempImage,
                    originalImage = originalImage,
                    startAtMillis = nearestStartAtMillis,
                    endAtMillis = nearestEndAtMillis,
                    selectedDays = selectedDays,
                    activationMode = activationMode,
                    alarmMetadataJson = contactlyAlarmManager.toJson(result.alarmMetadata)
                )
            } else {
                addToDatabase(
                    activationId = activationId,
                    contact = contact,
                    temporaryName = temporaryName,
                    tempImage = tempImage,
                    originalImage = originalImage,
                    startAtMillis = nearestStartAtMillis,
                    endAtMillis = nearestEndAtMillis,
                    selectedDays = selectedDays,
                    activationMode = activationMode,
                    alarmMetadataJson = contactlyAlarmManager.toJson(result.alarmMetadata)
                )
            }
        } else {
            Log.e("ActivationsViewModel", "Failed to activate alarms")
        }
    }

    private suspend fun addNearbyActivation(
        contact: Contact,
        activationId: Long,
        temporaryName: String,
        tempImage: String?,
        originalImage: String?,
        latitude: Double,
        longitude: Double,
        radiusMeters: Float,
        locationLabel: String?,
        isEditing: Boolean
    ) {
        // Register geofence first — only save to DB if it succeeds
        val success = geofenceManager.registerGeofence(activationId, latitude, longitude, radiusMeters)

        if (!success) {
            Log.e("ActivationsViewModel", "Geofence registration failed for activation: $activationId")
            _errorMessage.value = "Location permission is required for Nearby activations. Please grant location permission from Settings."
            return
        }

        if (isEditing) {
            updateToDatabase(
                activationId = activationId,
                originalName = contact.name.orEmpty(),
                temporaryName = temporaryName,
                tempImage = tempImage,
                originalImage = originalImage,
                startAtMillis = null,
                endAtMillis = null,
                selectedDays = null,
                activationMode = ActivationMode.NEARBY,
                alarmMetadataJson = null,
                latitude = latitude,
                longitude = longitude,
                radiusMeters = radiusMeters,
                locationLabel = locationLabel
            )
        } else {
            addToDatabase(
                activationId = activationId,
                contact = contact,
                temporaryName = temporaryName,
                tempImage = tempImage,
                originalImage = originalImage,
                startAtMillis = null,
                endAtMillis = null,
                selectedDays = null,
                activationMode = ActivationMode.NEARBY,
                alarmMetadataJson = null,
                latitude = latitude,
                longitude = longitude,
                radiusMeters = radiusMeters,
                locationLabel = locationLabel
            )
        }
    }

    private fun addToDatabase(
        activationId: Long,
        contact: Contact,
        temporaryName: String,
        tempImage: String?,
        originalImage: String?,
        startAtMillis: Long?,
        endAtMillis: Long?,
        selectedDays: Int?,
        activationMode: ActivationMode,
        alarmMetadataJson: String?,
        instantSwitchStatus: Boolean? = null,
        latitude: Double? = null,
        longitude: Double? = null,
        radiusMeters: Float? = null,
        locationLabel: String? = null
    ) {
        val id = contact.id ?: return
        viewModelScope.launch(Dispatchers.IO) {
            activationsRepo.create(
                activationId = activationId,
                contactId = id,
                contactLookupKey = contact.lookupKey,
                originalName = contact.name.orEmpty(),
                temporaryName = temporaryName,
                startAtMillis = startAtMillis,
                endAtMillis = endAtMillis,
                selectedDays = selectedDays,
                activatedAlarmsMetadata = alarmMetadataJson,
                activationMode = activationMode,
                tempImage = tempImage,
                originalImage = originalImage,
                instantSwitchStatus = instantSwitchStatus,
                latitude = latitude,
                longitude = longitude,
                radiusMeters = radiusMeters,
                locationLabel = locationLabel
            )
        }
    }

    private fun updateToDatabase(
        activationId: Long,
        originalName: String,
        temporaryName: String,
        tempImage: String?,
        originalImage: String?,
        startAtMillis: Long?,
        endAtMillis: Long?,
        selectedDays: Int?,
        activationMode: ActivationMode,
        alarmMetadataJson: String?,
        instantSwitchStatus: Boolean? = null,
        latitude: Double? = null,
        longitude: Double? = null,
        radiusMeters: Float? = null,
        locationLabel: String? = null
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val current = activationsRepo.getById(activationId) ?: return@launch
            val updated = current.copy(
                originalName = originalName,
                temporaryName = temporaryName,
                temporaryImage = tempImage,
                originalImage = originalImage,
                startAtMillis = startAtMillis,
                endAtMillis = endAtMillis,
                selectedDays = selectedDays,
                activationMode = ActivationMode.toInt(activationMode),
                activatedAlarmsMetadata = alarmMetadataJson,
                instantSwitchStatus = instantSwitchStatus,
                latitude = latitude,
                longitude = longitude,
                radiusMeters = radiusMeters,
                locationLabel = locationLabel,
            )
            activationsRepo.update(updated)
        }
    }
}
