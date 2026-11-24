package com.ty.gkschedule.ui.theme

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ty.gkschedule.CaptchaState
import com.ty.gkschedule.R

/**
 * ponytail: 验证码图 + 状态（加载中/加载失败）共用组件——登录页与快捷重登录弹窗同一套表现，
 * 免得三处各写一遍状态分支。点击可重新拉取。
 */
@Composable
fun CaptchaImageWithStatus(
    captchaImageBase64: String?,
    captchaState: CaptchaState,
    onRefreshCaptcha: () -> Unit,
    modifier: Modifier = Modifier,
    width: Int = 120,
    height: Int = 56,
) {
    val bitmap = remember(captchaImageBase64) {
        captchaImageBase64?.takeIf { it.isNotEmpty() }?.let {
            runCatching {
                val bytes = Base64.decode(it, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            }.getOrNull()
        }
    }
    val boxModifier = modifier
        .size(width = width.dp, height = height.dp)
        .clip(MaterialTheme.shapes.extraSmall)

    if (bitmap != null) {
        Card(
            modifier = boxModifier.clickable { onRefreshCaptcha() },
            shape = MaterialTheme.shapes.extraSmall
        ) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = stringResource(R.string.login_captcha),
                modifier = Modifier.size(width = width.dp, height = height.dp),
                contentScale = ContentScale.Fit
            )
        }
    } else {
        Box(
            modifier = boxModifier
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .clickable(enabled = captchaState !is CaptchaState.Loading) { onRefreshCaptcha() },
            contentAlignment = Alignment.Center
        ) {
            when (captchaState) {
                is CaptchaState.Loading -> CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    strokeWidth = 2.dp
                )
                is CaptchaState.Failed -> Text(
                    text = stringResource(R.string.captcha_load_failed),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
                else -> Text(
                    text = stringResource(R.string.captcha_loading),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/** 验证码状态的一行说明；成功时不出行。 */
@Composable
fun CaptchaStatusLine(
    captchaState: CaptchaState,
    modifier: Modifier = Modifier,
) {
    when (captchaState) {
        is CaptchaState.Loading -> Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.captcha_loading),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        is CaptchaState.Failed -> Text(
            modifier = modifier,
            text = stringResource(R.string.captcha_load_failed_fmt, captchaState.message),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error
        )
        else -> Unit
    }
}