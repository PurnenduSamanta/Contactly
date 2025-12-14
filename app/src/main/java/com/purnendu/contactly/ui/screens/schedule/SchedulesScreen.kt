package com.purnendu.contactly.ui.screens.schedule

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.ui.res.stringResource
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.purnendu.contactly.R
import com.purnendu.contactly.model.Contact
import com.purnendu.contactly.ui.screens.schedule.components.ScheduleItem
import com.purnendu.contactly.ui.screens.schedule.components.contactSelectionBottomSheet.ContactSelectionBottomSheet
import com.purnendu.contactly.ui.screens.schedule.components.editingBottomSheet.EditScheduleSheet
import com.purnendu.contactly.ui.theme.ContactlyTheme
import com.purnendu.contactly.components.ContactlyDialog
import com.purnendu.contactly.components.ContactlyTimePicker
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun SchedulesScreen(
    modifier: Modifier = Modifier,
    navController: NavController?=null,
    schedulesViewModel: SchedulesViewModel = viewModel()
) {
    val context = LocalContext.current
    val schedules by schedulesViewModel.schedules.collectAsState()
    val contacts by schedulesViewModel.contacts.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    var showContactSheet by remember { mutableStateOf(false) }
    var showEditSheet by remember { mutableStateOf(false) }
    var selectedContact by remember { mutableStateOf<Contact?>(null) }
    var temporaryName by remember { mutableStateOf("") }
    var startTimeText by remember { mutableStateOf("") }
    var endTimeText by remember { mutableStateOf("") }
    var startMillis by remember { mutableLongStateOf(0L) }
    var endMillis by remember { mutableLongStateOf(0L) }
    var selectedDays by remember { mutableStateOf(setOf(0, 1, 2, 3, 4, 5, 6)) }
    
    // Custom time picker states
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }
    
    // FAB scroll behavior
    val listState = rememberLazyListState()
    val fabVisible by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0
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


    Scaffold(
        snackbarHost = { SnackbarHost(snackBarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.title_schedules),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            androidx.compose.animation.AnimatedVisibility(
                visible = fabVisible && schedules.isNotEmpty(),
                enter = androidx.compose.animation.slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = androidx.compose.animation.core.spring(
                        dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
                        stiffness = androidx.compose.animation.core.Spring.StiffnessLow
                    )
                ) + androidx.compose.animation.fadeIn(
                    animationSpec = androidx.compose.animation.core.tween(300)
                ),
                exit = androidx.compose.animation.slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = androidx.compose.animation.core.tween(300)
                ) + androidx.compose.animation.fadeOut(
                    animationSpec = androidx.compose.animation.core.tween(200)
                )
            ) {
                ExtendedFloatingActionButton(
                    onClick = {
                        schedulesViewModel.loadContacts()
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
    ) { innerPadding ->
        if (schedules.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
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
                            .clickable {
                                schedulesViewModel.loadContacts()
                                showContactSheet = true
                            }
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
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(schedules) { schedule ->
                    ScheduleItem(
                        schedule = schedule,
                        avatarUri = schedule.contactId?.let { schedulesViewModel.contactForId(it)?.image as String? },
                        onEditClick = { sched ->
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
                                            selectedDays = com.purnendu.contactly.utils.DayUtils.extractDaysFromBitmask(entity?.selectedDays ?: 127).toSet()
                                            startTimeText =
                                                if (startMillis > 0) java.text.SimpleDateFormat("HH:mm").format(
                                                    java.util.Date(startMillis)
                                                ) else ""
                                            endTimeText =
                                                if (endMillis > 0) java.text.SimpleDateFormat("HH:mm").format(
                                                    java.util.Date(endMillis)
                                                ) else ""
                                            showEditSheet = true
                                        }
                                    }
                                }
                            }
                        },
                        onDeleteClick = {
                            if(snackBarHostState.currentSnackbarData==null)
                            {
                                coroutineScope.launch {
                                    val result = snackBarHostState.showSnackbar(
                                        message = "Do you want to delete this schedule?",
                                        actionLabel = "Delete",
                                        duration = SnackbarDuration.Short
                                    )
                                    if (result == SnackbarResult.ActionPerformed) {
                                        schedulesViewModel.deleteSchedule(it)
                                    }
                                }
                            }
                        }
                    )
                }
            }
        }
    }

    if (showContactSheet) {
        ContactSelectionBottomSheet(
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
        val formatter = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault())
        EditScheduleSheet(
            error = errorMessage.value,
            onErrorCardDismiss = { schedulesViewModel.clearError() },
            contact = contact,
            temporaryName = temporaryName,
            startTime = if(startMillis==0L) "" else formatter.format(startMillis),
            endTime = if(endMillis==0L) "" else formatter.format(endMillis),
            selectedDays = selectedDays,
            onTemporaryNameChange = { temporaryName = it },
            onDaysChanged = { selectedDays = it },
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

                if(endMillis < startMillis)
                {
                    schedulesViewModel.showError("End time can not be before start time")

                    return@EditScheduleSheet
                }

                if(endMillis == startMillis)
                {
                    schedulesViewModel.showError("End time and start time can not be same")

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
                val selectedDaysBitmask = com.purnendu.contactly.utils.DayUtils.daysToBitmask(selectedDays)
                    val editingId = schedules.firstOrNull { it.name == temporaryName && it.contactId == contact.id }?.id?.toLongOrNull()
                    if (editingId != null) {
                        schedulesViewModel.updateSchedule(
                            editingId,
                            contact,
                            temporaryName,
                            startMillis,
                            endMillis,
                            selectedDaysBitmask
                        )
                        Toast.makeText(context,context.getString(R.string.ScheduleUpdated),Toast.LENGTH_SHORT).show()
                    } else {
                        schedulesViewModel.addSchedule(contact, temporaryName, startMillis, endMillis, selectedDaysBitmask)
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
                val cal = java.util.Calendar.getInstance().apply {
                    set(java.util.Calendar.HOUR_OF_DAY, hour)
                    set(java.util.Calendar.MINUTE, minute)
                    set(java.util.Calendar.SECOND, 0)
                    set(java.util.Calendar.MILLISECOND, 0)
                }
                startMillis = cal.timeInMillis
                val formatter = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault())
                startTimeText = formatter.format(cal.time)
                showStartTimePicker = false
            }
        )
    }
    
    if (showEndTimePicker) {
        ContactlyTimePicker(
            onDismiss = { showEndTimePicker = false },
            onTimeSelected = { hour, minute ->
                val cal = java.util.Calendar.getInstance().apply {
                    set(java.util.Calendar.HOUR_OF_DAY, hour)
                    set(java.util.Calendar.MINUTE, minute)
                    set(java.util.Calendar.SECOND, 0)
                    set(java.util.Calendar.MILLISECOND, 0)
                }
                endMillis = cal.timeInMillis
                val formatter = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault())
                endTimeText = formatter.format(cal.time)
                showEndTimePicker = false
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SchedulesScreenPreview() {
    ContactlyTheme(appThemeMode = com.purnendu.contactly.utils.AppThemeMode.LIGHT) {
        SchedulesScreen(
            schedulesViewModel = viewModel()
        )
    }
}


