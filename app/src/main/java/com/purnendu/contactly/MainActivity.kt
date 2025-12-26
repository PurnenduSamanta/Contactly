package com.purnendu.contactly

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

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
import com.purnendu.contactly.ui.screens.schedule.SchedulesScreen
import com.purnendu.contactly.ui.screens.setting.SettingsScreen


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
            val themeMode by settingsViewModel.theme.collectAsStateWithLifecycle()
            ContactlyTheme(appThemeMode = themeMode) { ContactlyApp() }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun ContactlyApp() {
        val navController = rememberNavController()
        val items = listOf(
            Screen.Schedules,
            Screen.Settings,
        )
        
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination
        
        // Determine current screen for top bar title
        val currentScreen = items.find { screen ->
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
                Column(modifier = Modifier.fillMaxWidth()) {
                    HorizontalDivider(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    )

                    NavigationBar(
                        windowInsets = WindowInsets.navigationBars,
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        items.forEach { screen ->
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
                    SettingsScreen() 
                }
            }
        }
    }
}
