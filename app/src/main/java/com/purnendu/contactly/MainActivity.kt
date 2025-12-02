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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.Scaffold
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
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

        Scaffold(
            bottomBar = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    HorizontalDivider(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surfaceVariant)

                    NavigationBar(containerColor = MaterialTheme.colorScheme.surface)
                    {
                        val navBackStackEntry by navController.currentBackStackEntryAsState()
                        val currentDestination = navBackStackEntry?.destination
                        items.forEach { screen ->
                            val isSelected =  currentDestination?.hierarchy?.any { it.route == screen::class.qualifiedName  } == true
                            NavigationBarItem(
                                colors = NavigationBarItemDefaults.colors(indicatorColor = Color.Transparent),
                                icon = {
                                    Icon(
                                        if(isSelected) screen.selectedIcon!! else screen.notSelectedIcon!!,
                                        contentDescription = screen.title
                                    )
                                },
                                label = { Text(screen.title!!, fontWeight = if(isSelected) FontWeight.Bold else FontWeight.Normal) },
                                selected = isSelected,
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
            }
        ) { innerPadding ->
            NavHost(
                navController,
                startDestination = Screen.Schedules,
                Modifier.padding(innerPadding)
            ) {
                composable<Screen.Schedules> {
                    SchedulesScreen(
                        navController=navController,
                        onShowToast = {message->
                            Toast.makeText(this@MainActivity,message,Toast.LENGTH_SHORT).show()
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
                
