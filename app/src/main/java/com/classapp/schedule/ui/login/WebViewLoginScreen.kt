package com.classapp.schedule.ui.login

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.webkit.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.classapp.schedule.R

/**
 * Clean QR code login: WebView with CSS injection to show only the QR code area.
 * Hides all CAS page chrome, keeps only the DingTalk scan tab.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebViewLoginScreen(
    onLoginSuccess: (loginCode: String) -> Unit,
    onBack: () -> Unit
) {
    var isLoading by remember { mutableStateOf(true) }
    var webView by remember { mutableStateOf<WebView?>(null) }

    // CAS login URL with service redirect
    val casLoginUrl = "https://cas.gdust.edu.cn/cas/?service=https%3A%2F%2Fportal.gdust.edu.cn%2Fsmart-admin-api%2Fuser%2FssoLogin%2F%3FappId%3DportalRemote"

    // Aggressive CSS: hide ALL text, show only the QR code image/canvas
    val injectCss = """
        javascript:(function() {
            var s = document.createElement('style');
            s.textContent = `
                * {
                    margin: 0 !important;
                    padding: 0 !important;
                }
                body {
                    background: transparent !important;
                    overflow: hidden !important;
                }
                /* Hide everything by default */
                body > *:not(canvas):not(img):not(iframe) {
                    visibility: hidden !important;
                }
                /* But show containers that hold the QR */
                div, section, article, main {
                    visibility: visible !important;
                }
                /* Hide all text elements */
                p, span, h1, h2, h3, h4, h5, h6, a, li, label, 
                strong, em, small, b, i, button:not([style*="qr"]),
                footer, header, nav {
                    display: none !important;
                }
                /* Make images/canvas large and centered */
                img, canvas {
                    display: block !important;
                    margin: 0 auto !important;
                    max-width: 250px !important;
                    max-height: 250px !important;
                    object-fit: contain !important;
                }
                /* Hide specific CAS elements */
                [class*="header"], [class*="footer"], [class*="nav"],
                [class*="title"], [class*="desc"], [class*="tip"],
                [class*="logo"], [class*="copyright"], [class*="link"],
                [class*="tab"], [class*="switch"], [class*="form"],
                [id*="header"], [id*="footer"], [id*="nav"],
                [id*="title"], [id*="tab"] {
                    display: none !important;
                }
                /* Show QR containers */
                [class*="qr"], [class*="QR"], [id*="qr"], [id*="QR"],
                [class*="scan"], [class*="code"], [id*="code"],
                iframe[src*="dingtalk"], iframe[src*="ding"] {
                    display: block !important;
                    visibility: visible !important;
                }
            `;
            document.head.appendChild(s);
            // Also try to remove non-QR text nodes
            document.querySelectorAll('p, span, h1, h2, h3, h4, h5, h6, label, strong, em').forEach(function(el) {
                if (!el.querySelector('canvas') && !el.querySelector('img[src*="qr"]')) {
                    el.style.display = 'none';
                }
            });
        })();
    """.trimIndent()

    Scaffold(
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.scan_login_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { webView?.reload() }) {
                        Icon(Icons.Default.Refresh, "Refresh")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.scan_login_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            // QR code WebView — styled to show only the QR
            Card(
                modifier = Modifier.size(300.dp),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    AndroidView(
                        factory = { context ->
                            WebView(context).apply {
                                webView = this
                                settings.apply {
                                    javaScriptEnabled = true
                                    domStorageEnabled = true
                                    databaseEnabled = true
                                    setSupportZoom(false)
                                    builtInZoomControls = false
                                    displayZoomControls = false
                                    mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                    userAgentString = "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/120.0.0.0 Mobile Safari/537.36"
                                    loadWithOverviewMode = true
                                    useWideViewPort = true
                                }
                                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                                webViewClient = object : WebViewClient() {
                                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                        isLoading = true
                                    }
                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        isLoading = false
                                        // Inject CSS to hide everything except QR
                                        evaluateJavascript(injectCss, null)
                                        // Check for successful login redirect
                                        if (url != null && url.contains("portal.gdust.edu.cn") && url.contains("loginCode=")) {
                                            val loginCode = url.substringAfter("loginCode=").substringBefore("&").substringBefore("#")
                                            if (loginCode.isNotEmpty()) onLoginSuccess(loginCode)
                                        }
                                    }
                                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                        val url = request?.url?.toString() ?: return false
                                        if (url.contains("portal.gdust.edu.cn") && url.contains("loginCode=")) {
                                            val loginCode = url.substringAfter("loginCode=").substringBefore("&").substringBefore("#")
                                            if (loginCode.isNotEmpty()) { onLoginSuccess(loginCode); return true }
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
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.scan_login_tip),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
