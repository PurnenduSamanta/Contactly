package com.purnendu.contactly.ui.screens.setting

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.koin.compose.viewmodel.koinViewModel
import com.purnendu.contactly.ui.screens.setting.components.SettingsRow
import com.purnendu.contactly.ui.screens.setting.components.ThemeChip
import com.purnendu.contactly.ui.screens.setting.components.ViewModeToggle
import com.purnendu.contactly.utils.AppThemeMode
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.purnendu.contactly.BuildConfig
import com.purnendu.contactly.R
import com.purnendu.contactly.alarm.AliasAlarmReceiver
import com.purnendu.contactly.ui.components.ContactlyDialog
import com.purnendu.contactly.notification.NotificationHelper
import com.purnendu.contactly.ui.theme.ContactlyTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.core.net.toUri

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun SettingsScreen(
    settingsViewModel: SettingsViewModel= koinViewModel(),
    onNavigateToFeedback: () -> Unit = {},
    onNavigateToPrivacyPolicy: () -> Unit = {}
) {
    val context = LocalContext.current

    val themeMode by settingsViewModel.theme.collectAsStateWithLifecycle()
    val viewMode by settingsViewModel.viewMode.collectAsStateWithLifecycle()
    val notificationsEnabled by settingsViewModel.notificationsEnabled.collectAsStateWithLifecycle()
    val alarmStatusList by settingsViewModel.alarmStatusList.collectAsStateWithLifecycle()
    
    var showAlarmsDialog by remember { mutableStateOf(false) }
    val showPermissionDialog = remember { mutableStateOf(false) }
    val isNotificationSwitchTryToOn = remember { mutableIntStateOf(-1) }
    
    // Permission launcher for POST_NOTIFICATIONS (Android 13+)
    val notificationPermissionState = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        rememberPermissionState(permission = Manifest.permission.POST_NOTIFICATIONS)
    } else {
        null
    }

    LaunchedEffect(key1 = notificationPermissionState?.status, key2 = isNotificationSwitchTryToOn.intValue)
    {
        if(isNotificationSwitchTryToOn.intValue==-1)
            return@LaunchedEffect
        //Checking permission status
        if (notificationPermissionState != null) {
            when {
                notificationPermissionState.status.isGranted -> {
                    showPermissionDialog.value = false
                    settingsViewModel.setNotificationsEnabled(true)
                }

                notificationPermissionState.status.shouldShowRationale -> { showPermissionDialog.value = true }

                !notificationPermissionState.status.isGranted && !notificationPermissionState.status.shouldShowRationale -> { showPermissionDialog.value = true }
            }
        } else {
            showPermissionDialog.value = false
            settingsViewModel.setNotificationsEnabled(true)
        }
    }

    if(showPermissionDialog.value)
    {
        context.apply {
            ContactlyDialog(
                isConfirmButtonAvailable = true,
                isDismissButtonAvailable = true,
                title =   getString(R.string.dialog_notification_title),
                message = getString(R.string.dialog_notification_message),
                onConfirm = {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.parse("package:${context.packageName}")
                    }
                    startActivity(intent)
                    showPermissionDialog.value =false
                },
                onDismiss = { showPermissionDialog.value =false },
                confirmButtonText = getString(R.string.action_settings),
                dismissButtonText = getString(R.string.action_cancel)
            )
        }
    }

    val lifeCycleOwner = LocalLifecycleOwner.current
    DisposableEffect(key1 = lifeCycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if(!NotificationHelper.hasNotificationPermission(context))
                    settingsViewModel.setNotificationsEnabled(false)
            }
        }
        lifeCycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifeCycleOwner.lifecycle.removeObserver(observer)
        }
    }


    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {

            item {
                // Appearance Section Label
                Text(
                    stringResource(id = R.string.section_appearance),
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    modifier = Modifier.padding(
                        start = 16.dp,
                        top = 16.dp,
                        bottom = 10.dp
                    )
                )
            }

            item {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ThemeChip(
                        stringResource(id = R.string.theme_light),
                        themeMode == AppThemeMode.LIGHT
                    ) {
                        settingsViewModel.setTheme(AppThemeMode.LIGHT)
                    }
                    ThemeChip(
                        stringResource(id = R.string.theme_dark),
                        themeMode == AppThemeMode.DARK
                    ) {
                        settingsViewModel.setTheme(AppThemeMode.DARK)
                    }
                    ThemeChip(
                        stringResource(id = R.string.theme_system),
                        themeMode == AppThemeMode.SYSTEM
                    ) {
                        settingsViewModel.setTheme(AppThemeMode.SYSTEM)
                    }
                }
            }

            // View Mode Section
            item {
                Text(
                    "View Mode",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    modifier = Modifier.padding(
                        start = 16.dp,
                        top = 28.dp,
                        bottom = 10.dp
                    )
                )
            }
            
            item {
                ViewModeToggle(
                    selectedMode = viewMode,
                    onModeChange = { settingsViewModel.setViewMode(it) },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
            
            // Notifications Section
            item {
                Text(
                    "Notifications",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    modifier = Modifier.padding(
                        start = 16.dp,
                        top = 28.dp,
                        bottom = 10.dp
                    )
                )
            }
            
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Schedule Notifications",
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            "Get fun notifications while changing your contact name 🎭",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    }
                    Switch(
                        checked = notificationsEnabled,
                        onCheckedChange = { enabled ->
                            println("Switch $enabled")
                            if (enabled) {
                                isNotificationSwitchTryToOn.intValue += 1
                                // Check if we need to request permission (Android 13+)
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && 
                                    !NotificationHelper.hasNotificationPermission(context)) {
                                    notificationPermissionState?.launchPermissionRequest()
                                } else {
                                    settingsViewModel.setNotificationsEnabled(true)
                                }
                            } else {
                                isNotificationSwitchTryToOn.intValue = -1
                                settingsViewModel.setNotificationsEnabled(false)
                            }
                        }
                    )
                }
            }

            item {
                Text(
                    stringResource(id = R.string.section_about),
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    modifier = Modifier.padding(start = 16.dp, top = 28.dp, bottom = 6.dp)
                )
            }

            // Version Row
            item {
                SettingsRow(
                    name = stringResource(id = R.string.row_version),
                    value = BuildConfig.VERSION_NAME,
                    onClick = null
                )
            }

            // Privacy Policy Row
            item {
                SettingsRow(
                    name = stringResource(id = R.string.row_privacy_policy),
                    value = null,
                    onClick = onNavigateToPrivacyPolicy
                )
            }

            // Rate Us Row
            item {
                SettingsRow(
                    name = stringResource(id = R.string.row_rate_us),
                    value = null
                )
                {
                    val packageName = context.packageName
                    try {
                        // Try to open Play Store app directly
                        val intent = Intent(
                            Intent.ACTION_VIEW,
                            "market://details?id=$packageName".toUri()
                        )
                        context.startActivity(intent)
                    } catch (e: android.content.ActivityNotFoundException) {
                        // Fallback to web browser if Play Store app is not available
                        val intent = Intent(
                            Intent.ACTION_VIEW,
                            "https://play.google.com/store/apps/details?id=$packageName".toUri()
                        )
                        context.startActivity(intent)
                    }
                }
            }

            // Feedback Row
            item {
                SettingsRow(
                    name = stringResource(id = R.string.row_feedback),
                    value = null,
                    onClick = onNavigateToFeedback
                )
            }

           /* // Terms Row
            item {
                SettingsRow(
                    name = stringResource(id = R.string.row_terms),
                    value = null
                )
                {
                    val intent = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://example.com/terms")
                    )
                    context.startActivity(intent)
                }
            }*/


            if(BuildConfig.DEBUG)
            {
                // Debug Section
                item {
                    Text(
                        "Debug",
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        modifier = Modifier.padding(start = 16.dp, top = 28.dp, bottom = 6.dp)
                    )
                }

                // View Scheduled Alarms Row
                item {
                    SettingsRow(
                        name = "View Alarm Status",
                        value = null
                    )
                    {
                        settingsViewModel.loadAlarmStatus(true)
                        showAlarmsDialog = true
                    }
            }
        }
    }
    
    // Scheduled Alarms Dialog
    if (showAlarmsDialog) {
        val timeFormatter = SimpleDateFormat("hh:mm a", Locale.getDefault())
        val dayNames = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
        
        // Count total alarms and active alarms
        val totalAlarms = alarmStatusList.sumOf { it.alarms.size }
        val activeAlarms = alarmStatusList.sumOf { info -> info.alarms.count { it.isSetInAlarmManager } }
        
        AlertDialog(
            onDismissRequest = { showAlarmsDialog = false },
            title = { 
                Column {
                    Text(
                        "Alarm Status",
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "$activeAlarms / $totalAlarms alarms active",
                        fontSize = 12.sp,
                        color = if (activeAlarms == totalAlarms) Color(0xFF4CAF50) else Color(0xFFFF9800)
                    )
                }
            },
            text = {
                if (alarmStatusList.isEmpty()) {
                    Text("No schedules found")
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 400.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(alarmStatusList) { statusInfo ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp)
                                ) {
                                    // Schedule header
                                    Text(
                                        statusInfo.schedule.temporaryName,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        "Type: ${if (statusInfo.schedule.scheduleType == 0) "One-Time" else "Repeat"}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                    
                                    Spacer(modifier = Modifier.height(8.dp))
                                    
                                    // Alarm status for each alarm
                                    if (statusInfo.alarms.isEmpty()) {
                                        Text(
                                            "⚠️ No alarm metadata found",
                                            fontSize = 12.sp,
                                            color = Color(0xFFFF9800)
                                        )
                                    } else {
                                        statusInfo.alarms.forEach { alarmResult ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 2.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                // Status indicator
                                                Icon(
                                                    imageVector = if (alarmResult.isSetInAlarmManager) 
                                                        Icons.Default.Check else Icons.Default.Close,
                                                    contentDescription = null,
                                                    modifier = Modifier
                                                        .size(16.dp)
                                                        .background(
                                                            if (alarmResult.isSetInAlarmManager) 
                                                                Color(0xFF4CAF50) else Color(0xFFF44336),
                                                            CircleShape
                                                        )
                                                        .padding(2.dp),
                                                    tint = Color.White
                                                )
                                                
                                                Spacer(modifier = Modifier.width(8.dp))
                                                
                                                Column {
                                                    val opLabel = if (alarmResult.metadata.operation == AliasAlarmReceiver.OP_APPLY) 
                                                        "APPLY" else "REVERT"
                                                    val dayName = if (alarmResult.metadata.dayOfWeek in 0..6) 
                                                        dayNames[alarmResult.metadata.dayOfWeek] else "?"
                                                    
                                                    Text(
                                                        "$opLabel on $dayName",
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                    Text(
                                                        "→ \"${alarmResult.name}\" at ${timeFormatter.format(Date(alarmResult.metadata.triggerTimeMillis))}",
                                                        fontSize = 10.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                                    )
                                                    Text(
                                                        "ReqCode: ${alarmResult.metadata.requestCode}",
                                                        fontSize = 9.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAlarmsDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}


@Preview
@Composable
fun SettingsScreenPreview() {
    ContactlyTheme {SettingsScreen()}
}