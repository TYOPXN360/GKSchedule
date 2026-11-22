package com.ty.gkschedule.ui.theme

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * ponytail: material3 1.5.0-alpha27 删掉了旧的 AlertDialog(icon/title/text/confirmButton) 重载，
 * 只剩 content 版。这里用 BasicAlertDialog 还原同一套参数与原生 M3 外观，调用点不用改写法。
 */
@Composable
fun GkAlertDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: (@Composable () -> Unit)? = null,
    icon: (@Composable () -> Unit)? = null,
    title: (@Composable () -> Unit)? = null,
    text: (@Composable () -> Unit)? = null,
) {
    BasicAlertDialog(onDismissRequest = onDismissRequest, modifier = modifier) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurface,
            tonalElevation = 6.dp,
            modifier = Modifier.widthIn(min = 280.dp, max = 560.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                if (icon != null) {
                    Box(modifier = Modifier.padding(bottom = 16.dp).align(Alignment.CenterHorizontally)) { icon() }
                }
                if (title != null) {
                    Box(modifier = Modifier.padding(bottom = if (text != null) 16.dp else 0.dp)) { title() }
                }
                if (text != null) { text() }
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    dismissButton?.invoke()
                    Spacer(modifier = Modifier.width(8.dp))
                    confirmButton()
                }
            }
        }
    }
}