package com.purnendu.contactly

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.activity.result.ActivityResult

import com.purnendu.contactly.ui.theme.ContactlyTheme
import com.purnendu.contactly.ui.screens.setting.SettingsViewModel
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.purnendu.contactly.ui.screens.Screen
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.android.play.core.ktx.isFlexibleUpdateAllowed
import com.google.android.play.core.ktx.isImmediateUpdateAllowed
import com.google.firebase.Firebase
import com.google.firebase.crashlytics.crashlytics
import com.purnendu.contactly.ui.screens.schedule.SchedulesScreen
import com.purnendu.contactly.ui.screens.setting.SettingsScreen
import com.purnendu.contactly.ui.screens.webView.FeedbackScreen
import com.purnendu.contactly.ui.screens.webView.PrivacyPolicyScreen
import org.koin.compose.viewmodel.koinViewModel


class MainActivity : FragmentActivity() {

    private lateinit var appUpdateManager: AppUpdateManager
    private val updateType = AppUpdateType.IMMEDIATE
    private val activityResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result: ActivityResult ->
            if (result.resultCode != RESULT_OK) {
                //  this.finish()
            } else if (result.resultCode == RESULT_CANCELED) {
                //  this.finish()
            } else if (result.resultCode == com.google.android.play.core.install.model.ActivityResult.RESULT_IN_APP_UPDATE_FAILED) {
                //  this.finish()
            }
        }
    
    override fun onCreate(savedInstanceState: Bundle?) {

        Firebase.crashlytics.isCrashlyticsCollectionEnabled = !BuildConfig.DEBUG

        // Install the splash screen
        val splashScreen = installSplashScreen()

        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        appUpdateManager = AppUpdateManagerFactory.create(applicationContext)
        checkForAppUpdate()

        setContent {
            // Get ViewModels from Koin
            val mainActivityViewModel: MainActivityViewModel = koinViewModel()
            val settingsViewModel: SettingsViewModel = koinViewModel()
            
            // Keep the splash screen visible until the app is ready
            // Keep the splash screen visible until the app is ready and auth is done
            val isAppReady by mainActivityViewModel.isAppReady.collectAsStateWithLifecycle()
            val biometricEnabled by settingsViewModel.biometricEnabled.collectAsStateWithLifecycle()
            var isBiometricCheckPassed by remember { mutableStateOf(false) }
            
            LaunchedEffect(isAppReady) {
                if (isAppReady) {
                    if (biometricEnabled) {
                        val success = com.purnendu.contactly.utils.BiometricHelper.authenticate(
                            activity = this@MainActivity,
                            title = getString(R.string.app_name),
                            subtitle = getString(R.string.desc_biometric_auth)
                        )
                        if (success) {
                            isBiometricCheckPassed = true
                        } else {
                            finish()
                        }
                    } else {
                        isBiometricCheckPassed = true
                    }
                }
            }
            
            splashScreen.setKeepOnScreenCondition { !isAppReady || !isBiometricCheckPassed }
            
            if (isBiometricCheckPassed) {
                val themeMode by settingsViewModel.theme.collectAsStateWithLifecycle()
                ContactlyTheme(appThemeMode = themeMode) { ContactlyApp() }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun ContactlyApp() {
        val navController = rememberNavController()
        val bottomNavigationScreens = listOf(
            Screen.Schedules,
            Screen.Settings,
        )
        val allScreens = listOf(
            Screen.Schedules,
            Screen.Settings,
            Screen.Feedback,
            Screen.PrivacyPolicy,
        )
        
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination
        
        // Determine current screen for top bar title
        val currentScreen = allScreens.find { screen ->
            currentDestination?.hierarchy?.any { it.route == screen::class.qualifiedName } == true
        } ?: Screen.Schedules

        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = when (currentScreen) {
                                Screen.Schedules -> stringResource(id = R.string.title_schedules)
                                Screen.Settings ->  stringResource(id = R.string.title_settings)
                                Screen.Feedback ->  stringResource(id = R.string.title_feedback)
                                Screen.PrivacyPolicy -> stringResource(id = R.string.row_privacy_policy)
                            },
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            },
            bottomBar = {
                // Hide bottom navigation bar on Feedback and PrivacyPolicy screens
                if (currentScreen != Screen.Feedback && currentScreen != Screen.PrivacyPolicy) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        HorizontalDivider(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        )

                        NavigationBar(
                            windowInsets = WindowInsets.navigationBars,
                            containerColor = MaterialTheme.colorScheme.surface
                        ) {
                            bottomNavigationScreens.forEach { screen ->
                                val isSelected = currentDestination?.hierarchy?.any { 
                                    it.route == screen::class.qualifiedName 
                                } == true
                                NavigationBarItem(
                                    colors = NavigationBarItemDefaults.colors(indicatorColor = Color.Transparent),
                                    icon = {
                                        Icon(
                                            if (isSelected) screen.selectedIcon!! else screen.notSelectedIcon!!,
                                            contentDescription = screen.title
                                        )
                                    },
                                    label = {
                                        Text(
                                            screen.title!!,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    selected = isSelected,
                                    onClick = {
                                        navController.navigate(screen) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { innerPadding ->
            // All padding controlled from here - single source of truth
            NavHost(
                navController = navController,
                startDestination = Screen.Schedules,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable<Screen.Schedules> { 
                    SchedulesScreen(
                        navController = navController,
                        contentPadding = PaddingValues() // No extra padding needed
                    ) 
                }
                composable<Screen.Settings> { 
                    SettingsScreen(
                        onNavigateToFeedback = {
                            navController.navigate(Screen.Feedback)
                        },
                        onNavigateToPrivacyPolicy = {
                            navController.navigate(Screen.PrivacyPolicy)
                        }
                    ) 
                }
                composable<Screen.Feedback> {
                    FeedbackScreen()
                }
                composable<Screen.PrivacyPolicy> {
                    PrivacyPolicyScreen()
                }
            }
        }
    }

    private fun checkForAppUpdate() {
        appUpdateManager.appUpdateInfo.addOnSuccessListener { info ->
            val isUpdateAvailable = info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
            val isUpdateAllowed = when (updateType) {
                AppUpdateType.IMMEDIATE -> info.isImmediateUpdateAllowed
                AppUpdateType.FLEXIBLE -> info.isFlexibleUpdateAllowed
                else -> false
            }
            if (isUpdateAvailable && isUpdateAllowed) {
                appUpdateManager.startUpdateFlowForResult(
                    info,
                    activityResultLauncher,
                    AppUpdateOptions.newBuilder(updateType).build()
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()

        appUpdateManager
            .appUpdateInfo
            .addOnSuccessListener { appUpdateInfo ->
                if (appUpdateInfo.updateAvailability()
                    == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS
                ) {
                    // If an in-app update is already running, resume the update.
                    appUpdateManager.startUpdateFlowForResult(
                        appUpdateInfo,
                        activityResultLauncher,
                        AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build()
                    )
                }
            }
    }
}
