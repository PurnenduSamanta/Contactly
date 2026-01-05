package com.purnendu.contactly.ui.screens.privacypolicy

import androidx.compose.runtime.Composable
import com.purnendu.contactly.ui.components.ContactlyWebView

private const val PRIVACY_POLICY_URL = "https://www.termsfeed.com/live/02116554-2ff0-496d-9ab2-6444fd44ef29"

@Composable
fun PrivacyPolicyScreen(
    onBackClick: () -> Unit
) {
    ContactlyWebView(url = PRIVACY_POLICY_URL)
}
