package com.purnendu.contactly.ui.screens.feedback

import androidx.compose.runtime.Composable
import com.purnendu.contactly.ui.components.ContactlyWebView

private const val FEEDBACK_FORM_URL = "https://forms.gle/RorurgiCh4Jgh6C56"

@Composable
fun FeedbackScreen(
    onBackClick: () -> Unit
) {
    ContactlyWebView(url = FEEDBACK_FORM_URL)
}
