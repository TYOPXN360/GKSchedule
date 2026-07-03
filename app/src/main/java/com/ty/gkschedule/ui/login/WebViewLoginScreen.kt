package com.ty.gkschedule.ui.login

import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ty.gkschedule.R
import com.ty.gkschedule.api.GdustApi
import com.ty.gkschedule.util.HapticFeedback
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebViewLoginScreen(
    api: GdustApi,
    onLoginSuccess: (loginCode: String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var statusText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var isExpired by remember { mutableStateOf(false) }

    fun generateQr() {
        scope.launch(Dispatchers.IO) {
            isLoading = true
            isExpired = false
            statusText = ""
            try {
                // Phase 1: Get clientId from SSE (blocks until first event)
                val (clientId, stream) = api.openSseConnection()
                if (clientId == null || stream == null) {
                    statusText = context.getString(R.string.qr_fetch_failed)
                    isLoading = false
                    return@launch
                }

                // Phase 2: Generate QR code
                val qrUrl = "https://cas.gdust.edu.cn/cas/mobieAuth?clientId=$clientId"
                qrBitmap = generateZxingQr(qrUrl, 600)
                isLoading = false

                // Phase 3: Keep reading SSE for scan result (blocks until scan or timeout)
                val result = api.readSseResult(stream)
                if (result != null) {
                    onLoginSuccess(result)
                } else {
                    isExpired = true
                }
            } catch (e: Exception) {
                statusText = e.message ?: "Error"
                isLoading = false
            }
        }
    }

    fun saveQrToGallery() {
        val bmp = qrBitmap ?: return
        scope.launch(Dispatchers.IO) {
            try {
                val fileName = "qr_login.png"
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    // Delete old file first
                    val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                    context.contentResolver.delete(collection, "${MediaStore.Images.Media.DISPLAY_NAME} = ?", arrayOf(fileName))
                    val values = ContentValues().apply {
                        put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                        put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                        put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Screenshots")
                    }
                    val uri = context.contentResolver.insert(collection, values)
                    uri?.let { context.contentResolver.openOutputStream(it)?.use { os -> bmp.compress(Bitmap.CompressFormat.PNG, 100, os) } }
                } else {
                    @Suppress("DEPRECATION")
                    val dir = java.io.File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "Screenshots")
                    dir.mkdirs()
                    val file = java.io.File(dir, fileName)
                    if (file.exists()) file.delete()
                    file.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
                }
                // Toast on main thread
                kotlinx.coroutines.withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(context, context.getString(R.string.qr_saved), android.widget.Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                kotlinx.coroutines.withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(context, "Save failed: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Auto-generate on first load
    LaunchedEffect(Unit) { generateQr() }

    Scaffold(
        contentWindowInsets = WindowInsets.systemBars,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.scan_login_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
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

            // QR code card with expired overlay
            Card(
                modifier = Modifier.size(280.dp),
                shape = MaterialTheme.shapes.extraLarge,
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
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
                            modifier = Modifier.fillMaxSize().padding(24.dp),
                            contentScale = ContentScale.Fit
                        )
                    }

                    // Expired overlay
                    if (isExpired) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.6f))
                                .clickable {
                                    HapticFeedback.light(context)
                                    generateQr()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(40.dp),
                                    tint = MaterialTheme.colorScheme.inverseOnSurface
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = stringResource(R.string.qr_expired),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.inverseOnSurface
                                )
                                Text(
                                    text = stringResource(R.string.qr_tap_to_refresh),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action buttons: Save + Refresh
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = {
                        HapticFeedback.medium(context)
                        saveQrToGallery()
                    },
                    modifier = Modifier.weight(1f),
                    enabled = qrBitmap != null && !isLoading
                ) {
                    Icon(Icons.Default.Save, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.save_qr))
                }
                FilledTonalButton(
                    onClick = {
                        HapticFeedback.medium(context)
                        generateQr()
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !isLoading
                ) {
                    Icon(Icons.Default.Refresh, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.refresh_qr))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Status text
            if (statusText.isNotEmpty()) {
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            } else if (!isExpired) {
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

private fun generateZxingQr(data: String, size: Int): Bitmap {
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
