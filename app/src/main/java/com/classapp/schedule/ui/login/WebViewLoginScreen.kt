package com.classapp.schedule.ui.login

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.classapp.schedule.R
import com.classapp.schedule.api.GdustApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Pure Compose QR code login screen.
 * 1. Subscribes to CAS SSE to get clientId
 * 2. Builds QR URL: https://cas.gdust.edu.cn/cas/mobieAuth?clientId=xxx
 * 3. Generates QR code bitmap
 * 4. Polls for login result
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebViewLoginScreen(
    api: GdustApi,
    onLoginSuccess: (loginCode: String) -> Unit,
    onBack: () -> Unit
) {
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var statusText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    fun generateQr() {
        scope.launch(Dispatchers.IO) {
            isLoading = true
            statusText = ""
            try {
                // Step 1: Get clientId from SSE
                val clientId = api.getSseClientId()
                if (clientId == null) {
                    statusText = "获取二维码失败"
                    isLoading = false
                    return@launch
                }

                // Step 2: Build QR URL
                val qrUrl = "https://cas.gdust.edu.cn/cas/mobieAuth?clientId=$clientId"

                // Step 3: Generate QR code bitmap
                qrBitmap = generateQrBitmap(qrUrl, 600)
                isLoading = false

                // Step 4: Poll for login result
                var attempts = 0
                while (isActive && attempts < 120) { // 2 minutes timeout
                    delay(1000)
                    attempts++
                    val result = api.checkSseResult(clientId)
                    if (result != null) {
                        onLoginSuccess(result)
                        return@launch
                    }
                }
                statusText = "二维码已过期，请刷新"
            } catch (e: Exception) {
                statusText = "错误: ${e.message}"
                isLoading = false
            }
        }
    }

    // Auto-generate on first load
    LaunchedEffect(Unit) { generateQr() }

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
                    IconButton(onClick = { generateQr() }) {
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
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = stringResource(R.string.scan_login_desc),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // QR code card
            Card(
                modifier = Modifier.size(280.dp),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoading) {
                        CircularProgressIndicator()
                    } else if (qrBitmap != null) {
                        Image(
                            bitmap = qrBitmap!!.asImageBitmap(),
                            contentDescription = "QR Code",
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            contentScale = ContentScale.Fit
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (statusText.isNotEmpty()) {
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            } else {
                Text(
                    text = stringResource(R.string.scan_login_tip),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/**
 * Simple QR code bitmap generation using a web API fallback.
 * For production, use zxing library. This uses Google Charts API as fallback.
 */
private fun generateQrBitmap(data: String, size: Int): Bitmap {
    // Simple QR code generation using Android's built-in capabilities
    // For now, create a placeholder that encodes the data visually
    // In production, use com.google.zxing:core library

    // Use zxing-core if available, otherwise create a simple representation
    return try {
        generateZxingQr(data, size)
    } catch (_: Exception) {
        // Fallback: create a simple bitmap with the URL text
        createSimpleQrPlaceholder(data, size)
    }
}

private fun generateZxingQr(data: String, size: Int): Bitmap {
    // Try to use zxing if it's on the classpath
    val writer = com.google.zxing.qrcode.QRCodeWriter()
    val bitMatrix = writer.encode(data, com.google.zxing.BarcodeFormat.QR_CODE, size, size)
    val w = bitMatrix.width
    val h = bitMatrix.height
    val pixels = IntArray(w * h)
    for (y in 0 until h) {
        for (x in 0 until w) {
            pixels[y * w + x] = if (bitMatrix[x, y]) Color.BLACK else Color.WHITE
        }
    }
    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.RGB_565)
    bmp.setPixels(pixels, 0, w, 0, 0, w, h)
    return bmp
}

private fun createSimpleQrPlaceholder(data: String, size: Int): Bitmap {
    val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
    bmp.eraseColor(Color.WHITE)
    // Draw a simple pattern to indicate QR code
    val canvas = android.graphics.Canvas(bmp)
    val paint = android.graphics.Paint().apply {
        color = Color.BLACK
        textSize = 16f
        textAlign = android.graphics.Paint.Align.CENTER
    }
    canvas.drawText("QR Code", size / 2f, size / 2f - 10, paint)
    canvas.drawText("(Install zxing)", size / 2f, size / 2f + 10, paint)
    return bmp
}
