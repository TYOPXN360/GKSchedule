package com.ty.gkschedule.ui.exam

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.ty.gkschedule.R

@Composable
internal fun examMethodLabel(method: String): String = when (method) {
    "闭卷" -> stringResource(R.string.exam_method_closed)
    "开卷" -> stringResource(R.string.exam_method_open)
    "开卷(半)" -> stringResource(R.string.exam_method_semi)
    "机考" -> stringResource(R.string.exam_method_computer)
    else -> method
}

@Composable
internal fun examMethodFullLabel(raw: String): String {
    val parts = raw.split(",").filter { it.isNotBlank() }
    return when (parts.size) {
        0 -> ""
        1 -> examMethodLabel(parts[0])
        2 -> examMethodLabel(parts[0]) + ", " + examMethodLabel(parts[1])
        3 -> examMethodLabel(parts[0]) + ", " + examMethodLabel(parts[1]) + ", " + examMethodLabel(parts[2])
        else -> raw
    }
}
