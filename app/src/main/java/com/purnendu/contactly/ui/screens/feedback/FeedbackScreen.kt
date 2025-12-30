package com.purnendu.contactly.ui.screens.feedback

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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.graphics.toArgb
import com.purnendu.contactly.R


private const val FEEDBACK_FORM_URL = "https://forms.gle/RorurgiCh4Jgh6C56"

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun FeedbackScreen(
    onBackClick: () -> Unit
) {
    var isLoading by remember { mutableStateOf(true) }
    var loadingProgress by remember { mutableIntStateOf(0) }
    var webView: WebView? by remember { mutableStateOf(null) }
    
    // Handle back button press - navigate back in WebView if possible
    BackHandler(enabled = webView?.canGoBack() == true) {
        webView?.goBack()
    }

    val backgroundColor = MaterialTheme.colorScheme.background
    // Calculate if dark mode is active based on background luminance
    val isDarkTheme = remember(backgroundColor) {
        // Luminance calculation: (0.2126*R + 0.7152*G + 0.0722*B)
        // We can do this manually without extra imports or use helper if available.
        // Manual implementation avoiding complex imports:
        val argb = backgroundColor.toArgb()
        val r = android.graphics.Color.red(argb)
        val g = android.graphics.Color.green(argb)
        val b = android.graphics.Color.blue(argb)
        (0.2126 * r + 0.7152 * g + 0.0722 * b) / 255 < 0.5
    }

        Column(
            modifier = Modifier
                .fillMaxSize()
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
                    factory = { context ->
                        WebView(context).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            
                            // Enable JavaScript (required for Google Forms)
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            
                            // optimized for mobile - disable 'desktop' mode
                            settings.loadWithOverviewMode = false 
                            settings.useWideViewPort = false
                            
                            // Dark mode support
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                                settings.forceDark = if (isDarkTheme) android.webkit.WebSettings.FORCE_DARK_ON else android.webkit.WebSettings.FORCE_DARK_OFF
                            }
                            
                            webViewClient = object : WebViewClient() {
                                override fun shouldOverrideUrlLoading(view: WebView?, request: android.webkit.WebResourceRequest?): Boolean {
                                    return false // Load all URLs in this WebView
                                }

                                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                    super.onPageStarted(view, url, favicon)
                                    isLoading = true
                                }
                                
                                override fun onPageFinished(view: WebView?, url: String?) {
                                    super.onPageFinished(view, url)
                                    isLoading = false
                                    // Inject JS to remove default margins causing gaps
                                    view?.evaluateJavascript(
                                        "document.body.style.margin='0'; document.body.style.padding='0';", 
                                        null
                                    )
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
                            
                            loadUrl(FEEDBACK_FORM_URL)
                            webView = this
                        }
                    },
                    modifier = Modifier.fillMaxSize()
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
