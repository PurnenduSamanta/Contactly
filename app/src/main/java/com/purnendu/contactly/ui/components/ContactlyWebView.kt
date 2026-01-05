package com.purnendu.contactly.ui.components

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

/**
 * Shared WebView component for displaying web content within the app.
 * Used by FeedbackScreen and PrivacyPolicyScreen.
 * 
 * @param url The URL to load in the WebView
 * @param modifier Modifier for the component
 * @param injectCss Optional CSS to inject after page loads (removes margins/padding by default)
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ContactlyWebView(
    url: String,
    modifier: Modifier = Modifier,
    injectCss: String = "document.body.style.margin='0'; document.body.style.padding='0';"
) {
    var isLoading by remember { mutableStateOf(true) }
    var loadingProgress by remember { mutableIntStateOf(0) }
    var webView: WebView? by remember { mutableStateOf(null) }

    // Handle back button press - navigate back in WebView if possible
    BackHandler(enabled = webView?.canGoBack() == true) {
        webView?.goBack()
    }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Show loading progress bar
        if (isLoading) {
            LinearProgressIndicator(
                progress = { loadingProgress / 100f },
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    WebView(context).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )

                        // Enable JavaScript (required for Google Forms and other interactive content)
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true

                        // Optimized for mobile - disable 'desktop' mode
                        settings.loadWithOverviewMode = false
                        settings.useWideViewPort = false

                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(
                                view: WebView?,
                                request: android.webkit.WebResourceRequest?
                            ): Boolean {
                                return false // Load all URLs in this WebView
                            }

                            override fun onPageStarted(
                                view: WebView?,
                                url: String?,
                                favicon: Bitmap?
                            ) {
                                super.onPageStarted(view, url, favicon)
                                isLoading = true
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                isLoading = false
                                // Inject JS to remove default margins causing gaps
                                if (injectCss.isNotEmpty()) {
                                    view?.evaluateJavascript(injectCss, null)
                                }
                            }
                        }

                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                super.onProgressChanged(view, newProgress)
                                loadingProgress = newProgress
                                if (newProgress == 100) {
                                    isLoading = false
                                }
                            }
                        }

                        loadUrl(url)
                        webView = this
                    }
                },
            )

            // Show loading indicator in center while page loads initially
            if (isLoading && loadingProgress < 10) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
