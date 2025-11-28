package com.purnendu.contactly

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.app.AlarmManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.purnendu.contactly.ui.theme.ContactlyTheme
import com.purnendu.contactly.ui.screens.setting.SettingsViewModel
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.Scaffold
import androidx.navigation.compose.rememberNavController
import com.purnendu.contactly.ui.Screen
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import com.purnendu.contactly.components.pickTime
import com.purnendu.contactly.ui.screens.schedule.SchedulesScreen
import com.purnendu.contactly.ui.screens.setting.SettingsScreen
import com.purnendu.contactly.ui.theme.ThemedPermissionDialog


class MainActivity : ComponentActivity() {
    private val settingsViewModel: SettingsViewModel by viewModels()
    private val mainActivityViewModel: MainActivityViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        // Install the splash screen
        val splashScreen = installSplashScreen()

        // Keep the splash screen visible until the app is ready
        splashScreen.setKeepOnScreenCondition {
            !mainActivityViewModel.isAppReady.value
        }

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val themeMode by settingsViewModel.theme.collectAsState()
            ContactlyTheme(appThemeMode = themeMode) {
                ContactlyApp()
            }
        }
    }

    @Composable
    fun ContactlyApp() {
        val navController = rememberNavController()
        val items = listOf(
            Screen.Schedules,
            Screen.Settings,
        )

        // Show permission dialogs if needed
        val showExactAlarmDialog by mainActivityViewModel.showExactAlarmPermissionDialog.collectAsState()
        val showContactDialog by mainActivityViewModel.showContactPermissionDialog.collectAsState()

        if (showExactAlarmDialog || showContactDialog) {
            val title = if (showExactAlarmDialog && showContactDialog) {
                "Permissions Required"
            } else if (showExactAlarmDialog) {
                getString(R.string.dialog_exact_alarm_title)
            } else {
                getString(R.string.dialog_contacts_title)
            }

            val message = if (showExactAlarmDialog && showContactDialog) {
                "This app requires both exact alarm and contacts permissions to function properly. Please enable these permissions in settings."
            } else if (showExactAlarmDialog) {
                getString(R.string.dialog_exact_alarm_message)
            } else {
                getString(R.string.dialog_contacts_message)
            }

            ThemedPermissionDialog(
                title = title,
                message = message,
                onConfirm = {
                    if (showExactAlarmDialog) {
                        val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                        startActivity(intent)
                        mainActivityViewModel.dismissExactAlarmPermissionDialog()
                    }
                    if (showContactDialog) {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = android.net.Uri.parse("package:$packageName")
                        }
                        startActivity(intent)
                        mainActivityViewModel.dismissContactPermissionDialog()
                    }
                },
                onDismiss = { finish() },
                confirmText = getString(R.string.action_settings),
                dismissText = getString(R.string.action_exit)
            )
        }

        Scaffold(
            bottomBar = {
                NavigationBar {
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentDestination = navBackStackEntry?.destination
                    items.forEach { screen ->
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    screen.icon!!,
                                    contentDescription = screen.title
                                )
                            },
                            label = { Text(screen.title!!) },
                            selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                            onClick = {
                                navController.navigate(screen) {
                                    // Pop up to the start destination of the graph to
                                    // avoid building up a large stack of destinations
                                    // on the back stack as users select items
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    // Avoid multiple copies of the same destination when
                                    // reselecting the same item
                                    launchSingleTop = true
                                    // Restore state when reselecting a previously selected item
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        ) { innerPadding ->
            NavHost(
                navController,
                startDestination = Screen.Schedules,
                Modifier.padding(innerPadding)
            ) {
                composable<Screen.Schedules> {
                    SchedulesScreen(
                        onShowToast = {message->
                            Toast.makeText(this@MainActivity,message,Toast.LENGTH_SHORT).show()
                        },
                        onScheduleExactAlarm = {
                            val am = getSystemService(AlarmManager::class.java)
                            val exactOk = if (android.os.Build.VERSION.SDK_INT >= 31) am.canScheduleExactAlarms() else true
                            if (!exactOk) {
                                Toast.makeText(this@MainActivity, getString(R.string.toast_enable_exact_alarm), Toast.LENGTH_LONG).show()
                                val i = android.content.Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                                startActivity(i)
                            }
                        },
                        onTimePick = { onPicked->
                            pickTime(this@MainActivity,onPicked)
                        }
                    )
                }
                composable<Screen.Settings> {
                    SettingsScreen(
                        settingsViewModel = settingsViewModel,
                        onPrivacyPolicyClick = {
                            val intent = Intent(
                                Intent.ACTION_VIEW,
                                android.net.Uri.parse("https://example.com/privacy")
                            )
                            startActivity(intent)
                        },
                        onTermsClick = {
                            val intent = Intent(
                                Intent.ACTION_VIEW,
                                android.net.Uri.parse("https://example.com/terms")
                            )
                            startActivity(intent)
                        }
                    )
                }
            }
        }
    }
}
                
