package com.purnendu.contactly

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add

import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api

import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.activity.result.ActivityResult
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding

import com.purnendu.contactly.ui.theme.ContactlyTheme
import com.purnendu.contactly.ui.screens.setting.SettingsViewModel
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.SideEffect

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.purnendu.contactly.ui.screens.Screen
import androidx.navigation3.ui.NavDisplay
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.android.play.core.ktx.isFlexibleUpdateAllowed
import com.google.android.play.core.ktx.isImmediateUpdateAllowed
import com.google.firebase.Firebase
import com.google.firebase.crashlytics.crashlytics

import com.purnendu.contactly.ui.components.BottomNavigationWithCutout
import com.purnendu.contactly.ui.navigation.Navigator
import com.purnendu.contactly.ui.navigation.rememberContactlyEntryProvider
import com.purnendu.contactly.ui.navigation.rememberNavigationState
import com.purnendu.contactly.utils.BiometricHelper
import org.koin.compose.viewmodel.koinViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel


class MainActivity : FragmentActivity() {

    private lateinit var appUpdateManager: AppUpdateManager
    private val mainActivityViewModel: MainActivityViewModel by viewModel()
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

        // Handle share intent immediately — ViewModel is ready at Activity level!
        handleShareIntent(intent)

        setContent {
            // Get ViewModels from Koin
            val settingsViewModel: SettingsViewModel = koinViewModel()
            
            // Keep the splash screen visible until the app is ready
            // Keep the splash screen visible until the app is ready and auth is done
            val isAppReady by mainActivityViewModel.isAppReady.collectAsStateWithLifecycle()
            val biometricEnabled by settingsViewModel.biometricEnabled.collectAsStateWithLifecycle()
            var isBiometricCheckPassed by remember { mutableStateOf(false) }
            var hasAttemptedAuth by remember { mutableStateOf(false) }
            
            // Wait for both isAppReady AND biometricEnabled to be loaded (non-null)
            LaunchedEffect(isAppReady, biometricEnabled) {
                // Only proceed when app is ready AND preference has loaded (not null)
                if (isAppReady && biometricEnabled != null && !hasAttemptedAuth) {
                    hasAttemptedAuth = true
                    if (biometricEnabled == true) {
                        val success = BiometricHelper.authenticate(
                            activity = this@MainActivity,
                            title = "Unlock the ${getString(R.string.app_name)} app",
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
            
            // Keep splash until app is ready AND biometric preference is loaded AND auth is passed
            splashScreen.setKeepOnScreenCondition { !isAppReady || biometricEnabled == null || !isBiometricCheckPassed }
            
            if (isBiometricCheckPassed) {
                val themeMode by settingsViewModel.theme.collectAsStateWithLifecycle()
                ContactlyTheme(appThemeMode = themeMode) {
                    // Determine if current theme is dark based on the resolved theme mode
                    val isDarkTheme = when (themeMode) {
                        com.purnendu.contactly.common.AppThemeMode.SYSTEM -> androidx.compose.foundation.isSystemInDarkTheme()
                        com.purnendu.contactly.common.AppThemeMode.DARK -> true
                        com.purnendu.contactly.common.AppThemeMode.LIGHT -> false
                    }
                    
                    // Update status bar appearance: use dark icons in light mode, light icons in dark mode
                    val useDarkIcons = !isDarkTheme
                    SideEffect {
                        WindowCompat.getInsetsController(window, window.decorView).apply {
                            isAppearanceLightStatusBars = useDarkIcons
                            isAppearanceLightNavigationBars = useDarkIcons
                        }
                    }
                    ContactlyApp(mainActivityViewModel)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleShareIntent(intent)
    }

    private fun handleShareIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
            if (!sharedText.isNullOrBlank()) {
                mainActivityViewModel.handleSharedLocation(sharedText)
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun ContactlyApp(mainActivityViewModel: MainActivityViewModel) {
        val bottomNavScreens = listOf(
            Screen.Home,
            Screen.Settings
        )

        val navigationState = rememberNavigationState(
            startRoute = Screen.Home,
            topLevelRoutes = bottomNavScreens.toSet(),
        )
        val navigator = remember(navigationState) {
            Navigator(navigationState)
        }

        val currentTopLevelScreen = navigationState.topLevelRoute as? Screen ?: Screen.Home
        val currentStack = navigationState.backStacks[navigationState.topLevelRoute]
        val currentScreen = currentStack?.lastOrNull() as? Screen ?: Screen.Home
        val currentRoute = currentTopLevelScreen::class.qualifiedName

        val hasActivations by mainActivityViewModel.hasActivations.collectAsStateWithLifecycle()
        val sharedLocationText by mainActivityViewModel.sharedLocationText.collectAsStateWithLifecycle()

        val entryProvider = rememberContactlyEntryProvider(
            versionName = BuildConfig.VERSION_NAME,
            isDebugMode = BuildConfig.DEBUG,
            addActivationEvents = mainActivityViewModel.addActivationEvent,
            sharedLocationText = sharedLocationText,
            onSharedLocationConsumed = mainActivityViewModel::clearSharedLocation,
            onHasActivationsChanged = mainActivityViewModel::setHasActivations,
            navigator = navigator,
        )
        val navKeyEntryProvider: (NavKey) -> NavEntry<NavKey> = { key ->
            @Suppress("UNCHECKED_CAST")
            entryProvider(key as Screen) as NavEntry<NavKey>
        }
        val navEntries = navigationState.toEntries(navKeyEntryProvider)

        BackHandler(enabled = currentScreen != Screen.Home) {
            navigator.goBack()
        }

        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = when (currentScreen) {
                                Screen.Home -> stringResource(id = R.string.title_activations)
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
                if (currentScreen != Screen.Feedback && currentScreen != Screen.PrivacyPolicy) {
                    NavigationBar() { }
                    BottomNavigationWithCutout(
                        modifier = Modifier.windowInsetsPadding(NavigationBarDefaults.windowInsets),
                        screens = bottomNavScreens,
                        currentRoute = currentRoute,
                        onItemClick = { screen ->
                            navigator.navigate(screen)
                        },
                        fabContent = {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = stringResource(id = R.string.action_add_activation),
                                modifier = Modifier.size(28.dp)
                            )
                        },
                        fabOnClick = {
                            if (currentScreen != Screen.Home) {
                                navigator.navigate(Screen.Home)
                            }
                            mainActivityViewModel.triggerAddActivation()
                        },
                        showFab = hasActivations,
                        containerColor = MaterialTheme.colorScheme.surface,
                        fabContainerColor = MaterialTheme.colorScheme.primary,
                        fabContentColor = MaterialTheme.colorScheme.onPrimary
                    )
                }
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { innerPadding ->
            androidx.compose.foundation.layout.Box(
                modifier = Modifier.padding(innerPadding)
            ) {
                NavDisplay(
                    entries = navEntries,
                    onBack = {
                        navigator.goBack()
                    },
                )
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
