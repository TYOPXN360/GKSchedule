package com.classapp.schedule

import android.app.Application
import android.content.ContentValues
import android.graphics.Bitmap
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.classapp.schedule.api.CourseImporter
import com.classapp.schedule.api.GdustApi
import com.classapp.schedule.data.Course
import com.classapp.schedule.data.CourseDao
import com.classapp.schedule.data.CourseDatabase
import com.classapp.schedule.data.SettingsDataStore
import com.classapp.schedule.notification.ReminderScheduler
import com.classapp.schedule.util.IcsExport
import com.classapp.schedule.util.JsonImportExport
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate

class ScheduleViewModel(application: Application) : AndroidViewModel(application) {

    private val courseDao: CourseDao = CourseDatabase.getDatabase(application).courseDao()
    private val settings = SettingsDataStore(application)

    val courses: Flow<List<Course>> = courseDao.getAllCourses()
    val currentWeek: Flow<Int> = settings.getCurrentWeek()
    val totalWeeks: Flow<Int> = settings.totalWeeks
    val periodsPerDay: Flow<Int> = settings.periodsPerDay
    val semesterStart: Flow<LocalDate> = settings.semesterStart
    val darkMode: Flow<String> = settings.darkMode
    val language: Flow<String> = settings.language
    val firstDayOfWeek: Flow<Int> = settings.firstDayOfWeek
    val gridHeight: Flow<Int> = settings.gridHeight
    val gridCorner: Flow<Int> = settings.gridCorner
    val gridSpacing: Flow<Int> = settings.gridSpacing
    val showPeriodLabel: Flow<Boolean> = settings.showPeriodLabel
    val autoGridHeight: Flow<Boolean> = settings.autoGridHeight
    val mergeConsecutive: Flow<Boolean> = settings.mergeConsecutive
    val showTimeLabel: Flow<Boolean> = settings.showTimeLabel
    val savedRealName: Flow<String> = settings.savedRealName
    val savedDeptName: Flow<String> = settings.savedDeptName
    val detailedSplit: Flow<Boolean> = settings.detailedSplit
    val colorEngine: Flow<Int> = settings.colorEngine
    val colorGroupMode: Flow<Int> = settings.colorGroupMode
    val themeColorIndex: Flow<Int> = settings.themeColorIndex
    val reminderMinutes: Flow<Int> = settings.reminderMinutes
    val courseNames: Flow<List<String>> = courseDao.getAllCourseNames()

    private val _selectedWeek = MutableStateFlow(0)
    val selectedWeek: StateFlow<Int> = _selectedWeek

    // Login state
    private val api = GdustApi()
    private val _loginState = MutableStateFlow<LoginState>(LoginState.LoggedOut)
    val loginState: StateFlow<LoginState> = _loginState
    private val _captchaImage = MutableStateFlow<String?>(null)
    val captchaImage: StateFlow<String?> = _captchaImage
    private var captchaUuid = ""
    private var savedStudentId = ""
    private val _savedStudentIdFlow = MutableStateFlow("")
    val savedStudentIdFlow: StateFlow<String> = _savedStudentIdFlow

    // Loading state for refresh
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing

    init {
        viewModelScope.launch {
            _selectedWeek.value = settings.getCurrentWeek().first()
        }
        // Restore saved login info
        viewModelScope.launch {
            val token = settings.savedToken.first()
            val sid = settings.savedStudentId.first()
            val name = settings.savedRealName.first()
            android.util.Log.d("GdustApi", "restore: token=${token.take(20)}..., sid=$sid, name=$name")
            if (token.isNotEmpty() && sid.isNotEmpty()) {
                api.setToken(token)
                savedStudentId = sid
                _savedStudentIdFlow.value = sid
                _loginState.value = LoginState.Success(name.ifEmpty { sid }, sid)
                android.util.Log.d("GdustApi", "restore: login restored, hasToken=${api.hasToken()}")
                // Auto-refresh on app start
                refreshFromSchool()
            } else {
                android.util.Log.d("GdustApi", "restore: no saved login found")
            }
        }
        // Schedule reminders when courses or settings change
        viewModelScope.launch {
            combine(courses, settings.reminderMinutes, settings.semesterStart) { courseList, minutes, start ->
                Triple(courseList, minutes, start)
            }.collect { (courseList, minutes, start) ->
                if (minutes > 0) {
                    ReminderScheduler.scheduleDailyReminders(
                        getApplication(), courseList, start, minutes, ::getStartTime
                    )
                }
            }
        }
    }

    fun setWeek(week: Int) { _selectedWeek.value = week }

    fun getStartTime(period: Int): String = settings.getStartTime(period)
    fun getEndTime(period: Int): String = settings.getEndTime(period)

    suspend fun getCourseById(id: Long): Course? = courseDao.getCourseById(id)

    fun saveCourse(course: Course) {
        viewModelScope.launch {
            if (course.id == 0L) courseDao.insertCourse(course)
            else courseDao.updateCourse(course)
        }
    }

    fun deleteCourse(course: Course) {
        viewModelScope.launch { courseDao.deleteCourse(course) }
    }

    fun deleteCourseById(id: Long) {
        viewModelScope.launch { courseDao.deleteCourseById(id) }
    }

    fun deleteAllCourses() {
        viewModelScope.launch { courseDao.deleteAllCourses() }
    }

    // Settings setters
    fun setSemesterStart(date: LocalDate) { viewModelScope.launch { settings.setSemesterStart(date) } }
    fun setTotalWeeks(weeks: Int) { viewModelScope.launch { settings.setTotalWeeks(weeks) } }
    fun setPeriodsPerDay(periods: Int) { viewModelScope.launch { settings.setPeriodsPerDay(periods) } }
    fun setDarkMode(mode: String) { viewModelScope.launch { settings.setDarkMode(mode) } }
    fun setLanguage(lang: String) { viewModelScope.launch { settings.setLanguage(lang) } }
    fun setFirstDayOfWeek(day: Int) { viewModelScope.launch { settings.setFirstDayOfWeek(day) } }
    fun setGridHeight(h: Int) { viewModelScope.launch { settings.setGridHeight(h) } }
    fun setGridCorner(c: Int) { viewModelScope.launch { settings.setGridCorner(c) } }
    fun setGridSpacing(s: Int) { viewModelScope.launch { settings.setGridSpacing(s) } }
    fun setShowPeriodLabel(show: Boolean) { viewModelScope.launch { settings.setShowPeriodLabel(show) } }
    fun setAutoGridHeight(auto: Boolean) { viewModelScope.launch { settings.setAutoGridHeight(auto) } }
    fun setMergeConsecutive(merge: Boolean) { viewModelScope.launch { settings.setMergeConsecutive(merge) } }
    fun setShowTimeLabel(show: Boolean) { viewModelScope.launch { settings.setShowTimeLabel(show) } }
    fun setDetailedSplit(split: Boolean) { viewModelScope.launch { settings.setDetailedSplit(split) } }
    fun setColorEngine(engine: Int) { viewModelScope.launch { settings.setColorEngine(engine) } }
    fun setColorGroupMode(mode: Int) { viewModelScope.launch { settings.setColorGroupMode(mode) } }
    fun setThemeColorIndex(idx: Int) { viewModelScope.launch { settings.setThemeColorIndex(idx) } }
    fun setReminderMinutes(min: Int) { viewModelScope.launch { settings.setReminderMinutes(min) } }

    // Export - suspend-based for proper data access
    suspend fun exportIcs(): String? {
        val allCourses = courseDao.getAllCourses().first()
        if (allCourses.isEmpty()) return null
        val start = settings.semesterStart.first()
        return IcsExport.exportToIcs(allCourses, start, ::getStartTime, ::getEndTime)
    }

    fun saveIcsToDownload(icsContent: String): Boolean {
        return try {
            val context = getApplication<android.app.Application>()
            val fileName = "schedule_export.ics"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                    put(MediaStore.Downloads.MIME_TYPE, "text/calendar")
                    put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                uri?.let { context.contentResolver.openOutputStream(it)?.use { os -> os.write(icsContent.toByteArray()) } }
            } else {
                val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                File(dir, fileName).writeText(icsContent)
            }
            true
        } catch (_: Exception) { false }
    }

    fun saveJsonToDownload(jsonContent: String): Boolean {
        return try {
            val context = getApplication<android.app.Application>()
            val fileName = "schedule_export.json"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                    put(MediaStore.Downloads.MIME_TYPE, "application/json")
                    put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                uri?.let { context.contentResolver.openOutputStream(it)?.use { os -> os.write(jsonContent.toByteArray()) } }
            } else {
                val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                File(dir, fileName).writeText(jsonContent)
            }
            true
        } catch (_: Exception) { false }
    }

    fun importJson(jsonString: String) {
        viewModelScope.launch {
            val courses = JsonImportExport.importFromJson(jsonString)
            courseDao.deleteAllCourses()
            courses.forEach { courseDao.insertCourse(it) }
        }
    }

    // --- Login ---

    fun refreshCaptcha() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            api.getLoginCode().onSuccess { data ->
                val imgBase64 = data.getImageBase64()
                android.util.Log.d("GdustApi", "Captcha UUID: ${data.uuid}")
                android.util.Log.d("GdustApi", "Captcha image length: ${imgBase64.length}")
                android.util.Log.d("GdustApi", "Captcha image starts with: ${imgBase64.take(50)}")
                _captchaImage.value = imgBase64
                captchaUuid = data.resolveUuid()
                if (_loginState.value is LoginState.Error) {
                    _loginState.value = LoginState.LoggedOut
                }
            }.onFailure { e ->
                android.util.Log.e("GdustApi", "Captcha fetch failed: ${e.message}")
                if (_loginState.value !is LoginState.LoggedOut) {
                    _loginState.value = LoginState.Error(e.message ?: "获取验证码失败")
                }
            }
        }
    }

    fun login(studentId: String, password: String, captcha: String) {
        android.util.Log.d("GdustApi", "login() called: sid=$studentId, captcha=$captcha, uuid=$captchaUuid")
        _loginState.value = LoginState.Loading
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            android.util.Log.d("GdustApi", "login: starting API call on IO thread")
            api.login(studentId, password, captcha, captchaUuid)
                .onSuccess { user ->
                    android.util.Log.d("GdustApi", "login SUCCESS: ${user.realName}, id=${user.id}")
                    savedStudentId = studentId
                    _savedStudentIdFlow.value = studentId
                    _loginState.value = LoginState.Success(user.realName.ifEmpty { studentId }, studentId)
                    // Fetch user info for department
                    try {
                        val userInfo = api.getUserInfo().getOrNull()
                        settings.saveLoginInfo(user.token, studentId, user.realName, userInfo?.deptName ?: "")
                    } catch (_: Exception) {
                        settings.saveLoginInfo(user.token, studentId, user.realName)
                    }
                    importFromSchool(studentId)
                }
                .onFailure { e ->
                    android.util.Log.e("GdustApi", "login FAILED: ${e.message}", e)
                    _loginState.value = LoginState.Error(e.message ?: "登录失败")
                    refreshCaptcha()
                }
        }
    }

    private suspend fun importFromSchool(jobNumber: String) {
        try {
            android.util.Log.d("GdustApi", "importFromSchool: fetching courses for $jobNumber")

            // Get semester info first
            val calendar = api.getSchoolCalendar(jobNumber).getOrNull()
            if (calendar != null && calendar.startTime.isNotEmpty()) {
                settings.setSemesterStart(LocalDate.parse(calendar.startTime))
                if (calendar.allWeek > 0) settings.setTotalWeeks(calendar.allWeek)
            }
            val weeks = calendar?.allWeek ?: 20

            // Fetch courses for ALL weeks
            val allRemoteCourses = mutableListOf<com.classapp.schedule.api.RemoteCourse>()
            for (week in 1..weeks) {
                try {
                    val weekCourses = api.getStudentCourse(jobNumber, week = "$week", year = calendar?.year ?: "", semester = calendar?.semester ?: "").getOrThrow()
                    allRemoteCourses.addAll(weekCourses)
                } catch (e: Exception) {
                    android.util.Log.w("GdustApi", "Failed to fetch week $week: ${e.message}")
                }
            }
            android.util.Log.d("GdustApi", "importFromSchool: got ${allRemoteCourses.size} total courses across $weeks weeks")

            if (allRemoteCourses.isEmpty()) {
                _loginState.value = LoginState.ImportResult(0)
                return
            }

            // Convert and save
            val courses = CourseImporter.convertRemoteCourses(allRemoteCourses)
            courseDao.deleteAllCourses()
            courses.forEach { courseDao.insertCourse(it) }

            _loginState.value = LoginState.ImportResult(courses.size)
        } catch (e: Exception) {
            _loginState.value = LoginState.Error("导入失败: ${e.message}")
        }
    }

    // Public refresh — re-fetch courses using saved token
    fun refreshFromSchool() {
        if (!api.hasToken() || savedStudentId.isEmpty()) return
        _isRefreshing.value = true
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val calendar = api.getSchoolCalendar(savedStudentId).getOrNull()
                val year = calendar?.year ?: ""
                val semester = calendar?.semester ?: ""
                val weeks = calendar?.allWeek ?: totalWeeks.first()

                val remoteCourses = mutableListOf<com.classapp.schedule.api.RemoteCourse>()
                for (week in 1..weeks) {
                    try {
                        val result = api.getStudentCourse(savedStudentId, week = "$week", year = year, semester = semester)
                        result.onSuccess { list ->
                            remoteCourses.addAll(list)
                        }.onFailure { e ->
                            if (isTokenExpired(e.message)) {
                                handleTokenExpired()
                                return@launch
                            }
                        }
                    } catch (e: Exception) {
                        if (isTokenExpired(e.message)) {
                            handleTokenExpired()
                            return@launch
                        }
                    }
                }
                if (remoteCourses.isNotEmpty()) {
                    val courses = CourseImporter.convertRemoteCourses(remoteCourses)
                    courseDao.deleteAllCourses()
                    courses.forEach { courseDao.insertCourse(it) }
                }
            } catch (e: Exception) {
                if (isTokenExpired(e.message)) {
                    handleTokenExpired()
                }
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    private fun isTokenExpired(message: String?): Boolean {
        return message?.contains("离线") == true || message?.contains("重新登录") == true || message?.contains("token", ignoreCase = true) == true
    }

    private suspend fun handleTokenExpired() {
        android.util.Log.w("GdustApi", "Token expired, logging out")
        _isRefreshing.value = false
        savedStudentId = ""
        _savedStudentIdFlow.value = ""
        api.setToken("")
        settings.clearLoginInfo()
        _loginState.value = LoginState.Error("登录已过期，请重新登录")
    }

    fun logout() {
        _loginState.value = LoginState.LoggedOut
        _captchaImage.value = null
        captchaUuid = ""
        savedStudentId = ""
        _savedStudentIdFlow.value = ""
        api.setToken("")
        viewModelScope.launch { settings.clearLoginInfo() }
    }

    fun clearLoginError() {
        _loginState.value = LoginState.LoggedOut
    }
}

sealed class LoginState {
    data object LoggedOut : LoginState()
    data object Loading : LoginState()
    data class Success(val name: String, val studentId: String) : LoginState()
    data class Error(val message: String) : LoginState()
    data class ImportResult(val count: Int) : LoginState()
}
