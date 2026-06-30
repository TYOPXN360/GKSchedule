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
import com.classapp.schedule.data.CredentialStore
import com.classapp.schedule.data.SettingsDataStore
import com.classapp.schedule.notification.ReminderScheduler
import com.classapp.schedule.util.IcsExport
import com.classapp.schedule.util.ImageExport
import com.classapp.schedule.util.JsonImportExport
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate

class ScheduleViewModel(application: Application) : AndroidViewModel(application) {

    private val courseDao: CourseDao = CourseDatabase.getDatabase(application).courseDao()
    private val settings = SettingsDataStore(application)
    private val app = application
    val api = GdustApi() // Public for WebView login

    // Credential store
    val hasSavedCredentials: Flow<Boolean> = CredentialStore.hasCredentials(application)

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
    val showDateInHeader: Flow<Boolean> = settings.showDateInHeader
    val hideEmptyWeeks: Flow<Boolean> = settings.hideEmptyWeeks
    val themeColorIndex: Flow<Int> = settings.themeColorIndex
    val reminderMinutes: Flow<Int> = settings.reminderMinutes
    val autoSyncOnStart: Flow<Boolean> = settings.autoSyncOnStart
    val autoSyncIntervalValue: Flow<Int> = settings.autoSyncIntervalValue
    val autoSyncIntervalUnit: Flow<String> = settings.autoSyncIntervalUnit
    val tokenHeartbeat: Flow<Boolean> = settings.tokenHeartbeat
    val showExamSchedule: Flow<Boolean> = settings.showExamSchedule
    val examLookaheadWeeks: Flow<Int> = settings.examLookaheadWeeks
    val courseNames: Flow<List<String>> = courseDao.getAllCourseNames()

    private val _selectedWeek = MutableStateFlow(0)
    val selectedWeek: StateFlow<Int> = _selectedWeek

    // Login state
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

    // One-shot messages for snackbar
    private val _messages = MutableSharedFlow<String>()
    val messages: SharedFlow<String> = _messages

    init {
        viewModelScope.launch {
            _selectedWeek.value = settings.getCurrentWeek().first()
        }
        // Restore saved login info
        viewModelScope.launch {
            val token = settings.savedToken.first()
            val sid = settings.savedStudentId.first()
            val name = settings.savedRealName.first()
            val expired = settings.tokenExpired.first()
            android.util.Log.d("GdustApi", "restore: token=${token.take(20)}..., sid=$sid, name=$name, expired=$expired")
            if (expired && sid.isNotEmpty()) {
                savedStudentId = sid
                _savedStudentIdFlow.value = sid
                _loginState.value = LoginState.TokenExpired
                _messages.emit("登录已过期，请快速登录")
            } else if (token.isNotEmpty() && sid.isNotEmpty()) {
                api.setToken(token)
                savedStudentId = sid
                _savedStudentIdFlow.value = sid
                _loginState.value = LoginState.Success(name.ifEmpty { sid }, sid)
                // Restore CAS ticket for exam schedule
                val ticket = settings.casTicket.first()
                if (ticket.isNotEmpty()) api.setCasTicket(ticket)
                android.util.Log.d("GdustApi", "restore: login restored, hasToken=${api.hasToken()}, hasTicket=${ticket.isNotEmpty()}")
                // Load cached exams
                val cachedJson = settings.cachedExams.first()
                if (cachedJson.isNotEmpty()) {
                    try {
                        _examList.value = kotlinx.serialization.json.Json.decodeFromString(cachedJson)
                        _examYear.value = settings.cachedExamYear.first()
                        _examSemester.value = settings.cachedExamSemester.first()
                    } catch (_: Exception) {}
                }
                // Auto-refresh on app start if enabled
                val syncOnStart = settings.autoSyncOnStart.first()
                if (syncOnStart) refreshFromSchool()
            } else {
                android.util.Log.d("GdustApi", "restore: no saved login found")
            }
        }
        // Mirror token-expired changes written by background workers into UI state.
        viewModelScope.launch {
            settings.tokenExpired.collect { expired ->
                if (expired && _loginState.value !is LoginState.TokenExpired) {
                    val sid = settings.savedStudentId.first()
                    if (sid.isNotEmpty()) {
                        savedStudentId = sid
                        _savedStudentIdFlow.value = sid
                    }
                    api.setToken("")
                    _loginState.value = LoginState.TokenExpired
                    _messages.emit("登录已过期，请快速登录")
                }
            }
        }
        // Schedule reminders when courses or settings change
        viewModelScope.launch {
            combine(courses, settings.reminderMinutes, settings.semesterStart) { courseList, minutes, start ->
                Triple(courseList, minutes, start)
            }.collect { (courseList, minutes, start) ->
                if (minutes > 0 && canScheduleExactAlarms()) {
                    ReminderScheduler.scheduleDailyReminders(
                        getApplication(), courseList, start, minutes, ::getStartTime
                    )
                }
            }
        }
        // Schedule periodic auto-sync
        viewModelScope.launch {
            val value = settings.autoSyncIntervalValue.first()
            val unit = settings.autoSyncIntervalUnit.first()
            com.classapp.schedule.sync.AutoSyncWorker.schedule(app, value, unit)
        }
        // Schedule token heartbeat if enabled
        viewModelScope.launch {
            val heartbeatEnabled = settings.tokenHeartbeat.first()
            if (heartbeatEnabled) {
                com.classapp.schedule.sync.HeartbeatWorker.schedule(app)
            }
        }
    }

    private fun canScheduleExactAlarms(): Boolean {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            val alarmManager = app.getSystemService(android.app.AlarmManager::class.java)
            return alarmManager.canScheduleExactAlarms()
        }
        return true
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
    fun setShowDateInHeader(show: Boolean) { viewModelScope.launch { settings.setShowDateInHeader(show) } }
    fun setHideEmptyWeeks(hide: Boolean) { viewModelScope.launch { settings.setHideEmptyWeeks(hide) } }
    fun setThemeColorIndex(idx: Int) { viewModelScope.launch { settings.setThemeColorIndex(idx) } }
    fun setReminderMinutes(min: Int) { viewModelScope.launch { settings.setReminderMinutes(min) } }
    fun setAutoSyncOnStart(enabled: Boolean) { viewModelScope.launch { settings.setAutoSyncOnStart(enabled) } }
    fun setAutoSyncIntervalValue(value: Int) {
        viewModelScope.launch {
            settings.setAutoSyncIntervalValue(value)
            rescheduleSync()
        }
    }
    fun setAutoSyncIntervalUnit(unit: String) {
        viewModelScope.launch {
            settings.setAutoSyncIntervalUnit(unit)
            rescheduleSync()
        }
    }

    fun setTokenHeartbeat(enabled: Boolean) {
        viewModelScope.launch {
            settings.setTokenHeartbeat(enabled)
            if (enabled) {
                com.classapp.schedule.sync.HeartbeatWorker.schedule(app)
            } else {
                com.classapp.schedule.sync.HeartbeatWorker.cancel(app)
            }
        }
    }

    fun setShowExamSchedule(show: Boolean) {
        viewModelScope.launch { settings.setShowExamSchedule(show) }
    }

    fun setExamLookaheadWeeks(weeks: Int) {
        viewModelScope.launch { settings.setExamLookaheadWeeks(weeks) }
    }

    private suspend fun rescheduleSync() {
        val value = settings.autoSyncIntervalValue.first()
        val unit = settings.autoSyncIntervalUnit.first()
        com.classapp.schedule.sync.AutoSyncWorker.schedule(app, value, unit)
    }

    // Export - suspend-based for proper data access
    suspend fun exportIcs(): String? {
        val allCourses = courseDao.getAllCourses().first()
        if (allCourses.isEmpty()) return null
        val start = settings.semesterStart.first()
        return IcsExport.exportToIcs(allCourses, start, ::getStartTime, ::getEndTime)
    }

    suspend fun exportJson(): String? {
        val allCourses = courseDao.getAllCourses().first()
        if (allCourses.isEmpty()) return null
        return JsonImportExport.exportToJson(allCourses)
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

    fun saveBitmapToGallery(bitmap: Bitmap): Boolean {
        return ImageExport.saveBitmapToGallery(app, bitmap)
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
                    // Save encrypted credentials for auto-relogin
                    CredentialStore.save(app, studentId, password)
                    // Save CAS ticket for exam schedule access
                    settings.saveCasTicket(api.getCasTicket())
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

            // Convert and save — preserve manually edited courses
            val courses = CourseImporter.convertRemoteCourses(allRemoteCourses)
            val manualCourses = courseDao.getAllCourses().first().filter { it.isManuallyEdited }
            courseDao.deleteAllCourses()
            courses.forEach { courseDao.insertCourse(it) }
            // Re-insert manually edited courses (they overwrite school data)
            manualCourses.forEach { courseDao.insertCourse(it.copy(id = 0)) }

            _loginState.value = LoginState.ImportResult(courses.size)
        } catch (e: Exception) {
            _loginState.value = LoginState.Error("导入失败: ${e.message}")
        }
    }

    // Public refresh — re-fetch courses using saved token
    fun refreshFromSchool() {
        if (!api.hasToken() || savedStudentId.isEmpty()) {
            _isRefreshing.value = true
            viewModelScope.launch {
                _messages.emit("请先登录教务系统")
                _isRefreshing.value = false
            }
            return
        }
        _isRefreshing.value = true
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val calendar = api.getSchoolCalendar(savedStudentId).getOrNull()
                val year = calendar?.year ?: ""
                val semester = calendar?.semester ?: ""
                val weeks = calendar?.allWeek ?: totalWeeks.first()

                val remoteCourses = mutableListOf<com.classapp.schedule.api.RemoteCourse>()
                var failedWeeks = 0
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
                            failedWeeks++
                            android.util.Log.w("GdustApi", "refreshFromSchool: week $week failed: ${e.message}")
                        }
                    } catch (e: Exception) {
                        if (isTokenExpired(e.message)) {
                            handleTokenExpired()
                            return@launch
                        }
                        failedWeeks++
                        android.util.Log.w("GdustApi", "refreshFromSchool: week $week failed: ${e.message}")
                    }
                }
                if (failedWeeks > 0) {
                    _messages.emit("同步未完成，已保留原课表")
                    return@launch
                }
                if (remoteCourses.isNotEmpty()) {
                    val newCourses = CourseImporter.convertRemoteCourses(remoteCourses)
                    val existingCourses = courseDao.getAllCourses().first()
                    val manualCourses = existingCourses.filter { it.isManuallyEdited }
                    
                    // Smart diff: compare by name+day+startPeriod+weekRange
                    val existingKeys = existingCourses.filter { !it.isManuallyEdited }.map {
                        "${it.name}|${it.dayOfWeek}|${it.startPeriod}|${it.weekRange}"
                    }.toSet()
                    val newKeys = newCourses.map {
                        "${it.name}|${it.dayOfWeek}|${it.startPeriod}|${it.weekRange}"
                    }.toSet()
                    
                    val hasChanges = existingKeys != newKeys || 
                        newCourses.any { newC ->
                            val old = existingCourses.find { 
                                it.name == newC.name && it.dayOfWeek == newC.dayOfWeek && 
                                it.startPeriod == newC.startPeriod && it.weekRange == newC.weekRange 
                            }
                            old == null || old.teacher != newC.teacher || old.classroom != newC.classroom || old.periods != newC.periods
                        }
                    
                    if (hasChanges) {
                        courseDao.deleteAllCourses()
                        newCourses.forEach { courseDao.insertCourse(it) }
                        manualCourses.forEach { courseDao.insertCourse(it.copy(id = 0)) }
                        _messages.emit("已更新 ${newCourses.size} 门课程")
                    } else {
                        _messages.emit("课程无变化")
                    }
                }
            } catch (e: Exception) {
                if (isTokenExpired(e.message)) {
                    handleTokenExpired()
                } else {
                    _messages.emit("刷新失败: ${e.message ?: "网络错误"}")
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
        _isRefreshing.value = false
        // Only clear the API token, keep saved studentId for re-login
        api.setToken("")
        settings.markTokenExpired()
        _loginState.value = LoginState.TokenExpired
        _messages.emit("登录已过期，请快速登录")
    }

    /**
     * Quick re-login: only needs captcha. Credentials are stored encrypted.
     */
    /**
     * WebView login: called when CAS redirects to portal with loginCode.
     */
    fun webViewLogin(loginCode: String) {
        _loginState.value = LoginState.Loading
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                // Use the loginCode (which is the ticket) to get portal token
                api.checkTicket(loginCode).getOrNull()
                val user = api.portalLogin(loginCode).getOrThrow()
                savedStudentId = user.id
                _savedStudentIdFlow.value = user.id
                _loginState.value = LoginState.Success(user.realName.ifEmpty { user.id }, user.id)
                try {
                    val userInfo = api.getUserInfo().getOrNull()
                    settings.saveLoginInfo(user.token, user.id, user.realName, userInfo?.deptName ?: "")
                } catch (_: Exception) {
                    settings.saveLoginInfo(user.token, user.id, user.realName)
                }
                settings.clearTokenExpired()
                importFromSchool(user.id)
            } catch (e: Exception) {
                _loginState.value = LoginState.Error("登录失败: ${e.message}")
            }
        }
    }

    fun quickRelogin(captcha: String) {
        _loginState.value = LoginState.Loading
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            // Load saved credentials
            val sid = CredentialStore.loadStudentId(app).first()
            val pwd = CredentialStore.loadPassword(app).first()
            if (sid.isEmpty() || pwd.isEmpty()) {
                _loginState.value = LoginState.Error("无保存的凭据，请手动登录")
                return@launch
            }
            api.login(sid, pwd, captcha, captchaUuid)
                .onSuccess { user ->
                    savedStudentId = sid
                    _savedStudentIdFlow.value = sid
                    _loginState.value = LoginState.Success(user.realName.ifEmpty { sid }, sid)
                    try {
                        val userInfo = api.getUserInfo().getOrNull()
                        settings.saveLoginInfo(user.token, sid, user.realName, userInfo?.deptName ?: "")
                    } catch (_: Exception) {
                        settings.saveLoginInfo(user.token, sid, user.realName)
                    }
                    settings.clearTokenExpired()
                    importFromSchool(sid)
                }
                .onFailure { e ->
                    if (isTokenExpired(e.message)) {
                        handleTokenExpired()
                    } else {
                        _loginState.value = LoginState.Error(e.message ?: "登录失败")
                        refreshCaptcha()
                    }
                }
        }
    }

    // Exam schedule
    private val _examList = MutableStateFlow<List<com.classapp.schedule.api.ExamInfo>>(emptyList())
    val examList: StateFlow<List<com.classapp.schedule.api.ExamInfo>> = _examList
    private val _examLoading = MutableStateFlow(false)
    val examLoading: StateFlow<Boolean> = _examLoading
    private val _examYear = MutableStateFlow("")
    val examYear: StateFlow<String> = _examYear
    private val _examSemester = MutableStateFlow("")
    val examSemester: StateFlow<String> = _examSemester
    private val _showExamReloginDialog = MutableStateFlow(false)
    val showExamReloginDialog: StateFlow<Boolean> = _showExamReloginDialog

    fun setExamYear(year: String) { _examYear.value = year }
    fun setExamSemester(semester: String) { _examSemester.value = semester }

    fun refreshExamSchedule() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            _examLoading.value = true
            try {
                // Auto-detect academic year if not set
                val yearStr = _examYear.value.ifEmpty {
                    val now = java.time.LocalDate.now()
                    val month = now.monthValue
                    val startYear = if (month >= 9) now.year else now.year - 1
                    "$startYear-${startYear + 1}"
                }
                val semester = _examSemester.value.ifEmpty {
                    val month = java.time.LocalDate.now().monthValue
                    if (month in 2..7) "2" else "1"
                }
                if (_examYear.value.isEmpty()) _examYear.value = yearStr
                if (_examSemester.value.isEmpty()) _examSemester.value = semester

                // Extract start year from "2025-2026" format
                val year = yearStr.substringBefore("-")
                android.util.Log.d("GdustApi", "refreshExamSchedule: yearStr=$yearStr, year=$year, semester=$semester")
                val result = api.getExamSchedule(year, semester)
                result.onSuccess { exams ->
                    _examList.value = exams
                    // Cache exams
                    val json = kotlinx.serialization.json.Json.encodeToString(
                        kotlinx.serialization.builtins.ListSerializer(com.classapp.schedule.api.ExamInfo.serializer()), exams
                    )
                    settings.saveCachedExams(json, yearStr, semester)
                    _messages.emit("已获取 ${exams.size} 条考试信息")
                }.onFailure { e ->
                    val msg = e.message ?: ""
                    if (msg.contains("901") || msg.contains("认证失败") || msg.contains("未登录")) {
                        android.util.Log.d("GdustApi", "Exam auth failed (901), showing re-login dialog")
                        refreshCaptcha()
                        _showExamReloginDialog.value = true
                    } else {
                        _messages.emit("获取考试信息失败: $msg")
                    }
                }
            } catch (e: Exception) {
                _messages.emit("获取考试信息失败: ${e.message}")
            } finally {
                _examLoading.value = false
            }
        }
    }

    fun dismissExamReloginDialog() {
        _showExamReloginDialog.value = false
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
        if (_loginState.value is LoginState.Error) {
            _loginState.value = LoginState.LoggedOut
        }
    }
}

sealed class LoginState {
    data object LoggedOut : LoginState()
    data object Loading : LoginState()
    data class Success(val name: String, val studentId: String) : LoginState()
    data class Error(val message: String) : LoginState()
    data class ImportResult(val count: Int) : LoginState()
    data object TokenExpired : LoginState()
}
