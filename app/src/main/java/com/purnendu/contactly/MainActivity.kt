package com.purnendu.contactly

import android.os.Bundle
import android.provider.Settings
import android.app.AlarmManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.padding
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.purnendu.contactly.ui.screens.schedule.SchedulesScreen
import com.purnendu.contactly.ui.screens.schedule.SchedulesViewModel
import com.purnendu.contactly.ui.screens.schedule.components.contactSelectionBottomSheet.ContactSelectionBottomSheet
import com.purnendu.contactly.ui.screens.schedule.components.editingBottomSheet.EditScheduleSheet
import com.purnendu.contactly.ui.theme.ContactlyTheme
import com.purnendu.contactly.ui.screens.setting.SettingsViewModel
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.ui.res.stringResource
import androidx.compose.material3.Scaffold
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.Modifier
import com.purnendu.contactly.utils.AppThemeMode

class MainActivity : ComponentActivity() {
    private val schedulesViewModel: SchedulesViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeMode by settingsViewModel.theme.collectAsState()
            ContactlyTheme(appThemeMode = themeMode) {
                ContactlyApp(themeMode)
            }
        }
    }

    @Composable
    fun  ContactlyApp(themeMode: AppThemeMode)
    {
        val schedules by schedulesViewModel.schedules.collectAsState()
        val contacts by schedulesViewModel.contacts.collectAsState()

        var showContactSheet by remember { mutableStateOf(false) }
        var showEditSheet by remember { mutableStateOf(false) }
        var selectedContact by remember { mutableStateOf<com.purnendu.contactly.model.Contact?>(null) }
        var temporaryName by remember { mutableStateOf("") }
        var startTimeText by remember { mutableStateOf("") }
        var endTimeText by remember { mutableStateOf("") }
        var startMillis by remember { mutableStateOf(0L) }
        var endMillis by remember { mutableStateOf(0L) }
        var selectedTab by remember { mutableStateOf(0) }

        val permissionLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        )
        { result ->
            val readGranted = result[android.Manifest.permission.READ_CONTACTS] == true
            if (readGranted) {
                schedulesViewModel.loadContacts()
                showContactSheet = true
            } else {
                Toast.makeText(this, "Contacts permission denied", Toast.LENGTH_SHORT).show()
            }
        }

        Scaffold(
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = { Icon(Icons.Default.Home, contentDescription = stringResource(id = R.string.label_home)) },
                        label = { Text(stringResource(id = R.string.label_home)) }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = { Icon(Icons.Default.Settings, contentDescription = stringResource(id = R.string.label_settings)) },
                        label = { Text(stringResource(id = R.string.label_settings)) }
                    )
                }
            })

        { innerPadding ->

            if (selectedTab == 0) {
                SchedulesScreen(
                    modifier = Modifier.padding(innerPadding),
                    schedules = schedules,
                    resolveAvatar = { id -> schedulesViewModel.contactForId(id)?.image as String? },
                    onEditClick = { sched ->
                        val cid = sched.contactId
                        if (cid != null) {
                            permissionLauncher.launch(arrayOf(android.Manifest.permission.READ_CONTACTS, android.Manifest.permission.WRITE_CONTACTS))
                            val contact = schedulesViewModel.contactForId(cid)
                            if (contact != null) {
                                selectedContact = contact
                                temporaryName = sched.name
                                val sid = sched.id.toLongOrNull()
                                if (sid != null) {
                                    this@MainActivity.lifecycleScope.launch {
                                        val entity = schedulesViewModel.loadScheduleEntity(sid)
                                        startMillis = entity?.startAtMillis ?: 0L
                                        endMillis = entity?.endAtMillis ?: 0L
                                        startTimeText = if (startMillis > 0) java.text.SimpleDateFormat("HH:mm").format(java.util.Date(startMillis)) else ""
                                        endTimeText = if (endMillis > 0) java.text.SimpleDateFormat("HH:mm").format(java.util.Date(endMillis)) else ""
                                        showEditSheet = true
                                    }
                                }
                            }
                        }
                    },
                    onDeleteClick = { schedulesViewModel.deleteSchedule(it) },
                    onAddClick = {
                        permissionLauncher.launch(arrayOf(android.Manifest.permission.READ_CONTACTS, android.Manifest.permission.WRITE_CONTACTS))
                    },
                    onHomeClick = {
                        Toast.makeText(this, getString(R.string.toast_home), Toast.LENGTH_SHORT).show()
                    },
                    onSettingsClick = {
                        selectedTab = 1
                    }
                )
            }
            else {
                com.purnendu.contactly.ui.screens.setting.SettingsScreen(
                    currentTheme = themeMode,
                    onThemeChange = { settingsViewModel.setTheme(it) },
                    onBack = { selectedTab = 0 },
                    onPrivacyPolicyClick = {
                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://example.com/privacy"))
                        startActivity(intent)
                    },
                    onTermsClick = {
                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://example.com/terms"))
                        startActivity(intent)
                    }
                )
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
            val contact = selectedContact ?: com.purnendu.contactly.model.Contact("", "", null)
            EditScheduleSheet(
                contact = contact,
                temporaryName = temporaryName,
                startTime = startTimeText,
                endTime = endTimeText,
                onTemporaryNameChange = { temporaryName = it },
                onStartTimeClick = {
                    pickTime { millis, label ->
                        startMillis = millis
                        startTimeText = label
                    }
                },
                onEndTimeClick = {
                    pickTime { millis, label ->
                        endMillis = millis
                        endTimeText = label
                    }
                },
                onCancel = { showEditSheet = false },
                onSave = {
                    val c = selectedContact
                    if (c != null && temporaryName.isNotBlank() && startMillis > 0L && endMillis > startMillis) {
                        val am = getSystemService(AlarmManager::class.java)
                        val exactOk = if (android.os.Build.VERSION.SDK_INT >= 31) am.canScheduleExactAlarms() else true
                        if (!exactOk) {
                            Toast.makeText(this, getString(R.string.toast_enable_exact_alarm), Toast.LENGTH_LONG).show()
                            val i = android.content.Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                            startActivity(i)
                        }
                        val editingId = schedules.firstOrNull { it.name == temporaryName && it.contactId == c.id }?.id?.toLongOrNull()
                        if (editingId != null) {
                            schedulesViewModel.updateSchedule(editingId, c, temporaryName, startMillis, endMillis)
                        } else {
                            schedulesViewModel.addSchedule(c, temporaryName, startMillis, endMillis)
                        }
                        showEditSheet = false
                        val writeGranted = androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_CONTACTS) == android.content.pm.PackageManager.PERMISSION_GRANTED
                        if (!writeGranted) {
                            Toast.makeText(this, getString(R.string.toast_grant_write), Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(this, getString(R.string.toast_schedule_saved), Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(this, getString(R.string.toast_complete_fields), Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }


    }

    private fun pickTime(onPicked: (Long, String) -> Unit) {
        val now = java.util.Calendar.getInstance()
        val dlg = android.app.TimePickerDialog(
            this,
            { _, hourOfDay, minute ->
                val cal = java.util.Calendar.getInstance().apply {
                    set(java.util.Calendar.HOUR_OF_DAY, hourOfDay)
                    set(java.util.Calendar.MINUTE, minute)
                    set(java.util.Calendar.SECOND, 0)
                    set(java.util.Calendar.MILLISECOND, 0)
                }
                val millis = cal.timeInMillis
                val label = String.format("%02d:%02d", hourOfDay, minute)
                onPicked(millis, label)
            },
            now.get(java.util.Calendar.HOUR_OF_DAY),
            now.get(java.util.Calendar.MINUTE),
            true
        )
        dlg.show()
    }
}
                
