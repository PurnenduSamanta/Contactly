package com.purnendu.contactly.ui.screens.schedule

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.provider.ContactsContract
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import androidx.navigation.NavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.purnendu.contactly.R
import com.purnendu.contactly.model.Contact
import com.purnendu.contactly.model.Schedule
import com.purnendu.contactly.ui.screens.schedule.components.ScheduleFilter
import com.purnendu.contactly.ui.screens.schedule.components.ScheduleFilterChips
import com.purnendu.contactly.ui.screens.schedule.components.ScheduleItem
import com.purnendu.contactly.ui.screens.schedule.components.contactSelectionBottomSheet.ContactSelectionBottomSheet
import com.purnendu.contactly.ui.screens.schedule.components.editingBottomSheet.EditScheduleSheet
import com.purnendu.contactly.ui.theme.ContactlyTheme
import com.purnendu.contactly.ui.components.ContactlyDialog
import com.purnendu.contactly.ui.components.ContactlyTimePicker
import com.purnendu.contactly.ui.components.ConfirmationDialogState
import com.purnendu.contactly.ui.components.getDialogProperties
import com.purnendu.contactly.utils.ActivationMode
import com.purnendu.contactly.utils.ViewMode
import com.purnendu.contactly.utils.DayUtils
import com.purnendu.contactly.utils.AppThemeMode
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.UUID
import kotlin.math.abs
import androidx.core.net.toUri
import com.purnendu.contactly.MainActivityViewModel
import com.purnendu.contactly.utils.validateDeviceTime

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun SchedulesScreen(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
    navController: NavController? = null,
    schedulesViewModel: SchedulesViewModel = koinViewModel(),
    mainActivityViewModel: MainActivityViewModel? = null
) {
    val context = LocalContext.current
    val schedules by schedulesViewModel.schedules.collectAsStateWithLifecycle()
    val contacts by schedulesViewModel.contacts.collectAsStateWithLifecycle()
    val isContactsLoading by schedulesViewModel.isContactsLoading.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()
    
    // View mode preference from ViewModel
    val viewMode by schedulesViewModel.viewMode.collectAsStateWithLifecycle()

    // Schedule type filter
    var selectedFilter by remember { mutableStateOf(ScheduleFilter.ALL) }

    // Notify MainActivityViewModel when schedules list changes
    LaunchedEffect(schedules) {
        mainActivityViewModel?.setHasSchedules(schedules.isNotEmpty())
    }

    var isSaving by remember { mutableStateOf(false) }
    var showContactSheet by remember { mutableStateOf(false) }
    var showEditSheet by remember { mutableStateOf(false) }
    var selectedContact by remember { mutableStateOf<Contact?>(null) }
    var temporaryName by remember { mutableStateOf("") }
    var temporaryImage by remember { mutableStateOf<String?>(null) }
    var startMillis by remember { mutableLongStateOf(0L) }
    var endMillis by remember { mutableLongStateOf(0L) }
    var activationMode by remember { mutableStateOf(ActivationMode.ONE_TIME) }
    var selectedDays by remember { mutableStateOf(setOf(Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1)) }
    
    // Custom time picker states
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }

    // Image picker launcher for temporary image
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        temporaryImage = uri?.toString()
    }
    
    // Generic confirmation dialog state - can handle various confirmation scenarios
    var confirmationDialogState by remember { mutableStateOf<ConfirmationDialogState?>(null) }
    
    // FAB scroll behavior
    val listState = rememberLazyListState()
    val gridState = rememberLazyGridState()
    val fabVisible by remember {
        derivedStateOf {
            when (viewMode) {
                ViewMode.LIST -> {
                    listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0
                }
                ViewMode.GRID -> {
                    gridState.firstVisibleItemIndex == 0 && gridState.firstVisibleItemScrollOffset == 0
                }
            }
        }
    }

    val showContactDialog = schedulesViewModel.showContactPermissionDialog.collectAsStateWithLifecycle()
    val errorMessage = schedulesViewModel.errorMessage.collectAsStateWithLifecycle()

    val permissionState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.WRITE_CONTACTS
        )
    )
    val lifeCycleOwner = LocalLifecycleOwner.current
    DisposableEffect(key1 = lifeCycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) {
                permissionState.launchMultiplePermissionRequest()
            }
            if (event == Lifecycle.Event.ON_RESUME) {
               schedulesViewModel.checkCriticalPermissions()
            }
        }
        lifeCycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifeCycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val combinedPermissionKey =  buildString {
        append(permissionState.permissions[0].status.toString())
        append(permissionState.permissions[1].status.toString())
    }

    val showPermissionDialog = rememberSaveable { mutableStateOf(false) }

    if(showPermissionDialog.value || showContactDialog.value)
    {
        context.apply {
            ContactlyDialog(
                isConfirmButtonAvailable = true,
                isDismissButtonAvailable = true,
                title =   getString(R.string.dialog_contacts_title),
                message = getString(R.string.dialog_contacts_message),
                onConfirm = {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = "package:${context.packageName}".toUri()
                    }
                    startActivity(intent)
                    showPermissionDialog.value =false
                    schedulesViewModel.dismissContactPermissionDialog()
                },
                onDismiss = {
                    showPermissionDialog.value =false
                    schedulesViewModel.dismissContactPermissionDialog()
                    val activity = context as? Activity
                    activity?.finish()
                },
                confirmButtonText = getString(R.string.action_settings),
                dismissButtonText = getString(R.string.action_exit)
            )
        }
    }

    // Listen for add schedule events from center FAB in bottom nav
    mainActivityViewModel?.let { viewModel ->
        LaunchedEffect(Unit) {
            viewModel.addScheduleEvent.collect {
                showContactSheet = true
            }
        }
    }

    LaunchedEffect(key1 = combinedPermissionKey)
    {
        //Checking permission status
        when {
            permissionState.allPermissionsGranted -> {
                showPermissionDialog.value = false
                if(contacts.isEmpty())
                schedulesViewModel.loadContacts()
            }

            permissionState.shouldShowRationale -> {
                showPermissionDialog.value = true
            }

            !permissionState.allPermissionsGranted && !permissionState.shouldShowRationale -> {
                showPermissionDialog.value = true
            }
        }
    }

    val snackBarHostState = remember { SnackbarHostState() }


    // Shared callbacks for Schedule Items

    val onEditClick: (Schedule) -> Unit = { sched ->
        val cid = sched.contactId
        if (cid != null) {
            val contact = schedulesViewModel.contactForId(cid)
            if (contact != null) {
                selectedContact = contact
                temporaryName = sched.name
                val sid = sched.id.toLongOrNull()
                if (sid != null) {
                    coroutineScope.launch {
                        val entity = schedulesViewModel.loadScheduleEntity(sid)
                        startMillis = entity?.startAtMillis ?: 0L
                        endMillis = entity?.endAtMillis ?: 0L
                        selectedDays = DayUtils.extractDaysFromBitmask(entity?.selectedDays ?: 127).toSet()
                        temporaryImage = entity?.temporaryImage
                        activationMode = ActivationMode.fromInt(entity?.activationMode ?: 0)
                        showEditSheet = true
                    }
                }
            }
        }
    }

    val onDeleteClick: (Schedule) -> Unit = { sched ->
        if(snackBarHostState.currentSnackbarData == null) {
            coroutineScope.launch {
                val result = snackBarHostState.showSnackbar(
                    message = "Do you want to delete this schedule?",
                    actionLabel = "Delete",
                    duration = SnackbarDuration.Short
                )
                if (result == SnackbarResult.ActionPerformed) {
                    schedulesViewModel.deleteSchedule(sched)
                }
            }
        }
    }

    val onContactDetailsClick: (Schedule) -> Unit = { sched ->
        val contactId = sched.contactId
        if (contactId != null) {
            try {
                val intent = Intent(
                    Intent.ACTION_VIEW,
                    ContactsContract.Contacts.getLookupUri(
                        contactId,
                        schedulesViewModel.contactForId(contactId)?.lookupKey ?: ""
                    )
                )
                context.startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(context, "Unable to open contact", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackBarHostState) },
        containerColor = Color.Transparent,
        floatingActionButton = {
         /*   AnimatedVisibility(
                visible = fabVisible && schedules.isNotEmpty(),
                enter = slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                ) + fadeIn(
                    animationSpec = tween(300)
                ),
                exit = slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = tween(300)
                ) + fadeOut(
                    animationSpec = tween(200)
                )
            ) {
                FloatingActionButton(
                    onClick = {
                        // TODO: Implement instant switch functionality
                    },
                    shape = CircleShape,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    elevation = FloatingActionButtonDefaults.elevation(
                        defaultElevation = 4.dp,
                        pressedElevation = 8.dp
                    ),
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_instant_switch),
                        contentDescription = stringResource(id = R.string.action_instant_switch),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }*/
        },
        modifier = modifier.fillMaxSize()
    ) { _ ->
        if (schedules.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {

                Column(
                    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                )
                {
                    Box(
                        modifier = Modifier
                            .background(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = CircleShape
                            )
                            .padding(30.dp), contentAlignment = Alignment.Center
                    )
                    {

                        Icon(
                            modifier = Modifier.size(40.dp),
                            painter = painterResource(R.drawable.calendar_month),
                            contentDescription = "calender",
                        )


                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        stringResource(id = R.string.empty_no_schedules),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Spacer(modifier = Modifier.height(5.dp))

                    Text(
                        stringResource(id = R.string.empty_get_started),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier
                            .background(
                                color = MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { showContactSheet = true }
                            .padding(horizontal = 15.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Add Schedule",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        Text(
                            stringResource(id = R.string.action_add_schedule),
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                // Filter chips row
                ScheduleFilterChips(
                    selectedFilter = selectedFilter,
                    onFilterChange = { selectedFilter = it }
                )

                // Schedule list with fade animation on filter change
                Crossfade(
                    targetState = selectedFilter,
                    animationSpec = tween(300),
                    label = "schedule-filter-animation",
                    modifier = Modifier.weight(1f)
                ) { filter ->
                    val displaySchedules = if (filter.toActivationMode() == null) schedules
                        else schedules.filter { it.activationMode == filter.toActivationMode() }

                    when (viewMode) {
                        ViewMode.LIST -> {
                            LazyColumn(
                                state = listState,
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(vertical = 8.dp)
                            ) {
                                itemsIndexed(displaySchedules) { index, schedule ->
                                    ScheduleItem(
                                        schedule = schedule,
                                        index = index,
                                        viewMode = ViewMode.LIST,
                                        onEditClick = onEditClick,
                                        onDeleteClick = onDeleteClick,
                                        onContactDetailsClick = onContactDetailsClick,
                                        onInstantToggle = { schedulesViewModel.toggleInstantSchedule(it) }
                                    )
                                }
                            }
                        }

                        ViewMode.GRID -> {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                state = gridState,
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(displaySchedules) { schedule ->
                                    ScheduleItem(
                                        schedule = schedule,
                                        viewMode = ViewMode.GRID,
                                        onEditClick = onEditClick,
                                        onDeleteClick = onDeleteClick,
                                        onContactDetailsClick = onContactDetailsClick,
                                        onInstantToggle = { schedulesViewModel.toggleInstantSchedule(it) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showContactSheet) {
        ContactSelectionBottomSheet(
            isLoading = isContactsLoading,
            onSyncClick = { schedulesViewModel.loadContacts() },
            error = errorMessage.value,
            onErrorCardDismiss = {schedulesViewModel.clearError()},
            contacts = contacts,
            onDismissContactSelection = {
                schedulesViewModel.clearError()
                showContactSheet = false},
            onContactClick = { contact ->
                val duplicateScheduledContactList = schedules.filter { it.contactId == contact.id }
                if(duplicateScheduledContactList.isNotEmpty())
                {
                    schedulesViewModel.showError(context.getString(R.string.duplicate_contact_alert))
                    return@ContactSelectionBottomSheet
                }
                selectedContact = contact
                showContactSheet = false
                temporaryName = ""
                temporaryImage = null  // Reset temp image for new schedule
                startMillis = 0L
                endMillis = 0L
                activationMode = ActivationMode.ONE_TIME
                selectedDays = setOf(Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1)
                showEditSheet = true
                schedulesViewModel.clearError()
            }
        )
    }

    if (showEditSheet) {
        val contact = schedulesViewModel.contactForId(selectedContact?.id ?: 0L) ?: return
        val formatter = SimpleDateFormat("hh:mm a", Locale.getDefault())
        EditScheduleSheet(
            error = errorMessage.value,
            onErrorCardDismiss = { schedulesViewModel.clearError() },
            contact = contact,
            temporaryName = temporaryName,
            temporaryImageUri = temporaryImage,
            startTime = if(startMillis==0L) "" else formatter.format(startMillis),
            endTime = if(endMillis==0L) "" else formatter.format(endMillis),
            selectedDays = selectedDays,
            activationMode = activationMode,
            isSaving = isSaving,
            onTemporaryNameChange = { temporaryName = it },
            onTemporaryImageClick = { imagePickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
            onTemporaryImageRemove = { temporaryImage = null },
            onDaysChanged = { selectedDays = it },
            onScheduleTypeChange = { newType ->
                activationMode = newType
                val today = Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1
                selectedDays = setOf(today)
            },
            onStartTimeClick = { showStartTimePicker = true },
            onEndTimeClick = { showEndTimePicker = true },
            onCancel = {
                showEditSheet = false
                showContactSheet = false
                selectedContact = null
                temporaryImage = null  // Clear temp image on cancel
                schedulesViewModel.clearError()
            },
            onSave = {
                coroutineScope.launch {
                    isSaving = true

                    // Check if contact has an actual name (not just phone/email)
                    if (contact.name == null) {
                        schedulesViewModel.showError("This contact doesn't have a name saved. Please add a name to the contact first.")
                        isSaving = false
                        return@launch
                    }

                    if(temporaryName.isEmpty() || temporaryName.isBlank())
                    {
                        schedulesViewModel.showError("Temporary name can not be empty")
                        isSaving=false
                        return@launch
                    }

                    // Time/day validations only for scheduled (non-INSTANT) types
                    // Extract time-of-day only (ignore date) for comparison
                    val startCal = Calendar.getInstance().apply { timeInMillis = startMillis }
                    val endCal = Calendar.getInstance().apply { timeInMillis = endMillis }
                    val startTimeOfDay = startCal.get(Calendar.HOUR_OF_DAY) * 60 + startCal.get(Calendar.MINUTE)
                    val endTimeOfDay = endCal.get(Calendar.HOUR_OF_DAY) * 60 + endCal.get(Calendar.MINUTE)

                    if (activationMode != ActivationMode.INSTANT) {
                        if(startMillis==0L)
                        {
                            schedulesViewModel.showError("Start time can not be empty")
                            isSaving=false
                            return@launch
                        }

                        if(endMillis==0L)
                        {
                            schedulesViewModel.showError("End time can not be empty")
                            isSaving=false
                            return@launch
                        }

                        if(startTimeOfDay == endTimeOfDay)
                        {
                            schedulesViewModel.showError("End time and start time can not be same")
                            isSaving=false
                            return@launch
                        }

                        // End time must be greater than start time (no crossing midnight allowed)
                        // Schedules must be within a single 24-hour day
                        if(endTimeOfDay < startTimeOfDay)
                        {
                            schedulesViewModel.showError("End time must be after start time")
                            isSaving=false
                            return@launch
                        }

                        if(selectedDays.isEmpty())
                        {
                            schedulesViewModel.showError("Select at least one day")
                            isSaving=false
                            return@launch
                        }

                        val exactOk = schedulesViewModel.canScheduleExactAlarmPermissions()
                        if (!exactOk)
                        {
                            Toast.makeText(context, context.getString(R.string.toast_enable_exact_alarm), Toast.LENGTH_LONG).show()
                            val i = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                            context.startActivity(i)
                            isSaving=false
                            return@launch
                        }

                        if(!validateDeviceTime(context).isValid)
                        {
                            schedulesViewModel.showError(validateDeviceTime(context).errorMessage.toString())
                            isSaving=false
                            return@launch
                        }
                    }

                    // Lambda to perform the actual save operation
                    val performSave: (Long?, Long?) -> Unit = { saveStartMillis, saveEndMillis ->
                        val selectedDaysBitmask = if (activationMode != ActivationMode.INSTANT) DayUtils.daysToBitmask(selectedDays) else null
                        val existingScheduleId = schedules.firstOrNull { it.contactId == contact.id }?.id?.toLongOrNull()

                        // Generate scheduleId for new schedules (needed for image file naming)
                        val scheduleIdToUse = existingScheduleId ?: abs(UUID.randomUUID().mostSignificantBits)

                        schedulesViewModel.addSchedule(
                            contact = contact,
                            scheduleId = existingScheduleId ?: scheduleIdToUse,
                            temporaryName = temporaryName,
                            tempImage = temporaryImage,
                            startAtMillis = saveStartMillis,
                            endAtMillis = saveEndMillis,
                            selectedDays = selectedDaysBitmask,
                            activationMode = activationMode,
                            isEditing = existingScheduleId != null
                        )
                        if (existingScheduleId != null) {
                            val msg = if (activationMode == ActivationMode.INSTANT) R.string.toast_instant_updated else R.string.ScheduleUpdated
                            Toast.makeText(context, context.getString(msg), Toast.LENGTH_SHORT).show()
                        } else {
                            val msg = if (activationMode == ActivationMode.INSTANT) R.string.toast_instant_saved else R.string.toast_schedule_saved
                            Toast.makeText(context, context.getString(msg), Toast.LENGTH_SHORT).show()
                        }
                        isSaving=false
                        showEditSheet = false
                        showContactSheet = false
                        temporaryImage = null  // Clear temp image after save
                    }

                    // INSTANT: Save immediately (no time-based logic needed)
                    if (activationMode == ActivationMode.INSTANT) {
                        performSave(null, null)
                        return@launch
                    }

                    // Check if start time is in the past (for today's selected day)
                    val now = Calendar.getInstance()
                    val currentTimeOfDay = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
                    val todayDayIndex = now.get(Calendar.DAY_OF_WEEK) - 1 // 0 = Sunday, 1 = Monday, etc.

                    // Check if today is selected and start time is in the past
                    val isTodaySelected = selectedDays.contains(todayDayIndex)
                    val isStartTimeInPast = startTimeOfDay < currentTimeOfDay

                    if (isTodaySelected && isStartTimeInPast) {
                        // Show confirmation dialog for past time
                        // Note: calculateNextOccurrencePair automatically handles past times
                        // by scheduling to next week, so we just need to inform the user
                        confirmationDialogState = ConfirmationDialogState.PastTimeSchedule(
                            onConfirm = {
                                // Both ONE_TIME and REPEAT use same logic now
                                // calculateNextOccurrencePair handles past time automatically
                                performSave(startMillis, endMillis)
                            }
                        )
                    } else {
                        // Proceed directly with save using original times
                        performSave(startMillis, endMillis)
                    }


                }


            }
        )
    }
    
    // Custom Time Pickers
    if (showStartTimePicker) {
        ContactlyTimePicker(
            onDismiss = { showStartTimePicker = false },
            onTimeSelected = { hour, minute ->
                val cal = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, hour)
                    set(Calendar.MINUTE, minute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                startMillis = cal.timeInMillis
                showStartTimePicker = false
            }
        )
    }
    
    if (showEndTimePicker) {
        ContactlyTimePicker(
            onDismiss = { showEndTimePicker = false },
            onTimeSelected = { hour, minute ->
                val cal = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, hour)
                    set(Calendar.MINUTE, minute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                endMillis = cal.timeInMillis
                showEndTimePicker = false
            }
        )
    }
    
    // Generic Confirmation Dialog - handles various confirmation scenarios
    confirmationDialogState?.let { state ->
        val dialogProperties = state.getDialogProperties()
        ContactlyDialog(
            title = dialogProperties.title,
            message = dialogProperties.message,
            onConfirm = {
                when (state) {
                    is ConfirmationDialogState.PastTimeSchedule -> {
                        state.onConfirm()
                    }
                    // Add more cases here as you add new ConfirmationDialogState types
                }
                confirmationDialogState = null
            },
            onDismiss = {
                confirmationDialogState = null
            },
            isConfirmButtonAvailable = true,
            isDismissButtonAvailable = true,
            confirmButtonText = dialogProperties.confirmButtonText,
            dismissButtonText = dialogProperties.dismissButtonText
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SchedulesScreenPreview() {
    ContactlyTheme(appThemeMode = AppThemeMode.LIGHT) {
        SchedulesScreen(
            schedulesViewModel = koinViewModel()
        )
    }
}


