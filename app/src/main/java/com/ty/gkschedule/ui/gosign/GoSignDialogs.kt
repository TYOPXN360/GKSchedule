package com.ty.gkschedule.ui.gosign

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ty.gkschedule.R
import com.ty.gkschedule.api.ChaoxingApi
import com.ty.gkschedule.ui.theme.GkAlertDialog

/**
 * ponytail: 一门本地课可以绑多门超星课——设置页/课程编辑页共用的多选对话框。
 * 候选来自上次拉取的全量课程；确认后由调用方写入绑定并同步主 ID。
 */
@Composable
fun CxCourseMultiSelectDialog(
    title: String,
    courses: List<ChaoxingApi.CxCourse>,
    initiallySelected: Set<String>,
    onConfirm: (List<ChaoxingApi.CxCourse>) -> Unit,
    onDismiss: () -> Unit
) {
    // key 用 classId:courseId，避免同名课程互相顶掉
    var selected by remember(courses, initiallySelected) { mutableStateOf(initiallySelected) }
    GkAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                if (courses.isEmpty()) {
                    Text(
                        stringResource(R.string.go_sign_no_cx_courses),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        stringResource(R.string.go_sign_selected_count, selected.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                courses.forEach { course ->
                    val key = ChaoxingApi.cxKey(course.classId, course.courseId)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selected = if (key in selected) selected - key else selected + key
                            }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = key in selected,
                            onCheckedChange = { checked ->
                                selected = if (checked) selected + key else selected - key
                            }
                        )
                        Column(modifier = Modifier.padding(start = 8.dp)) {
                            Text(course.name, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                "classId=${course.classId} · courseId=${course.courseId}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = selected.isNotEmpty(),
                onClick = {
                    onConfirm(courses.filter { ChaoxingApi.cxKey(it.classId, it.courseId) in selected })
                }
            ) { Text(stringResource(R.string.go_sign_confirm_ok)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.go_sign_cancel)) } }
    )
}

/**
 * 一门课绑了多门超星课时，跳转签到前让用户选一个。
 */
@Composable
fun GoSignTargetPickerDialog(
    courseName: String,
    targets: List<ChaoxingApi.CxTarget>,
    onPick: (ChaoxingApi.CxTarget) -> Unit,
    onDismiss: () -> Unit
) {
    GkAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.go_sign_pick_jump_title), fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                Text(
                    stringResource(R.string.go_sign_pick_jump_body, courseName),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                targets.forEach { target ->
                    TextButton(onClick = { onPick(target) }, modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(target.name, modifier = Modifier.fillMaxWidth())
                            Text(
                                "classId=${target.classId} · courseId=${target.courseId} · fid=${target.fid}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.go_sign_cancel)) } }
    )
}
