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
    data object Home : Screen("home", Icons.Default.Home, Icons.Outlined.Home, "Home")
    @Serializable
    data object Settings : Screen("settings", Icons.Default.Settings, Icons.Outlined.Settings, "Settings")
    @Serializable
    data object Feedback : Screen("feedback", null, null, "Feedback")
    @Serializable
    data object PrivacyPolicy : Screen("privacy_policy", null, null, "Privacy Policy")
}