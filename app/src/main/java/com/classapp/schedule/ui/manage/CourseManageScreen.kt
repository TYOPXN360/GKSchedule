package com.classapp.schedule.ui.manage

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.classapp.schedule.R
import com.classapp.schedule.data.Course
import com.classapp.schedule.util.CourseColors

@Composable
fun CourseManageScreen(
    courses: List<Course>,
    onCourseClick: (Course) -> Unit,
    onAddCourse: () -> Unit,
    onDeleteCourse: (Course) -> Unit,
    onDeleteAll: () -> Unit
) {
    var showDeleteAllDialog by remember { mutableStateOf(false) }
    var courseToDelete by remember { mutableStateOf<Course?>(null) }

    // Group courses by name
    val groupedCourses = remember(courses) {
        courses.groupBy { it.name }.toSortedMap()
    }

    val dayNames = listOf(
        "", stringResource(R.string.mon), stringResource(R.string.tue),
        stringResource(R.string.wed), stringResource(R.string.thu),
        stringResource(R.string.fri), stringResource(R.string.sat),
        stringResource(R.string.sun)
    )

    Scaffold(
        contentWindowInsets = WindowInsets.systemBars,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddCourse,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, stringResource(R.string.add_course))
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.course_manage_title),
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Text(
                        text = stringResource(R.string.course_count_format, courses.size),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (courses.isNotEmpty()) {
                    IconButton(onClick = { showDeleteAllDialog = true }) {
                        Icon(
                            Icons.Default.DeleteSweep,
                            contentDescription = stringResource(R.string.delete_all_courses),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            if (courses.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Schedule, contentDescription = "暂无课程",
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.no_course_today),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    groupedCourses.forEach { (name, courseList) ->
                        item {
                            Text(
                                text = name,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                        items(courseList) { course ->
                            CourseListItem(
                                course = course,
                                dayName = dayNames[course.dayOfWeek],
                                onClick = { onCourseClick(course) },
                                onDelete = { courseToDelete = course }
                            )
                        }
                    }
                }
            }
        }
    }

    // Delete single course dialog
    courseToDelete?.let { course ->
        AlertDialog(
            onDismissRequest = { courseToDelete = null },
            title = { Text(stringResource(R.string.confirm_delete)) },
            text = { Text(stringResource(R.string.confirm_delete_msg)) },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteCourse(course)
                    courseToDelete = null
                }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { courseToDelete = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // Delete all dialog
    if (showDeleteAllDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAllDialog = false },
            title = { Text(stringResource(R.string.confirm_delete_all)) },
            text = { Text(stringResource(R.string.confirm_delete_all_msg)) },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteAll()
                    showDeleteAllDialog = false
                }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAllDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun CourseListItem(
    course: Course,
    dayName: String?,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = MaterialTheme.shapes.small,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.primary)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "$dayName  ${course.startPeriod}-${course.endPeriod()}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
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
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete, contentDescription = stringResource(R.string.delete),
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
