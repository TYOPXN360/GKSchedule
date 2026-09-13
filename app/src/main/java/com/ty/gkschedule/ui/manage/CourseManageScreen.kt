package com.ty.gkschedule.ui.manage

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import com.ty.gkschedule.R
import com.ty.gkschedule.data.Course
import com.ty.gkschedule.util.CourseColors
import top.yukonga.miuix.kmp.blur.blur
import top.yukonga.miuix.kmp.blur.drawBackdrop
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseManageScreen(
    courses: List<Course>,
    colorEngine: Int = 0,
    colorGroupMode: Int = 0,
    blurEnabled: Boolean = true,
    onCourseClick: (Course) -> Unit,
    onAddCourse: () -> Unit,
    onDeleteCourse: (Course) -> Unit,
    onDeleteAll: () -> Unit,
    onBack: (() -> Unit)? = null,
    onScrollHidePill: (Boolean) -> Unit = {}
) {
    var showDeleteAllDialog by remember { mutableStateOf(false) }
    var courseToDelete by remember { mutableStateOf<Course?>(null) }

    val courseGroups = remember(courses) { courses.groupBy { it.name } }
    val uniqueCourses = remember(courseGroups) { courseGroups.values.map { it.first() }.sortedBy { it.name } }

    val listState = rememberLazyListState()
    // ponytail: pill跟滚动方向走——下滑内容(手指上推)立即藏，上滑立即现；顶部不强制现
    var lastOffset by remember { mutableIntStateOf(0) }
    var lastIndex by remember { mutableIntStateOf(0) }
    LaunchedEffect(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset) {
        val idx = listState.firstVisibleItemIndex
        val off = listState.firstVisibleItemScrollOffset
        // 同一item内比offset，跨item按方向：index变大=下滑藏，变小=上滑现
        val scrollingDown = when {
            idx != lastIndex -> idx > lastIndex
            else -> off > lastOffset
        }
        onScrollHidePill(scrollingDown)
        lastOffset = off
        lastIndex = idx
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    // ponytail: miuix源层，顶栏drawBackdrop吃糊
    val backdrop = top.yukonga.miuix.kmp.blur.rememberLayerBackdrop()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        // ponytail: 顶栏自己吃系统栏，内容区不再重复垫（双重Insets留白根因）
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            // ponytail: 展开态大字headineMedium；折叠用alpha/size双插值防下沉跳变；门数常驻小标题
            val fraction = scrollBehavior.state.collapsedFraction
            val titleSize = lerp(
                MaterialTheme.typography.headlineMedium.fontSize,
                MaterialTheme.typography.titleLarge.fontSize,
                fraction
            )
            com.ty.gkschedule.ui.theme.BlurLargeTopBar(
                windowInsets = WindowInsets(0, 0, 0, 0),
                title = {
                    Column {
                        Text(
                            stringResource(R.string.course_manage_title),
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            fontSize = titleSize
                        )
                        Text(
                            stringResource(R.string.course_count_format, courses.size),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                // ponytail: 折叠后门数降对比但不消失
                                alpha = 1f - fraction * 0.3f
                            ),
                            maxLines = 1
                        )
                    }
                },
                actions = {
                    if (courses.isNotEmpty()) {
                        IconButton(onClick = { showDeleteAllDialog = true }) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        }
                    }
                },
                scrollBehavior = scrollBehavior,
                backdrop = backdrop,
                blurEnabled = blurEnabled
            )
        },
        floatingActionButton = {
            // ponytail: clip shape与按钮外轮廓同源——不一致必漏角
            val fabShape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
            FloatingActionButton(
                onClick = onAddCourse,
                shape = fabShape,
                elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp),
                modifier = Modifier.drawBackdrop(
                    backdrop = backdrop,
                    shape = { fabShape },
                    effects = { blur(28.dp.toPx()) }
                ),
                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.75f)
            ) {
                Icon(Icons.Default.Add, stringResource(R.string.add_course))
            }
        }
    ) { padding ->
        // ponytail: miuix源层全屏，避让走contentPadding，item滚动穿过顶栏下方
        val topPad = padding.calculateTopPadding()
        Box(
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(backdrop)
                .background(MaterialTheme.colorScheme.surface)
        ) {
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
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = topPad + 8.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // ponytail: 门数跟标题走了，列表头不再摆第二份
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

    com.ty.gkschedule.ui.theme.Md3Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        variant = com.ty.gkschedule.ui.theme.Md3CardVariant.Elevated,
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
