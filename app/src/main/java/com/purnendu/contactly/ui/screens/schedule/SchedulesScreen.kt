package com.purnendu.contactly.ui.screens.schedule

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.purnendu.contactly.R
import com.purnendu.contactly.model.Contact
import com.purnendu.contactly.ui.screens.schedule.components.ScheduleItem
import com.purnendu.contactly.ui.screens.schedule.components.contactSelectionBottomSheet.ContactSelectionBottomSheet
import com.purnendu.contactly.ui.screens.schedule.components.editingBottomSheet.EditScheduleSheet
import com.purnendu.contactly.ui.theme.ContactlyTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SchedulesScreen(
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

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    )
    { result ->
        val readGranted = result[Manifest.permission.READ_CONTACTS] == true
        if (readGranted) {
            schedulesViewModel.loadContacts()
            showContactSheet = true
        } else {
            onShowToast("Contacts permission denied")
        }
    }


    Scaffold(
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
                        permissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.READ_CONTACTS,
                                Manifest.permission.WRITE_CONTACTS
                            )
                        )
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
                                permissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.READ_CONTACTS,
                                        Manifest.permission.WRITE_CONTACTS
                                    )
                                )
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
                        onDeleteClick = { schedulesViewModel.deleteSchedule(it) }
                    )
                }
            }
        }
    }

    if (showContactSheet) {
        ContactSelectionBottomSheet(
            contacts = contacts,
            onContactClick = { contact ->
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
            onCancel = { showEditSheet = false },
            onSave = {
                val c = selectedContact
                if (c != null && temporaryName.isNotBlank() && startMillis > 0L && endMillis > startMillis) {
                    onScheduleExactAlarm()
                    val editingId =
                        schedules.firstOrNull { it.name == temporaryName && it.contactId == c.id }?.id?.toLongOrNull()
                    if (editingId != null) {
                        schedulesViewModel.updateSchedule(
                            editingId,
                            c,
                            temporaryName,
                            startMillis,
                            endMillis
                        )
                    } else {
                        schedulesViewModel.addSchedule(c, temporaryName, startMillis, endMillis)
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
                } else {
                    onShowToast(context.getString(R.string.toast_complete_fields))
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