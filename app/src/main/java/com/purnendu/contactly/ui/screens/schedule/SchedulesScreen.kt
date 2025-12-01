package com.purnendu.contactly.ui.screens.schedule

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.saveable.rememberSaveable
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
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun SchedulesScreen(
    navController: NavController?=null,
    modifier: Modifier = Modifier,
    schedulesViewModel: SchedulesViewModel = viewModel(),
    onShowToast: (String) -> Unit,
    onScheduleExactAlarm: () -> Unit,
    onTimePick: (((Long, String) -> Unit) -> Unit)
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
    var startMillis by remember { mutableStateOf(0L) }
    var endMillis by remember { mutableStateOf(0L) }

    val showContactDialog = schedulesViewModel.showContactPermissionDialog.collectAsStateWithLifecycle()

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

    val combinedPermissionKey = remember { derivedStateOf { buildString {
        append(permissionState.permissions[0].status.toString())
        append(permissionState.permissions[1].status.toString())
    } } }

    val showPermissionDialog = rememberSaveable { mutableStateOf(false) }
    val showAlertDialog = rememberSaveable { mutableStateOf(false) }


    if(showAlertDialog.value)
    {
        ContactlyDialog(
            isConfirmButtonAvailable = true,
            isDismissButtonAvailable = false,
            title =   stringResource(R.string.app_name),
            message = stringResource(R.string.duplicate_contact_alert),
            onConfirm = { showAlertDialog.value = false},
            onDismiss = {},
            confirmButtonText = stringResource(R.string.ok),
            dismissButtonText = ""
        )
    }


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
            if (schedules.isNotEmpty()) {
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
                            .padding(horizontal = 15.dp, vertical = 10.dp)
                            .clickable {
                                schedulesViewModel.loadContacts()
                                showContactSheet = true
                            },
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
            contacts = contacts,
            onDismissContactSelection = {showContactSheet = false},
            onContactClick = { contact ->
                val duplicateScheduledContactList = schedules.filter { it.contactId == contact.id }
                if(duplicateScheduledContactList.isNotEmpty())
                {
                    showContactSheet = false
                    showAlertDialog.value = true
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
            }
        )
    }

    if (showEditSheet) {
        val contact = selectedContact ?: Contact("", "", null)
        EditScheduleSheet(
            contact = contact,
            temporaryName = temporaryName,
            startTime = startTimeText,
            endTime = endTimeText,
            onTemporaryNameChange = { temporaryName = it },
            onStartTimeClick = {
                onTimePick { millis, label ->
                    startMillis = millis
                    startTimeText = label
                }
            },
            onEndTimeClick = {
                onTimePick { millis, label ->
                    endMillis = millis
                    endTimeText = label
                }
            },
            onCancel = {
                showEditSheet = false
                showContactSheet = false
                selectedContact=null
                       },
            onSave = {
                val contact = selectedContact ?: return@EditScheduleSheet

                if(temporaryName.isEmpty() || temporaryName.isBlank())
                {
                    if(snackBarHostState.currentSnackbarData==null)
                    {
                        coroutineScope.launch {
                            val result = snackBarHostState.showSnackbar(
                                message = "Temporary name can not be empty",
                                actionLabel = "Dismiss",
                                duration = SnackbarDuration.Short
                            )
                            if (result == SnackbarResult.ActionPerformed) {
                                snackBarHostState.currentSnackbarData?.dismiss()
                            }
                        }
                    }

                    return@EditScheduleSheet
                }

                if(endMillis <= startMillis)
                {
                    if(snackBarHostState.currentSnackbarData==null)
                    {
                        coroutineScope.launch {
                            val result = snackBarHostState.showSnackbar(
                                message = "End time can not be before start time",
                                actionLabel = "Dismiss",
                                duration = SnackbarDuration.Short
                            )
                            if (result == SnackbarResult.ActionPerformed) {
                                snackBarHostState.currentSnackbarData?.dismiss()
                            }
                        }
                    }

                    return@EditScheduleSheet
                }
                    onScheduleExactAlarm()
                    val editingId =
                        schedules.firstOrNull { it.name == temporaryName && it.contactId == contact.id }?.id?.toLongOrNull()
                    if (editingId != null) {
                        schedulesViewModel.updateSchedule(
                            editingId,
                            contact,
                            temporaryName,
                            startMillis,
                            endMillis
                        )
                    } else {
                        schedulesViewModel.addSchedule(contact, temporaryName, startMillis, endMillis)
                    }
                    showEditSheet = false
                    val writeGranted = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.WRITE_CONTACTS
                    ) == PackageManager.PERMISSION_GRANTED
                    if (!writeGranted) {
                        onShowToast(context.getString(R.string.toast_grant_write))
                    } else {
                        onShowToast(context.getString(R.string.toast_schedule_saved))
                    }

            }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SchedulesScreenPreview() {
    ContactlyTheme(appThemeMode = com.purnendu.contactly.utils.AppThemeMode.LIGHT) {
        SchedulesScreen(
            schedulesViewModel = viewModel(),
            onShowToast = {},
            onScheduleExactAlarm = {},
            onTimePick = {}
        )
    }
}