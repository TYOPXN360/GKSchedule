package com.ty.gkschedule.ui.about

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ty.gkschedule.R
import com.ty.gkschedule.ui.theme.Md3Card
import com.ty.gkschedule.ui.theme.Md3CardVariant
import com.ty.gkschedule.util.UpdateChecker
import com.ty.gkschedule.util.UpdateInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutDetailPage(
    onBack: () -> Unit,
    blurEnabled: Boolean = false,
    autoShowUpdateDialog: Boolean = false,
    preloadedUpdateInfo: UpdateInfo? = null
) {
    val context = LocalContext.current
    var showDisclaimerDialog by remember { mutableStateOf(false) }
    var showUpdateDialog by remember { mutableStateOf(false) }
    var updateInfo by remember { mutableStateOf<UpdateInfo?>(null) }
    var isCheckingUpdate by remember { mutableStateOf(false) }
    var updateError by remember { mutableStateOf<String?>(null) }

    // Force update: 1 min 内点击超过 3 次强制弹窗
    var clickCount by remember { mutableIntStateOf(0) }
    var firstClickTime by remember { mutableLongStateOf(0L) }
    var forceShowDialog by remember { mutableStateOf(false) }

    val currentVersion = remember { UpdateChecker.getCurrentVersion(context) }

    // ponytail: 通知进页自动弹更新框——复用现有showUpdateDialog
    LaunchedEffect(autoShowUpdateDialog, preloadedUpdateInfo) {
        if (autoShowUpdateDialog && preloadedUpdateInfo != null) {
            updateInfo = preloadedUpdateInfo
            showUpdateDialog = true
        }
    }

    // ponytail: miuix源层
    val backdrop = top.yukonga.miuix.kmp.blur.rememberLayerBackdrop()
    // ponytail: 底色与主App同值——暗surface/亮surfaceContainer（硬编码surfaceContainer暗色偏亮一档）
    val isDark = com.ty.gkschedule.ui.theme.LocalAppIsDark.current
    val pageBg = if (isDark) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceContainer
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = pageBg,
        topBar = {
            com.ty.gkschedule.ui.theme.BlurTopBar(
                title = { Text(stringResource(R.string.about_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                },
                backdrop = backdrop,
                blurEnabled = blurEnabled
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(backdrop)
                .background(pageBg)
                                // ponytail: 避让走滚动内padding，源纹理全屏录(含顶栏身后)
                .verticalScroll(rememberScrollState())
                .padding(top = padding.calculateTopPadding()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            // App icon
            Image(
                painter = painterResource(id = R.mipmap.ic_launcher),
                contentDescription = "App Icon",
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(24.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.height(16.dp))

            // App name
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Version
            Text(
                text = "v$currentVersion",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Description
            Text(
                text = stringResource(R.string.about_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Update check section
            Md3Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                variant = Md3CardVariant.Elevated
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Current version
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.update_current_version),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = currentVersion,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Check update button
                    Button(
                        onClick = {
                            if (!isCheckingUpdate) {
                                // Track clicks for force update
                                val now = System.currentTimeMillis()
                                if (now - firstClickTime > 60_000) {
                                    clickCount = 1
                                    firstClickTime = now
                                } else {
                                    clickCount++
                                }
                                // 1 min 内超过 3 次，强制弹窗
                                if (clickCount >= 3) {
                                    clickCount = 0
                                    forceShowDialog = true
                                }

                                isCheckingUpdate = true
                                updateError = null
                                GlobalScope.launch(Dispatchers.IO) {
                                    UpdateChecker.checkForUpdate(context)
                                        .onSuccess { info ->
                                            withContext(Dispatchers.Main) {
                                                updateInfo = info
                                                isCheckingUpdate = false
                                                if (info.isUpdateAvailable || forceShowDialog) {
                                                    showUpdateDialog = true
                                                    forceShowDialog = false
                                                }
                                            }
                                        }
                                        .onFailure { e ->
                                            withContext(Dispatchers.Main) {
                                                updateError = e.message
                                                isCheckingUpdate = false
                                            }
                                        }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isCheckingUpdate
                    ) {
                        if (isCheckingUpdate) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        } else {
                            Icon(Icons.Default.Refresh, null)
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(stringResource(R.string.settings_category_update))
                    }

                    // Error message
                    if (updateError != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.errorContainer
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Error, null, tint = MaterialTheme.colorScheme.error)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = updateError!!,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }

                    // No update available message
                    if (updateInfo != null && !updateInfo!!.isUpdateAvailable && updateError == null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            // ponytail: Lowest暗色近纯黑——换High与Md3Card/Dialog同阶
                            color = MaterialTheme.colorScheme.surfaceContainerHigh
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.update_not_available),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Credits section
            Md3Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                variant = Md3CardVariant.Elevated
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.credits_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    CreditItem(stringResource(R.string.credits_language), "Jetpack Compose (Kotlin)")
                    CreditItem(stringResource(R.string.credits_design), "Material Design 3 Expressive")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // AI Assistants section
            Md3Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                variant = Md3CardVariant.Elevated
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.credits_ai),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    AIItem("GPT-5.5")
                    AIItem("Deepseek-v4-pro")
                    AIItem("XiaoMi-Mimo-V2.5")
                    AIItem("XiaoMi-Mimo-V2.5-pro")
                    AIItem("Google-Gemini-v3.5-flash")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Reference projects section
            Md3Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                variant = Md3CardVariant.Elevated
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.credits_projects),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("• SchedU", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("• 拾光课程表", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("• TimeFlow", style = MaterialTheme.typography.bodyMedium)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Disclaimer button
            OutlinedButton(
                onClick = { showDisclaimerDialog = true },
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Text(stringResource(R.string.disclaimer_title))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Copyright
            Text(
                text = stringResource(R.string.about_copyright),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Disclaimer dialog
    if (showDisclaimerDialog) {
        com.ty.gkschedule.ui.theme.BlurAlertDialog(
            onDismissRequest = { showDisclaimerDialog = false },
            title = { Text(stringResource(R.string.disclaimer_title)) },
            text = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = stringResource(R.string.disclaimer_text),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showDisclaimerDialog = false }) {
                    Text("OK")
                }
            },
            blurEnabled = blurEnabled
        )
    }

    // Update dialog
    if (showUpdateDialog && updateInfo != null) {
        com.ty.gkschedule.ui.theme.BlurAlertDialog(
            onDismissRequest = { showUpdateDialog = false },
            icon = { Icon(Icons.Default.SystemUpdate, null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text(stringResource(R.string.update_available)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "${stringResource(R.string.update_current_version)}: v$currentVersion",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "${stringResource(R.string.update_latest_version)}: v${updateInfo!!.latestVersion}",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    if (updateInfo!!.fileSize > 0) {
                        Text(
                            text = "${stringResource(R.string.update_size)}: ${formatFileSize(updateInfo!!.fileSize)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (updateInfo!!.releaseNotes.isNotEmpty()) {
                        HorizontalDivider()
                        Text(
                            text = updateInfo!!.releaseNotes,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 150.dp)
                                .verticalScroll(rememberScrollState())
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showUpdateDialog = false
                        val url = updateInfo?.downloadUrl.orEmpty()
                        if (url.isNotEmpty()) {
                            // ponytail: 下载走系统DownloadManager（通知栏/断点/完成安装全托管）
                            UpdateChecker.enqueueDownload(
                                context, url,
                                "GKSchedule-v${updateInfo!!.latestVersion}.apk"
                            )
                            android.widget.Toast.makeText(context, "已开始下载，可在通知栏查看进度", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    },
                    enabled = updateInfo?.downloadUrl?.isNotEmpty() == true
                ) {
                    Icon(Icons.Default.Download, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.update_download))
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        showUpdateDialog = false
                        val url = updateInfo?.downloadUrl.orEmpty()
                        if (url.isNotEmpty()) UpdateChecker.openInBrowser(context, url)
                    }) {
                        Text(stringResource(R.string.update_browser))
                    }
                    TextButton(onClick = { showUpdateDialog = false }) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            },
            // ponytail: 更新框固定纯色，不跟毛玻璃开关走半透明
            blurEnabled = false
        )
    }
}

@Composable
private fun CreditItem(label: String, value: String) {
    Row {
        Text(
            text = "$label: ",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
    Spacer(modifier = Modifier.height(8.dp))
}

@Composable
private fun AIItem(name: String) {
    Row(
        modifier = Modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "• ",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = name,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        else -> "${"%.1f".format(bytes / (1024.0 * 1024.0))} MB"
    }
}
