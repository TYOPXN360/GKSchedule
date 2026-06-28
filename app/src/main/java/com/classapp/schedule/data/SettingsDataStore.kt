package com.classapp.schedule.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsDataStore(private val context: Context) {

    companion object {
        private val SEMESTER_START = stringPreferencesKey("semester_start")
        private val TOTAL_WEEKS = intPreferencesKey("total_weeks")
        private val PERIODS_PER_DAY = intPreferencesKey("periods_per_day")
        private val DARK_MODE = stringPreferencesKey("dark_mode")
        private val LANGUAGE = stringPreferencesKey("language")
        private val FIRST_DAY_OF_WEEK = intPreferencesKey("first_day_of_week") // 1=Mon,7=Sun
        private val GRID_HEIGHT = intPreferencesKey("grid_height") // dp
        private val GRID_CORNER = intPreferencesKey("grid_corner") // dp
        private val GRID_SPACING = intPreferencesKey("grid_spacing") // dp
        private val SHOW_PERIOD_LABEL = booleanPreferencesKey("show_period_label")
        private val THEME_COLOR_INDEX = intPreferencesKey("theme_color_index")
        private val AUTO_GRID_HEIGHT = booleanPreferencesKey("auto_grid_height")
        private val MERGE_CONSECUTIVE = booleanPreferencesKey("merge_consecutive")
        private val SHOW_TIME_LABEL = booleanPreferencesKey("show_time_label")
        private val SAVED_TOKEN = stringPreferencesKey("saved_token")
        private val SAVED_STUDENT_ID = stringPreferencesKey("saved_student_id")
        private val SAVED_REAL_NAME = stringPreferencesKey("saved_real_name")
        private val SAVED_DEPT_NAME = stringPreferencesKey("saved_dept_name")
        private val TOKEN_EXPIRED = booleanPreferencesKey("token_expired")
        private val DETAILED_SPLIT = booleanPreferencesKey("detailed_split")
        private val COLOR_ENGINE = intPreferencesKey("color_engine") // 0=Monet 1=Container 2=Classic 3=HSL
        private val COLOR_GROUP_MODE = intPreferencesKey("color_group_mode") // 0=same 1=same+sat 2=different
        private val SHOW_DATE_IN_HEADER = booleanPreferencesKey("show_date_in_header")
        private val HIDE_EMPTY_WEEKS = booleanPreferencesKey("hide_empty_weeks")
        private val REMINDER_MINUTES = intPreferencesKey("reminder_minutes") // 0=off,5,10,15,30
        private val AUTO_SYNC_ON_START = booleanPreferencesKey("auto_sync_on_start")
        private val AUTO_SYNC_INTERVAL_VALUE = intPreferencesKey("auto_sync_interval_value")
        private val AUTO_SYNC_INTERVAL_UNIT = stringPreferencesKey("auto_sync_interval_unit") // "min", "h", "d"
        private val TOKEN_HEARTBEAT = booleanPreferencesKey("token_heartbeat")
        private val SHOW_EXAM_SCHEDULE = booleanPreferencesKey("show_exam_schedule")
        private val EXAM_LOOKAHEAD_WEEKS = intPreferencesKey("exam_lookahead_weeks")
        private val CAS_TICKET = stringPreferencesKey("cas_ticket")
        private val CACHED_EXAMS = stringPreferencesKey("cached_exams")
        private val CACHED_EXAM_YEAR = stringPreferencesKey("cached_exam_year")
        private val CACHED_EXAM_SEMESTER = stringPreferencesKey("cached_exam_semester")

        private val DEFAULT_START_TIMES = listOf(
            "08:30", "09:20", "10:25", "11:15",  // Morning 1-4
            "14:40", "15:30", "16:30", "17:20",  // Afternoon 5-8
            "19:30", "20:20"                       // Evening 9-10
        )
        private val DEFAULT_END_TIMES = listOf(
            "09:15", "10:05", "11:10", "12:00",
            "15:25", "16:15", "17:15", "18:05",
            "20:15", "21:05"
        )
    }

    val semesterStart: Flow<LocalDate> = context.dataStore.data.map { prefs ->
        prefs[SEMESTER_START]?.let { LocalDate.parse(it) } ?: LocalDate.now()
    }
    val totalWeeks: Flow<Int> = context.dataStore.data.map { prefs -> prefs[TOTAL_WEEKS] ?: 20 }
    val periodsPerDay: Flow<Int> = context.dataStore.data.map { prefs -> prefs[PERIODS_PER_DAY] ?: 10 }
    val darkMode: Flow<String> = context.dataStore.data.map { prefs -> prefs[DARK_MODE] ?: "system" }
    val language: Flow<String> = context.dataStore.data.map { prefs -> prefs[LANGUAGE] ?: "system" }
    val firstDayOfWeek: Flow<Int> = context.dataStore.data.map { prefs -> prefs[FIRST_DAY_OF_WEEK] ?: 1 }
    val gridHeight: Flow<Int> = context.dataStore.data.map { prefs -> prefs[GRID_HEIGHT] ?: 52 }
    val gridCorner: Flow<Int> = context.dataStore.data.map { prefs -> prefs[GRID_CORNER] ?: 8 }
    val gridSpacing: Flow<Int> = context.dataStore.data.map { prefs -> prefs[GRID_SPACING] ?: 2 }
    val showPeriodLabel: Flow<Boolean> = context.dataStore.data.map { prefs -> prefs[SHOW_PERIOD_LABEL] ?: true }
    val themeColorIndex: Flow<Int> = context.dataStore.data.map { prefs -> prefs[THEME_COLOR_INDEX] ?: 0 }
    val autoGridHeight: Flow<Boolean> = context.dataStore.data.map { prefs -> prefs[AUTO_GRID_HEIGHT] ?: true }
    val mergeConsecutive: Flow<Boolean> = context.dataStore.data.map { prefs -> prefs[MERGE_CONSECUTIVE] ?: true }
    val showTimeLabel: Flow<Boolean> = context.dataStore.data.map { prefs -> prefs[SHOW_TIME_LABEL] ?: true }
    val savedToken: Flow<String> = context.dataStore.data.map { prefs -> prefs[SAVED_TOKEN] ?: "" }
    val savedStudentId: Flow<String> = context.dataStore.data.map { prefs -> prefs[SAVED_STUDENT_ID] ?: "" }
    val savedRealName: Flow<String> = context.dataStore.data.map { prefs -> prefs[SAVED_REAL_NAME] ?: "" }
    val savedDeptName: Flow<String> = context.dataStore.data.map { prefs -> prefs[SAVED_DEPT_NAME] ?: "" }
    val tokenExpired: Flow<Boolean> = context.dataStore.data.map { prefs -> prefs[TOKEN_EXPIRED] ?: false }
    val detailedSplit: Flow<Boolean> = context.dataStore.data.map { prefs -> prefs[DETAILED_SPLIT] ?: false }
    val colorEngine: Flow<Int> = context.dataStore.data.map { prefs -> prefs[COLOR_ENGINE] ?: 0 }
    val colorGroupMode: Flow<Int> = context.dataStore.data.map { prefs -> prefs[COLOR_GROUP_MODE] ?: 2 }
    val showDateInHeader: Flow<Boolean> = context.dataStore.data.map { prefs -> prefs[SHOW_DATE_IN_HEADER] ?: false }
    val hideEmptyWeeks: Flow<Boolean> = context.dataStore.data.map { prefs -> prefs[HIDE_EMPTY_WEEKS] ?: false }
    val reminderMinutes: Flow<Int> = context.dataStore.data.map { prefs -> prefs[REMINDER_MINUTES] ?: 0 }
    val autoSyncOnStart: Flow<Boolean> = context.dataStore.data.map { prefs -> prefs[AUTO_SYNC_ON_START] ?: true }
    val autoSyncIntervalValue: Flow<Int> = context.dataStore.data.map { prefs -> prefs[AUTO_SYNC_INTERVAL_VALUE] ?: 1 }
    val autoSyncIntervalUnit: Flow<String> = context.dataStore.data.map { prefs -> prefs[AUTO_SYNC_INTERVAL_UNIT] ?: "d" }
    val tokenHeartbeat: Flow<Boolean> = context.dataStore.data.map { prefs -> prefs[TOKEN_HEARTBEAT] ?: true }
    val showExamSchedule: Flow<Boolean> = context.dataStore.data.map { prefs -> prefs[SHOW_EXAM_SCHEDULE] ?: false }
    val examLookaheadWeeks: Flow<Int> = context.dataStore.data.map { prefs -> prefs[EXAM_LOOKAHEAD_WEEKS] ?: 1 }
    val casTicket: Flow<String> = context.dataStore.data.map { prefs -> prefs[CAS_TICKET] ?: "" }
    val cachedExams: Flow<String> = context.dataStore.data.map { prefs -> prefs[CACHED_EXAMS] ?: "" }
    val cachedExamYear: Flow<String> = context.dataStore.data.map { prefs -> prefs[CACHED_EXAM_YEAR] ?: "" }
    val cachedExamSemester: Flow<String> = context.dataStore.data.map { prefs -> prefs[CACHED_EXAM_SEMESTER] ?: "" }

    fun getCurrentWeek(): Flow<Int> = context.dataStore.data.map { prefs ->
        val start = prefs[SEMESTER_START]?.let { LocalDate.parse(it) } ?: LocalDate.now()
        val days = java.time.temporal.ChronoUnit.DAYS.between(start, LocalDate.now())
        val week = (days / 7).toInt() + 1
        week.coerceIn(1, prefs[TOTAL_WEEKS] ?: 20)
    }

    fun getStartTimes(): List<String> = DEFAULT_START_TIMES
    fun getEndTimes(): List<String> = DEFAULT_END_TIMES

    fun getStartTime(period: Int): String =
        DEFAULT_START_TIMES[(period - 1).coerceIn(0, DEFAULT_START_TIMES.size - 1)]

    fun getEndTime(period: Int): String =
        DEFAULT_END_TIMES[(period - 1).coerceIn(0, DEFAULT_END_TIMES.size - 1)]

    // Setters
    suspend fun setSemesterStart(date: LocalDate) { context.dataStore.edit { it[SEMESTER_START] = date.toString() } }
    suspend fun setTotalWeeks(weeks: Int) { context.dataStore.edit { it[TOTAL_WEEKS] = weeks } }
    suspend fun setPeriodsPerDay(periods: Int) { context.dataStore.edit { it[PERIODS_PER_DAY] = periods } }
    suspend fun setDarkMode(mode: String) { context.dataStore.edit { it[DARK_MODE] = mode } }
    suspend fun setLanguage(lang: String) { context.dataStore.edit { it[LANGUAGE] = lang } }
    suspend fun setFirstDayOfWeek(day: Int) { context.dataStore.edit { it[FIRST_DAY_OF_WEEK] = day } }
    suspend fun setGridHeight(h: Int) { context.dataStore.edit { it[GRID_HEIGHT] = h } }
    suspend fun setGridCorner(c: Int) { context.dataStore.edit { it[GRID_CORNER] = c } }
    suspend fun setGridSpacing(s: Int) { context.dataStore.edit { it[GRID_SPACING] = s } }
    suspend fun setShowPeriodLabel(show: Boolean) { context.dataStore.edit { it[SHOW_PERIOD_LABEL] = show } }
    suspend fun setThemeColorIndex(idx: Int) { context.dataStore.edit { it[THEME_COLOR_INDEX] = idx } }
    suspend fun setAutoGridHeight(auto: Boolean) { context.dataStore.edit { it[AUTO_GRID_HEIGHT] = auto } }
    suspend fun setMergeConsecutive(merge: Boolean) { context.dataStore.edit { it[MERGE_CONSECUTIVE] = merge } }
    suspend fun setShowTimeLabel(show: Boolean) { context.dataStore.edit { it[SHOW_TIME_LABEL] = show } }
    suspend fun saveLoginInfo(token: String, studentId: String, realName: String, deptName: String = "") {
        context.dataStore.edit {
            it[SAVED_TOKEN] = token
            it[SAVED_STUDENT_ID] = studentId
            it[SAVED_REAL_NAME] = realName
            it[SAVED_DEPT_NAME] = deptName
            it[TOKEN_EXPIRED] = false
        }
    }
    suspend fun clearLoginInfo() {
        context.dataStore.edit {
            it.remove(SAVED_TOKEN)
            it.remove(SAVED_STUDENT_ID)
            it.remove(SAVED_REAL_NAME)
            it.remove(SAVED_DEPT_NAME)
            it[TOKEN_EXPIRED] = false
        }
    }
    suspend fun markTokenExpired() {
        context.dataStore.edit {
            it.remove(SAVED_TOKEN)
            it[TOKEN_EXPIRED] = true
        }
    }
    suspend fun clearTokenExpired() { context.dataStore.edit { it[TOKEN_EXPIRED] = false } }
    suspend fun setDetailedSplit(split: Boolean) { context.dataStore.edit { it[DETAILED_SPLIT] = split } }
    suspend fun setColorEngine(engine: Int) { context.dataStore.edit { it[COLOR_ENGINE] = engine } }
    suspend fun setColorGroupMode(mode: Int) { context.dataStore.edit { it[COLOR_GROUP_MODE] = mode } }
    suspend fun setShowDateInHeader(show: Boolean) { context.dataStore.edit { it[SHOW_DATE_IN_HEADER] = show } }
    suspend fun setHideEmptyWeeks(hide: Boolean) { context.dataStore.edit { it[HIDE_EMPTY_WEEKS] = hide } }
    suspend fun setReminderMinutes(min: Int) { context.dataStore.edit { it[REMINDER_MINUTES] = min } }
    suspend fun setAutoSyncOnStart(enabled: Boolean) { context.dataStore.edit { it[AUTO_SYNC_ON_START] = enabled } }
    suspend fun setAutoSyncIntervalValue(value: Int) { context.dataStore.edit { it[AUTO_SYNC_INTERVAL_VALUE] = value } }
    suspend fun setAutoSyncIntervalUnit(unit: String) { context.dataStore.edit { it[AUTO_SYNC_INTERVAL_UNIT] = unit } }
    suspend fun setTokenHeartbeat(enabled: Boolean) { context.dataStore.edit { it[TOKEN_HEARTBEAT] = enabled } }
    suspend fun setShowExamSchedule(show: Boolean) { context.dataStore.edit { it[SHOW_EXAM_SCHEDULE] = show } }
    suspend fun setExamLookaheadWeeks(weeks: Int) { context.dataStore.edit { it[EXAM_LOOKAHEAD_WEEKS] = weeks.coerceIn(0, 20) } }
    suspend fun saveCasTicket(ticket: String) { context.dataStore.edit { it[CAS_TICKET] = ticket } }
    suspend fun saveCachedExams(json: String, year: String, semester: String) {
        context.dataStore.edit {
            it[CACHED_EXAMS] = json
            it[CACHED_EXAM_YEAR] = year
            it[CACHED_EXAM_SEMESTER] = semester
        }
    }
}
