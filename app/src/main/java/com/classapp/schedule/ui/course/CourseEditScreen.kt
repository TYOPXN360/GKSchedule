package com.classapp.schedule.ui.course

import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.classapp.schedule.R
import com.classapp.schedule.data.Course
import com.classapp.schedule.util.CourseColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseEditScreen(
    course: Course?,
    periodsPerDay: Int,
    onSave: (Course) -> Unit,
    onDelete: (Course) -> Unit,
    onBack: () -> Unit
) {
    val isEditing = course != null
    val context = LocalContext.current

    var name by remember { mutableStateOf(course?.name ?: "") }
    var teacher by remember { mutableStateOf(course?.teacher ?: "") }
    var classroom by remember { mutableStateOf(course?.classroom ?: "") }
    var dayOfWeek by remember { mutableIntStateOf(course?.dayOfWeek ?: 1) }
    var startPeriod by remember { mutableIntStateOf(course?.startPeriod ?: 1) }
    var periods by remember { mutableIntStateOf(course?.periods ?: 1) }
    var colorIndex by remember { mutableIntStateOf(course?.colorIndex ?: 0) }
    var weekRange by remember { mutableStateOf(course?.weekRange ?: "all") }
    var customWeekRange by remember { mutableStateOf(if (course?.weekRange == "all" || course?.weekRange == "odd" || course?.weekRange == "even") "" else (course?.weekRange ?: "")) }
    var remark by remember { mutableStateOf(course?.remark ?: "") }
    var isCustomTime by remember { mutableStateOf(course?.isCustomTime ?: false) }
    var customStartTime by remember { mutableStateOf(course?.customStartTime ?: "08:00") }
    var customEndTime by remember { mutableStateOf(course?.customEndTime ?: "09:00") }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val dayNames = listOf(
        stringResource(R.string.mon), stringResource(R.string.tue),
        stringResource(R.string.wed), stringResource(R.string.thu),
        stringResource(R.string.fri), stringResource(R.string.sat),
        stringResource(R.string.sun)
    )
    val weekRangeOptions = listOf(
        "all" to stringResource(R.string.all_weeks),
        "odd" to stringResource(R.string.odd_weeks),
        "even" to stringResource(R.string.even_weeks),
        "custom" to stringResource(R.string.custom_weeks)
    )

    Scaffold(
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Text(if (isEditing) stringResource(R.string.edit_course) else stringResource(R.string.add_new_course))
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    if (isEditing) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, stringResource(R.string.delete),
                                tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Name
            OutlinedTextField(
                value = name, onValueChange = { name = it },
                label = { Text(stringResource(R.string.course_name)) },
                modifier = Modifier.fillMaxWidth(), singleLine = true
            )
            // Teacher
            OutlinedTextField(
                value = teacher, onValueChange = { teacher = it },
                label = { Text(stringResource(R.string.course_teacher)) },
                modifier = Modifier.fillMaxWidth(), singleLine = true
            )
            // Classroom
            OutlinedTextField(
                value = classroom, onValueChange = { classroom = it },
                label = { Text(stringResource(R.string.course_classroom)) },
                modifier = Modifier.fillMaxWidth(), singleLine = true
            )

            // Day of week
            Text(stringResource(R.string.day_of_week), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                dayNames.forEachIndexed { index, dn ->
                    FilterChip(
                        selected = dayOfWeek == index + 1,
                        onClick = { dayOfWeek = index + 1 },
                        label = { Text(dn, style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Custom time toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(stringResource(R.string.custom_time), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                Switch(checked = isCustomTime, onCheckedChange = { isCustomTime = it })
            }

            if (isCustomTime) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            val parts = customStartTime.split(":")
                            TimePickerDialog(context, { _, h, m ->
                                customStartTime = "%02d:%02d".format(h, m)
                            }, parts[0].toIntOrNull() ?: 8, parts[1].toIntOrNull() ?: 0, true).show()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("${stringResource(R.string.custom_start_time)}: $customStartTime")
                    }
                    OutlinedButton(
                        onClick = {
                            val parts = customEndTime.split(":")
                            TimePickerDialog(context, { _, h, m ->
                                customEndTime = "%02d:%02d".format(h, m)
                            }, parts[0].toIntOrNull() ?: 9, parts[1].toIntOrNull() ?: 0, true).show()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("${stringResource(R.string.custom_end_time)}: $customEndTime")
                    }
                }
            } else {
                // Start period + Duration steppers
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.start_period), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.weight(1f))
                    Stepper(value = startPeriod, min = 1, max = periodsPerDay, onChange = { startPeriod = it })
                }
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.duration), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.weight(1f))
                    Stepper(value = periods, min = 1, max = (periodsPerDay - startPeriod + 1).coerceAtLeast(1), onChange = { periods = it })
                }
            }

            // Color
            Text(stringResource(R.string.course_color), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            val editMonetColors = CourseColors.getColors(0)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                editMonetColors.forEachIndexed { index, (bg, fg) ->
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(fg)
                            .then(if (colorIndex == index) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape) else Modifier)
                            .clickable { colorIndex = index }
                    )
                }
            }

            // Week range
            Text(stringResource(R.string.week_range), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                weekRangeOptions.forEach { (key, label) ->
                    FilterChip(
                        selected = weekRange == key || (key == "custom" && weekRange !in listOf("all", "odd", "even")),
                        onClick = { weekRange = key },
                        label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }
            if (weekRange == "custom") {
                OutlinedTextField(
                    value = customWeekRange, onValueChange = { customWeekRange = it },
                    label = { Text(stringResource(R.string.week_range_hint)) },
                    modifier = Modifier.fillMaxWidth(), singleLine = true
                )
            }

            // Remark
            OutlinedTextField(
                value = remark, onValueChange = { remark = it },
                label = { Text(stringResource(R.string.course_remark)) },
                placeholder = { Text(stringResource(R.string.course_remark_hint)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2, maxLines = 4
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Save
            Button(
                onClick = {
                    val finalWeekRange = when (weekRange) {
                        "all", "odd", "even" -> weekRange
                        else -> customWeekRange.ifEmpty { weekRange }
                    }
                    onSave(Course(
                        id = course?.id ?: 0,
                        name = name.trim(), teacher = teacher.trim(), classroom = classroom.trim(),
                        dayOfWeek = dayOfWeek, startPeriod = startPeriod, periods = periods,
                        colorIndex = colorIndex, weekRange = finalWeekRange,
                        remark = remark.trim(), isCustomTime = isCustomTime,
                        customStartTime = customStartTime, customEndTime = customEndTime
                    ))
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = name.isNotBlank()
            ) { Text(stringResource(R.string.save)) }
        }
    }

    if (showDeleteDialog && course != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.confirm_delete)) },
            text = { Text(stringResource(R.string.confirm_delete_msg)) },
            confirmButton = {
                TextButton(onClick = { onDelete(course); showDeleteDialog = false }) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun Stepper(value: Int, min: Int, max: Int, onChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        IconButton(onClick = { if (value > min) onChange(value - 1) }, enabled = value > min) {
            Text("−", style = MaterialTheme.typography.titleLarge)
        }
        Text("$value", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        IconButton(onClick = { if (value < max) onChange(value + 1) }, enabled = value < max) {
            Text("+", style = MaterialTheme.typography.titleLarge)
        }
    }
}
