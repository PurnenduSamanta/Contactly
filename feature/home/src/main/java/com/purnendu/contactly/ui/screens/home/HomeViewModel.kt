package com.purnendu.contactly.ui.screens.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.purnendu.contactly.common.ActivationMode
import com.purnendu.contactly.common.PermissionChecker
import com.purnendu.contactly.common.StatusEventBus
import com.purnendu.contactly.common.ViewMode
import com.purnendu.contactly.domain.model.Activation
import com.purnendu.contactly.domain.model.Contact
import com.purnendu.contactly.domain.model.LocationCoordinates
import com.purnendu.contactly.domain.model.TimeValidationResult
import com.purnendu.contactly.domain.usecase.ActivationCommand
import com.purnendu.contactly.domain.usecase.CheckBackgroundLocationPermissionUseCase
import com.purnendu.contactly.domain.usecase.CreateActivationUseCase
import com.purnendu.contactly.domain.usecase.DeleteActivationUseCase
import com.purnendu.contactly.domain.usecase.ExtractSharedLocationLabelUseCase
import com.purnendu.contactly.domain.usecase.FetchContactsUseCase
import com.purnendu.contactly.domain.usecase.GetActivationsUseCase
import com.purnendu.contactly.domain.usecase.ManageAppPreferencesUseCase
import com.purnendu.contactly.domain.usecase.ParseSharedLocationUseCase
import com.purnendu.contactly.domain.usecase.ToggleInstantActivationUseCase
import com.purnendu.contactly.domain.usecase.UpdateActivationUseCase
import com.purnendu.contactly.domain.usecase.ValidateDeviceTimeUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(
    private val permissionChecker: PermissionChecker,
    private val getActivationsUseCase: GetActivationsUseCase,
    private val fetchContactsUseCase: FetchContactsUseCase,
    private val createActivationUseCase: CreateActivationUseCase,
    private val updateActivationUseCase: UpdateActivationUseCase,
    private val deleteActivationUseCase: DeleteActivationUseCase,
    private val toggleInstantActivationUseCase: ToggleInstantActivationUseCase,
    private val validateDeviceTimeUseCase: ValidateDeviceTimeUseCase,
    private val checkBackgroundLocationPermissionUseCase: CheckBackgroundLocationPermissionUseCase,
    private val parseSharedLocationUseCase: ParseSharedLocationUseCase,
    private val extractSharedLocationLabelUseCase: ExtractSharedLocationLabelUseCase,
    manageAppPreferencesUseCase: ManageAppPreferencesUseCase
) : ViewModel() {

    private val _showContactPermissionDialog = MutableStateFlow(false)
    val showContactPermissionDialog: StateFlow<Boolean> = _showContactPermissionDialog

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    private val _refreshTrigger = MutableStateFlow(0L)

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val activations: StateFlow<List<Activation>> = _refreshTrigger
        .flatMapLatest { getActivationsUseCase() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val viewMode: StateFlow<ViewMode> = manageAppPreferencesUseCase.viewModeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ViewMode.LIST)

    private val _contacts = MutableStateFlow<List<Contact>>(emptyList())
    val contacts: StateFlow<List<Contact>> = _contacts

    private val _isContactsLoading = MutableStateFlow(false)
    val isContactsLoading: StateFlow<Boolean> = _isContactsLoading

    init {
        viewModelScope.launch {
            StatusEventBus.alarmFired.collect {
                _refreshTrigger.value = System.currentTimeMillis()
            }
        }

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
                _contacts.value = fetchContactsUseCase()
            } catch (_: SecurityException) {
                _contacts.value = emptyList()
            } finally {
                _isContactsLoading.value = false
            }
        }
    }

    fun contactForId(id: Long): Contact? {
        return try {
            fetchContactsUseCase.byId(id)
        } catch (_: SecurityException) {
            null
        }
    }

    fun checkCriticalPermissions() {
        _showContactPermissionDialog.value = !permissionChecker.hasContactsPermission()
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

    fun canHaveExactAlarmPermissions(): Boolean = permissionChecker.canActivateExactAlarms()

    fun hasBackgroundLocationPermission(): Boolean = checkBackgroundLocationPermissionUseCase()

    suspend fun validateDeviceTime(): TimeValidationResult = validateDeviceTimeUseCase()

    suspend fun parseSharedLocation(sharedText: String): LocationCoordinates? {
        return parseSharedLocationUseCase(sharedText)
    }

    fun extractSharedLocationLabel(sharedText: String): String? {
        return extractSharedLocationLabelUseCase(sharedText)
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
        latitude: Double? = null,
        longitude: Double? = null,
        radiusMeters: Float? = null,
        locationLabel: String? = null
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val command = ActivationCommand(
                contact = contact,
                activationId = activationId,
                temporaryName = temporaryName,
                tempImageUri = tempImage,
                startAtMillis = startAtMillis,
                endAtMillis = endAtMillis,
                selectedDays = selectedDays,
                activationMode = activationMode,
                latitude = latitude,
                longitude = longitude,
                radiusMeters = radiusMeters,
                locationLabel = locationLabel
            )

            val result = if (isEditing) {
                updateActivationUseCase(command)
            } else {
                createActivationUseCase(command)
            }

            if (!result.success) {
                _errorMessage.value = result.errorMessage ?: "Failed to save activation"
            }
        }
    }

    fun deleteActivation(activation: Activation) {
        viewModelScope.launch(Dispatchers.IO) {
            val success = deleteActivationUseCase(activation)
            if (!success) {
                Log.e("HomeViewModel", "Failed to delete activation: ${activation.id}")
            }
        }
    }

    fun toggleInstantActivation(activation: Activation) {
        val activationId = activation.id.toLongOrNull() ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val success = toggleInstantActivationUseCase(activationId)
            if (!success) {
                Log.e("HomeViewModel", "Failed to toggle instant activation: $activationId")
            }
        }
    }
}
