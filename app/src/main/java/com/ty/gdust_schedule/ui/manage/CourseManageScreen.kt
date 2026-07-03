package com.ty.gdust_schedule.ui.manage

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ty.gdust_schedule.R
import com.ty.gdust_schedule.data.Course
import com.ty.gdust_schedule.util.CourseColors

@Composable
fun CourseManageScreen(
    courses: List<Course>,
    colorEngine: Int = 0,
    colorGroupMode: Int = 0,
    onCourseClick: (Course) -> Unit,
    onAddCourse: () -> Unit,
    onDeleteCourse: (Course) -> Unit,
    onDeleteAll: () -> Unit
) {
    var showDeleteAllDialog by remember { mutableStateOf(false) }
    var courseToDelete by remember { mutableStateOf<Course?>(null) }

    val courseGroups = remember(courses) { courses.groupBy { it.name } }
    val uniqueCourses = remember(courseGroups) { courseGroups.values.map { it.first() }.sortedBy { it.name } }

    Box(modifier = Modifier.fillMaxSize()) {
        if (courses.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Schedule, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = stringResource(R.string.no_course_today), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column {
                            Text(stringResource(R.string.course_manage_title), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(stringResource(R.string.course_count_format, courses.size), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (courses.isNotEmpty()) {
                            IconButton(onClick = { showDeleteAllDialog = true }) {
                                Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }

                items(uniqueCourses) { course ->
                    val count = courseGroups[course.name].orEmpty().size
                    CourseListItem(
                        course = course,
                        instanceCount = count,
                        colorEngine = colorEngine,
                        colorGroupMode = colorGroupMode,
                        onClick = { onCourseClick(course) },
                        onDelete = { courseToDelete = course }
                    )
                }
            }
        }

        FloatingActionButton(
            onClick = onAddCourse,
            containerColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
        ) {
            Icon(Icons.Default.Add, stringResource(R.string.add_course))
        }
    }

    courseToDelete?.let { course ->
        AlertDialog(
            onDismissRequest = { courseToDelete = null },
            title = { Text(stringResource(R.string.confirm_delete)) },
            text = { Text(stringResource(R.string.confirm_delete_msg)) },
            confirmButton = { TextButton(onClick = { onDeleteCourse(course); courseToDelete = null }) { Text(stringResource(R.string.delete)) } },
            dismissButton = { TextButton(onClick = { courseToDelete = null }) { Text(stringResource(R.string.cancel)) } }
        )
    }
    if (showDeleteAllDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAllDialog = false },
            title = { Text(stringResource(R.string.confirm_delete_all)) },
            text = { Text(stringResource(R.string.confirm_delete_all_msg)) },
            confirmButton = { TextButton(onClick = { onDeleteAll(); showDeleteAllDialog = false }) { Text(stringResource(R.string.delete)) } },
            dismissButton = { TextButton(onClick = { showDeleteAllDialog = false }) { Text(stringResource(R.string.cancel)) } }
        )
    }
}

@Composable
private fun CourseListItem(
    course: Course,
    instanceCount: Int = 1,
    colorEngine: Int,
    colorGroupMode: Int,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val courseColor = CourseColors.getColor(
        engine = colorEngine,
        groupMode = colorGroupMode,
        courseName = course.name,
        classroom = course.classroom
    )

    com.ty.gdust_schedule.ui.theme.Md3Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        variant = com.ty.gdust_schedule.ui.theme.Md3CardVariant.Elevated,
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(36.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(courseColor.content)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = course.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (instanceCount > 1) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(shape = CircleShape, color = courseColor.container) {
                            Text(
                                text = "${instanceCount}节",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = courseColor.content
                            )
                        }
                    }
                    if (course.isHidden) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ) {
                            Text(
                                text = stringResource(R.string.hidden_tag),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                if (course.classroom.isNotEmpty() || course.teacher.isNotEmpty()) {
                    Text(
                        text = listOfNotNull(
                            course.teacher.ifEmpty { null },
                            course.classroom.ifEmpty { null }
                        ).joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = course.weekRange,
                    style = MaterialTheme.typography.labelSmall,
                    color = courseColor.content.copy(alpha = 0.8f)
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = stringResource(R.string.delete),
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
