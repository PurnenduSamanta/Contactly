package com.purnendu.contactly.ui.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

@Serializable
sealed class Screen(val route: String, @Contextual val selectedIcon: ImageVector?, @Contextual val notSelectedIcon: ImageVector?, val title: String?) {
    @Serializable
    data object Schedules : Screen("schedules", Icons.Default.Home, Icons.Outlined.Home, "Home")
    @Serializable
    data object Settings : Screen("settings", Icons.Default.Settings, Icons.Outlined.Settings, "Settings")
}