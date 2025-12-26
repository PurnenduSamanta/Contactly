package com.purnendu.contactly.ui.screens.schedule

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.provider.ContactsContract
import android.widget.Toast
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
import androidx.compose.material3.ExtendedFloatingActionButton
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
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import com.purnendu.contactly.ui.screens.schedule.components.ScheduleItem
import com.purnendu.contactly.ui.screens.schedule.components.contactSelectionBottomSheet.ContactSelectionBottomSheet
import com.purnendu.contactly.ui.screens.schedule.components.editingBottomSheet.EditScheduleSheet
import com.purnendu.contactly.ui.theme.ContactlyTheme
import com.purnendu.contactly.ui.components.ContactlyDialog
import com.purnendu.contactly.ui.components.ContactlyTimePicker
import com.purnendu.contactly.utils.ScheduleType
import com.purnendu.contactly.utils.ViewMode
import com.purnendu.contactly.utils.DayUtils
import com.purnendu.contactly.utils.AppThemeMode
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun SchedulesScreen(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
    navController: NavController? = null,
    schedulesViewModel: SchedulesViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val schedules by schedulesViewModel.schedules.collectAsStateWithLifecycle()
    val contacts by schedulesViewModel.contacts.collectAsStateWithLifecycle()
    val isContactsLoading by schedulesViewModel.isContactsLoading.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()
    
    // View mode preference from ViewModel
    val viewMode by schedulesViewModel.viewMode.collectAsStateWithLifecycle()

    var showContactSheet by remember { mutableStateOf(false) }
    var showEditSheet by remember { mutableStateOf(false) }
    var selectedContact by remember { mutableStateOf<Contact?>(null) }
    var temporaryName by remember { mutableStateOf("") }
    var startTimeText by remember { mutableStateOf("") }
    var endTimeText by remember { mutableStateOf("") }
    var startMillis by remember { mutableLongStateOf(0L) }
    var endMillis by remember { mutableLongStateOf(0L) }
    var scheduleType by remember { mutableStateOf(ScheduleType.ONE_TIME) }
    var selectedDays by remember { mutableStateOf(setOf(Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1)) }
    
    // Custom time picker states
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }
    
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
                        data = Uri.parse("package:${context.packageName}")
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
                        startTimeText = if (startMillis > 0) SimpleDateFormat("HH:mm").format(Date(startMillis)) else ""
                        endTimeText = if (endMillis > 0) SimpleDateFormat("HH:mm").format(Date(endMillis)) else ""
                        scheduleType = if(entity?.scheduleType == 0) ScheduleType.ONE_TIME else ScheduleType.REPEAT
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
            AnimatedVisibility(
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
                ExtendedFloatingActionButton(
                    onClick = {
                        showContactSheet = true
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
                {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = stringResource(id = R.string.action_add_schedule)
                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        Text(
                            "Add Schedule",
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold
                        )

                    }
                }
            }
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
                    modifier = Modifier.fillMaxWidth(),
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
            // Conditional rendering based on view mode
            when (viewMode) {
                ViewMode.LIST -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        items(schedules) { schedule ->
                            ScheduleItem(
                                schedule = schedule,
                                viewMode = ViewMode.LIST,
                                avatarUri = schedule.contactId?.let { schedulesViewModel.contactForId(it)?.image as String? },
                                onEditClick = onEditClick,
                                onDeleteClick = onDeleteClick,
                                onContactDetailsClick = onContactDetailsClick
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
                        items(schedules) { schedule ->
                            ScheduleItem(
                                schedule = schedule,
                                viewMode = ViewMode.GRID,
                                avatarUri = schedule.contactId?.let { schedulesViewModel.contactForId(it)?.image as String? },
                                onEditClick = onEditClick,
                                onDeleteClick = onDeleteClick,
                                onContactDetailsClick = onContactDetailsClick
                            )
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
                startTimeText = ""
                endTimeText = ""
                startMillis = 0L
                endMillis = 0L
                showEditSheet = true
                schedulesViewModel.clearError()
            }
        )
    }

    if (showEditSheet) {

        val contact = selectedContact ?: Contact("", "", null)
        val formatter = SimpleDateFormat("hh:mm a", Locale.getDefault())
        EditScheduleSheet(
            error = errorMessage.value,
            onErrorCardDismiss = { schedulesViewModel.clearError() },
            contact = contact,
            temporaryName = temporaryName,
            startTime = if(startMillis==0L) "" else formatter.format(startMillis),
            endTime = if(endMillis==0L) "" else formatter.format(endMillis),
            selectedDays = selectedDays,
            scheduleType = scheduleType,
            onTemporaryNameChange = { temporaryName = it },
            onDaysChanged = { selectedDays = it },
            onScheduleTypeChange = { newType ->
                scheduleType = newType
                val today = Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1
                selectedDays = setOf(today)
            },
            onStartTimeClick = { showStartTimePicker = true },
            onEndTimeClick = { showEndTimePicker = true },
            onCancel = {
                showEditSheet = false
                showContactSheet = false
                selectedContact=null
                schedulesViewModel.clearError()
                       },
            onSave = {
                val contact = selectedContact ?: return@EditScheduleSheet

                if(temporaryName.isEmpty() || temporaryName.isBlank())
                {
                    schedulesViewModel.showError("Temporary name can not be empty")

                    return@EditScheduleSheet
                }

                if(startTimeText.isEmpty())
                {
                    schedulesViewModel.showError("Start time can not be empty")

                    return@EditScheduleSheet
                }

                if(endTimeText.isEmpty())
                {
                    schedulesViewModel.showError("End time can not be empty")

                    return@EditScheduleSheet
                }

                // Extract time-of-day only (ignore date) for comparison
                val startCal = Calendar.getInstance().apply { timeInMillis = startMillis }
                val endCal = Calendar.getInstance().apply { timeInMillis = endMillis }
                val startTimeOfDay = startCal.get(Calendar.HOUR_OF_DAY) * 60 + startCal.get(Calendar.MINUTE)
                val endTimeOfDay = endCal.get(Calendar.HOUR_OF_DAY) * 60 + endCal.get(Calendar.MINUTE)

                if(startTimeOfDay == endTimeOfDay)
                {
                    schedulesViewModel.showError("End time and start time can not be same")

                    return@EditScheduleSheet
                }

                // End time must be greater than start time (no crossing midnight allowed)
                // Schedules must be within a single 24-hour day
                if(endTimeOfDay < startTimeOfDay)
                {
                    schedulesViewModel.showError("End time must be after start time. For overnight schedules, please create separate schedules for each day.")

                    return@EditScheduleSheet
                }

                if(selectedDays.isEmpty())
                {
                    schedulesViewModel.showError("Select at least one day")

                    return@EditScheduleSheet
                }

                val exactOk = schedulesViewModel.canScheduleExactAlarmPermissions()
                if (!exactOk)
                {
                    Toast.makeText(context, context.getString(R.string.toast_enable_exact_alarm), Toast.LENGTH_LONG).show()
                    val i = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                    context.startActivity(i)
                    return@EditScheduleSheet
                }
                val selectedDaysBitmask = DayUtils.daysToBitmask(selectedDays)
                    val editingId = schedules.firstOrNull { it.name == temporaryName && it.contactId == contact.id }?.id?.toLongOrNull()
                    if (editingId != null) {
                        schedulesViewModel.updateSchedule(
                            editingId,
                            contact,
                            temporaryName,
                            startMillis,
                            endMillis,
                            selectedDaysBitmask,
                            scheduleType
                        )
                        Toast.makeText(context,context.getString(R.string.ScheduleUpdated),Toast.LENGTH_SHORT).show()
                    } else {
                        schedulesViewModel.addSchedule(contact,
                            abs(UUID.randomUUID().mostSignificantBits), temporaryName, startMillis, endMillis, selectedDaysBitmask,scheduleType)
                        Toast.makeText(context,context.getString(R.string.toast_schedule_saved),Toast.LENGTH_SHORT).show()
                    }
                    showEditSheet = false
                    showContactSheet=false
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
                val formatter = SimpleDateFormat("hh:mm a", Locale.getDefault())
                startTimeText = formatter.format(cal.time)
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
                val formatter = SimpleDateFormat("hh:mm a", Locale.getDefault())
                endTimeText = formatter.format(cal.time)
                showEndTimePicker = false
            }
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


