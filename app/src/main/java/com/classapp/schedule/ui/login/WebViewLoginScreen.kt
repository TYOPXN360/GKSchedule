package com.classapp.schedule.ui.login

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.webkit.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import com.classapp.schedule.R

/**
 * WebView-based login screen that loads the CAS login page.
 * Supports QR code scan (DingTalk) and password login.
 * Captures the ticket from redirect URL after successful login.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebViewLoginScreen(
    onLoginSuccess: (ticket: String) -> Unit,
    onBack: () -> Unit
) {
    var isLoading by remember { mutableStateOf(true) }
    var currentUrl by remember { mutableStateOf("") }

    // The CAS login URL that redirects to portal after login
    val casLoginUrl = "https://cas.gdust.edu.cn/cas/?service=https%3A%2F%2Fportal.gdust.edu.cn%2Fsmart-admin-api%2Fuser%2FssoLogin%2F%3FappId%3DportalRemote"

    Scaffold(
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.webview_login_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            AndroidView(
                factory = { context ->
                    WebView(context).apply {
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            databaseEnabled = true
                            setSupportZoom(true)
                            builtInZoomControls = true
                            displayZoomControls = false
                            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                            userAgentString = "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
                        }

                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                isLoading = true
                                currentUrl = url ?: ""
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                isLoading = false
                                currentUrl = url ?: ""

                                // Check if we got redirected to portal with a ticket
                                if (url != null && url.contains("portal.gdust.edu.cn") && url.contains("loginCode=")) {
                                    // Extract loginCode from URL
                                    val loginCode = url.substringAfter("loginCode=").substringBefore("&").substringBefore("#")
                                    if (loginCode.isNotEmpty()) {
                                        onLoginSuccess(loginCode)
                                    }
                                }
                            }

                            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                val url = request?.url?.toString() ?: return false
                                // Intercept portal redirect to capture loginCode
                                if (url.contains("portal.gdust.edu.cn") && url.contains("loginCode=")) {
                                    val loginCode = url.substringAfter("loginCode=").substringBefore("&").substringBefore("#")
                                    if (loginCode.isNotEmpty()) {
                                        onLoginSuccess(loginCode)
                                        return true
                                    }
                                }
                                return false
                            }
                        }

                        loadUrl(casLoginUrl)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            if (isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }
    }
}
