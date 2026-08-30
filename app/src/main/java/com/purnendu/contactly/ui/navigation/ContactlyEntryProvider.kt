package com.purnendu.contactly.ui.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation3.runtime.entryProvider
import com.purnendu.contactly.ui.screens.Screen
import com.purnendu.contactly.ui.screens.home.HomeScreen
import com.purnendu.contactly.ui.screens.setting.SettingsScreen
import com.purnendu.contactly.ui.screens.webView.FeedbackScreen
import com.purnendu.contactly.ui.screens.webView.PrivacyPolicyScreen
import com.purnendu.contactly.data.utils.isNetworkAvailable
import kotlinx.coroutines.flow.SharedFlow

/**
 * Defines the Nav3 destinations for Contactly.
 *
 * The next migration step will hand this provider to NavDisplay, but we extract
 * it now so destination definitions already live in Nav3 form.
 */
@Composable
fun rememberContactlyEntryProvider(
    versionName: String,
    isDebugMode: Boolean,
    addActivationEvents: SharedFlow<Unit>,
    sharedLocationText: String?,
    onSharedLocationConsumed: () -> Unit,
    onHasActivationsChanged: (Boolean) -> Unit,
    navigator: Navigator,
) = entryProvider {
    entry<Screen.Home> {
        HomeEntryContent(
            addActivationEvents = addActivationEvents,
            sharedLocationText = sharedLocationText,
            onSharedLocationConsumed = onSharedLocationConsumed,
            onHasActivationsChanged = onHasActivationsChanged,
        )
    }
    entry<Screen.Settings> {
        SettingsEntryContent(
            versionName = versionName,
            isDebugMode = isDebugMode,
            navigator = navigator,
        )
    }
    entry<Screen.Feedback> {
        FeedbackEntryContent()
    }
    entry<Screen.PrivacyPolicy> {
        PrivacyPolicyEntryContent()
    }
}

@Composable
private fun HomeEntryContent(
    addActivationEvents: SharedFlow<Unit>,
    sharedLocationText: String?,
    onSharedLocationConsumed: () -> Unit,
    onHasActivationsChanged: (Boolean) -> Unit,
) {
    HomeScreen(
        contentPadding = PaddingValues(),
        addActivationEvents = addActivationEvents,
        sharedLocationText = sharedLocationText,
        onSharedLocationConsumed = onSharedLocationConsumed,
        onHasActivationsChanged = onHasActivationsChanged,
    )
}

@Composable
private fun SettingsEntryContent(
    versionName: String,
    isDebugMode: Boolean,
    navigator: Navigator,
) {
    val context = LocalContext.current

    SettingsScreen(
        versionName = versionName,
        isDebugMode = isDebugMode,
        onNavigateToFeedback = {
            if (isNetworkAvailable(context = context)) {
                navigator.navigate(Screen.Feedback)
            }
        },
        onNavigateToPrivacyPolicy = {
            if (isNetworkAvailable(context = context)) {
                navigator.navigate(Screen.PrivacyPolicy)
            }
        },
    )
}

@Composable
private fun FeedbackEntryContent() {
    FeedbackScreen()
}

@Composable
private fun PrivacyPolicyEntryContent() {
    PrivacyPolicyScreen()
}
