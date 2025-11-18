package com.purnendu.contactly.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable


@Serializable
sealed class Screen(val route: String, @Contextual val icon: ImageVector?, val title: String?) {
    @Serializable
    data object Schedules : Screen("schedules", Icons.Default.Home, "Home")
    @Serializable
    data object Settings : Screen("settings", Icons.Default.Settings, "Settings")
}
