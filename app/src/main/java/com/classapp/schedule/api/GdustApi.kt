package com.classapp.schedule.api

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

// --- API Data Models ---

@Serializable
data class LoginCodeResponse(
    val code: Int = 0,
    val msg: String = "",
    val success: Boolean = false,
    val ok: Boolean = false,
    val data: LoginCodeData? = null,
    val uuid: String? = null,
    val image: String? = null,
    val img: String? = null,
    val captcha: String? = null,
    val base64: String? = null
) {
    fun extractData(): LoginCodeData? {
        if (data != null) {
            val hasImage = data.getImageBase64().isNotEmpty()
            val hasUuid = data.resolveUuid().isNotEmpty()
            if (hasImage || hasUuid) return data
        }
        val imgStr = image ?: img ?: captcha ?: base64
        val uuidStr = uuid
        if (uuidStr != null || imgStr != null) {
            return LoginCodeData(uuidStr ?: "", imgStr ?: "")
        }
        return null
    }
}

@Serializable
data class LoginCodeData(
    val uuid: String = "",
    val image: String = "",
    val img: String = "",
    val captcha: String = "",
    val base64: String = "",
    val codeUrl: String = "", // GDUST CAS returns image here
    val code: String = ""     // Some CAS return uuid as 'code'
) {
    fun getImageBase64(): String {
        val raw = codeUrl.ifEmpty { image.ifEmpty { img.ifEmpty { captcha.ifEmpty { base64 } } } }
        return raw.replace(Regex("^data:image/[^;]+;base64,"), "")
    }
    fun resolveUuid(): String = uuid.ifEmpty { code }
}

@Serializable
data class LoginByAccountRequest(
    val loginName: String,
    val loginPwd: String,
    val code: String,
    val uuid: String
)

@Serializable
data class LoginByAccountResponse(
    val code: Int = 0,
    val msg: String = "",
    val success: Boolean = false,
    val ok: Boolean = false,
    val data: String = "" // ticket on success
)

@Serializable
data class UserLoginResponse(
    val code: Int = 0,
    val msg: String = "",
    val success: Boolean = false,
    val data: UserLoginData? = null
)

@Serializable
data class UserLoginData(
    val userBase: UserBase? = null
)

@Serializable
data class UserBase(
    val id: String = "",
    val token: String = "",
    val realName: String = "",
    val sex: Int = 0,
    val email: String = ""
)

@Serializable
data class SchoolCalendarResponse(
    val code: Int = 0,
    val msg: String = "",
    val success: Boolean = false,
    val data: SchoolCalendarData? = null
)

@Serializable
data class SchoolCalendarData(
    val year: String = "",
    val semester: String = "",
    val week: Int = 0,
    val allWeek: Int = 20,
    val startTime: String = "",
    val endTime: String = ""
)

@Serializable
data class CourseListResponse(
    val code: Int = 0,
    val msg: String = "",
    val success: Boolean = false,
    val data: CourseListData? = null
)

@Serializable
data class CourseListData(
    val courseList: List<RemoteCourse> = emptyList()
)

@Serializable
data class RemoteCourse(
    val singleOrDoubleWeek: String? = null, // null, "单", "双"
    val week: Int = 0,
    val dayWeek: Int = 0,        // 1=Mon ... 7=Sun
    val whichSection: Int = 0,   // period number
    val classroomName: String = "",
    val courseName: String = "",
    val teacher: String = "",
    val courseDate: String = ""
)

// --- API Client ---

class GdustApi {

    companion object {
        private const val CAS_BASE = "https://cas.gdust.edu.cn"
        private const val PORTAL_BASE = "https://portal.gdust.edu.cn"
        private const val CAS_API = "$CAS_BASE/cas-api"
        private const val SMART_API = "$PORTAL_BASE/smart-admin-api"
        private const val JWXT_BASE = "http://172.16.254.1"
        private const val JWXT_EXAM_SERVICE = "$JWXT_BASE/sso/lyiotlogin"
    }

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .followRedirects(false) // Important: we need to handle redirects manually
        .cookieJar(InMemoryCookieJar())
        .build()

    private var authToken: String = ""

    /**
     * Step 1: Get captcha image + UUID
     */
    fun getLoginCode(): Result<LoginCodeData> = runCatching {
        val request = Request.Builder()
            .url("$CAS_API/cas/loginCode")
            .get()
            .build()

        val response = client.newCall(request).execute()
        val body = response.body?.string() ?: throw Exception("Empty response")
        
        // Try to parse as JSON
        val parsed = try {
            json.decodeFromString<LoginCodeResponse>(body)
        } catch (e: Exception) {
            throw Exception("解析验证码响应失败: ${body.take(100)}")
        }
        
        val data = parsed.extractData()
        val isSuccess = parsed.success || parsed.ok || parsed.code == 0
        when {
            data != null -> data
            isSuccess -> throw Exception("验证码数据为空")
            else -> throw Exception(parsed.msg.ifEmpty { "获取验证码失败" })
        }
    }

    /**
     * Step 2: Login with credentials + captcha
     */
    fun loginByAccount(loginName: String, loginPwd: String, code: String, uuid: String): Result<String> = runCatching {
        val bodyJson = json.encodeToString(LoginByAccountRequest.serializer(),
            LoginByAccountRequest(loginName, loginPwd, code, uuid))

        val request = Request.Builder()
            .url("$CAS_API/cas/loginByAccount")
            .post(bodyJson.toRequestBody("application/json".toMediaType()))
            .header("Origin", CAS_BASE)
            .header("Referer", "$CAS_BASE/cas/")
            .build()

        val response = client.newCall(request).execute()
        val respBody = response.body?.string() ?: throw Exception("Empty response")
        android.util.Log.d("GdustApi", "loginByAccount response: ${respBody.take(300)}")
        val parsed = json.decodeFromString<LoginByAccountResponse>(respBody)
        val isSuccess = parsed.success || parsed.ok || parsed.code == 1
        android.util.Log.d("GdustApi", "loginByAccount parsed: success=${parsed.success}, ok=${parsed.ok}, code=${parsed.code}, data=${parsed.data.take(50)}, msg=${parsed.msg}")
        if (isSuccess && parsed.data.isNotEmpty()) {
            parsed.data // ticket
        } else if (isSuccess) {
            throw Exception("登录成功但未返回ticket: ${parsed.msg}")
        } else {
            throw Exception(parsed.msg.ifEmpty { "登录失败" })
        }
    }

    /**
     * Step 3: Check ticket and get portal login code
     */
    fun checkTicket(ticket: String): Result<String> = runCatching {
        val request = Request.Builder()
            .url("$CAS_API/cas/checkTicket?ticket=$ticket")
            .get()
            .build()

        val response = client.newCall(request).execute()
        response.body?.string() // might be empty, but cookies are captured
        ticket
    }

    /**
     * Step 4: Get user token from portal
     */
    fun portalLogin(loginCode: String): Result<UserBase> = runCatching {
        val request = Request.Builder()
            .url("$SMART_API/user/login?loginCode=$loginCode&appId=portalRemote")
            .get()
            .header("Referer", "$PORTAL_BASE/")
            .build()

        val response = client.newCall(request).execute()
        val body = response.body?.string() ?: throw Exception("Empty response")
        val parsed = json.decodeFromString<UserLoginResponse>(body)
        if (parsed.success && parsed.data?.userBase != null) {
            val user = parsed.data.userBase
            authToken = user.token
            user
        } else {
            throw Exception(parsed.msg.ifEmpty { "获取token失败" })
        }
    }

    /**
     * Full login flow: returns (UserBase, ticket) where ticket can be used for portal login
     */
    fun login(studentId: String, password: String, captcha: String, captchaUuid: String): Result<UserBase> = runCatching {
        // Step 2: Login
        val ticket = loginByAccount(studentId, password, captcha, captchaUuid).getOrThrow()
        casTicket = ticket // Save CAS ticket for JWXT SSO

        // Step 3: Check ticket
        checkTicket(ticket).getOrThrow()

        // Step 4: Portal login
        portalLogin(ticket).getOrThrow()
    }

    /**
     * Get school calendar (semester info)
     */
    fun getSchoolCalendar(jobNumber: String): Result<SchoolCalendarData> = runCatching {
        val request = Request.Builder()
            .url("$SMART_API/app/zf/get_school_calendar?jobNumber=$jobNumber")
            .get()
            .header("TOKEN", authToken)
            .header("Referer", "$PORTAL_BASE/")
            .build()

        val response = client.newCall(request).execute()
        val body = response.body?.string() ?: throw Exception("Empty response")
        val parsed = json.decodeFromString<SchoolCalendarResponse>(body)
        if (parsed.success && parsed.data != null) {
            parsed.data
        } else {
            throw Exception(parsed.msg.ifEmpty { "获取学期信息失败" })
        }
    }

    /**
     * Get course list for a specific week (empty = all)
     */
    fun getStudentCourse(jobNumber: String, week: String = "", year: String = "", semester: String = ""): Result<List<RemoteCourse>> = runCatching {
        val url = "$SMART_API/app/zf/get_student_course?jobNumber=$jobNumber&week=$week&year=$year&semester=$semester"
        val request = Request.Builder()
            .url(url)
            .get()
            .header("TOKEN", authToken)
            .header("Referer", "$PORTAL_BASE/")
            .build()

        val response = client.newCall(request).execute()
        val body = response.body?.string() ?: throw Exception("Empty response")
        val parsed = json.decodeFromString<CourseListResponse>(body)
        if (parsed.success && parsed.data != null) {
            parsed.data.courseList
        } else {
            throw Exception(parsed.msg.ifEmpty { "获取课程失败" })
        }
    }

    fun setToken(token: String) { authToken = token }
    fun hasToken(): Boolean = authToken.isNotEmpty()

    /**
     * Open SSE connection, read first event to get clientId.
     * Returns (clientId, inputStream) — caller must keep reading inputStream for scan result.
     */
    fun openSseConnection(): Pair<String?, java.io.InputStream?> {
        return try {
            val request = Request.Builder()
                .url("$CAS_API/sse/subscribe")
                .get()
                .header("Accept", "text/event-stream")
                .header("User-Agent", "Mozilla/5.0")
                .build()
            val response = client.newCall(request).execute()
            val stream = response.body?.byteStream() ?: return null to null
            val buffer = ByteArray(4096)
            val sb = StringBuilder()
            var clientId: String? = null

            // Read until we get the first data event (clientId)
            while (true) {
                val bytesRead = stream.read(buffer)
                if (bytesRead == -1) break
                sb.append(String(buffer, 0, bytesRead))
                for (line in sb.toString().lines()) {
                    if (line.startsWith("data:")) {
                        val data = line.removePrefix("data:").trim()
                        if (data.isNotEmpty() && data != "[DONE]") {
                            clientId = data
                            android.util.Log.d("GdustApi", "SSE clientId: $clientId")
                            return clientId to stream
                        }
                    }
                }
                if (sb.length > 16384) break
            }
            null to null
        } catch (e: Exception) {
            android.util.Log.e("GdustApi", "openSseConnection failed: ${e.message}")
            null to null
        }
    }

    /**
     * Keep reading SSE stream for scan result (second data event).
     * Blocks until result arrives or stream closes.
     */
    fun readSseResult(stream: java.io.InputStream): String? {
        return try {
            val buffer = ByteArray(4096)
            val sb = StringBuilder()
            val startTime = System.currentTimeMillis()
            val timeoutMs = 5 * 60 * 1000L // 5 minutes

            while (System.currentTimeMillis() - startTime < timeoutMs) {
                val available = stream.available()
                if (available > 0) {
                    val bytesRead = stream.read(buffer, 0, minOf(available, buffer.size))
                    if (bytesRead == -1) break
                    sb.append(String(buffer, 0, bytesRead))
                    android.util.Log.d("GdustApi", "SSE chunk: ${String(buffer, 0, bytesRead).trim()}")
                    for (line in sb.toString().lines()) {
                        if (line.startsWith("data:")) {
                            val data = line.removePrefix("data:").trim()
                            if (data.isNotEmpty() && data != "[DONE]") {
                                android.util.Log.d("GdustApi", "SSE scan result: $data")
                                return data
                            }
                        }
                    }
                } else {
                    Thread.sleep(500)
                }
            }
            android.util.Log.d("GdustApi", "SSE timeout or closed")
            null
        } catch (e: Exception) {
            android.util.Log.e("GdustApi", "readSseResult failed: ${e.message}")
            null
        } finally {
            try { stream.close() } catch (_: Exception) {}
        }
    }

    /**
     * Get user info (name, department, etc.)
     */
    fun getUserInfo(): Result<UserInfo> = runCatching {
        val request = Request.Builder()
            .url("$SMART_API/app/userInfo/addition")
            .get()
            .header("TOKEN", authToken)
            .header("Referer", "$PORTAL_BASE/")
            .build()

        val response = client.newCall(request).execute()
        val body = response.body?.string() ?: throw Exception("Empty response")
        val parsed = json.decodeFromString<UserInfoResponse>(body)
        if (parsed.success && parsed.data != null) {
            parsed.data
        } else {
            throw Exception(parsed.msg.ifEmpty { "获取用户信息失败" })
        }
    }

    /**
     * Get exam schedule from JWXT (教务系统)
     * First login to exam system via CAS, then query
     */
    private var casTicket: String = ""

    fun setCasTicket(ticket: String) { casTicket = ticket }
    fun getCasTicket(): String = casTicket

    fun getExamSchedule(year: String, semester: String): Result<List<ExamInfo>> = runCatching {
        android.util.Log.d("GdustApi", "=== getExamSchedule START ===")

        val jwxtClient = client.newBuilder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .followRedirects(false)
            .build()

        // Try using saved CAS ticket directly with JWXT SSO
        if (casTicket.isNotEmpty()) {
            android.util.Log.d("GdustApi", "Trying saved casTicket with JWXT SSO")
            val ssoUrl = "$JWXT_BASE/sso/lyiotlogin?ticket=$casTicket"
            val ssoResp = jwxtClient.newCall(Request.Builder().url(ssoUrl).build()).execute()
            android.util.Log.d("GdustApi", "SSO: status=${ssoResp.code}, Location=${ssoResp.header("Location")}")

            // Follow redirects to establish session
            var next = ssoResp.header("Location")
            var step = 1
            while (next != null && step <= 6) {
                val url = if (next.startsWith("http")) next else "$JWXT_BASE$next"
                val r = jwxtClient.newCall(Request.Builder().url(url).build()).execute()
                android.util.Log.d("GdustApi", "Redir[$step]: $url -> ${r.code}, Location=${r.header("Location")}")
                next = r.header("Location")
                step++
            }
        }

        // Also try transferPage which might use CAS cookies
        android.util.Log.d("GdustApi", "Trying transferPage")
        val tpResp = jwxtClient.newCall(Request.Builder()
            .url("$CAS_BASE/cas/transferPage?service=$JWXT_EXAM_SERVICE")
            .build()).execute()
        android.util.Log.d("GdustApi", "transferPage: status=${tpResp.code}, Location=${tpResp.header("Location")}")
        val tpTicket = tpResp.header("Location")?.substringAfter("ticket=", "")
        if (!tpTicket.isNullOrEmpty()) {
            android.util.Log.d("GdustApi", "transferPage gave ticket: ${tpTicket.take(30)}...")
            val ssoUrl = "$JWXT_BASE/sso/lyiotlogin?ticket=$tpTicket"
            val ssoResp = jwxtClient.newCall(Request.Builder().url(ssoUrl).build()).execute()
            android.util.Log.d("GdustApi", "SSO with tpTicket: status=${ssoResp.code}, Location=${ssoResp.header("Location")}")
            var next = ssoResp.header("Location")
            var step = 1
            while (next != null && step <= 6) {
                val url = if (next.startsWith("http")) next else "$JWXT_BASE$next"
                val r = jwxtClient.newCall(Request.Builder().url(url).build()).execute()
                android.util.Log.d("GdustApi", "Redir[$step]: $url -> ${r.code}")
                next = r.header("Location")
                step++
            }
        }

        // Query
        val xqm = when (semester) { "1" -> "3"; "2" -> "12"; else -> "12" }
        val queryUrl = "$JWXT_BASE/kwgl/kscx_cxXsksxxIndex.html?doType=query&gnmkdm=N358105"
        val requestBody = FormBody.Builder()
            .add("xnm", year).add("xqm", xqm)
            .add("ksmcdmb_id", "").add("kch", "").add("kc", "").add("ksrq", "").add("kkbm_id", "")
            .add("_search", "false").add("nd", System.currentTimeMillis().toString())
            .add("queryModel.showCount", "50").add("queryModel.currentPage", "1")
            .add("queryModel.sortName", " ").add("queryModel.sortOrder", "asc").add("time", "0")
            .build()
        val qResp = jwxtClient.newCall(Request.Builder().url(queryUrl).post(requestBody)
            .header("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8")
            .header("X-Requested-With", "XMLHttpRequest").build()).execute()
        val body = qResp.body?.string() ?: throw Exception("Empty response")
        android.util.Log.d("GdustApi", "Query: status=${qResp.code}, body=${body.take(300)}")

        if (qResp.code != 200 || body.contains("<!DOCTYPE") || body.isEmpty()) {
            throw Exception("教务系统认证失败，请在校园网浏览器中先登录教务系统后再试")
        }
        val parsed = json.decodeFromString<ExamResponse>(body)
        android.util.Log.d("GdustApi", "=== DONE: ${parsed.items?.size ?: 0} exams ===")
        parsed.items ?: emptyList()
    }
}

@Serializable
data class UserInfoResponse(
    val code: Int = 0,
    val msg: String = "",
    val success: Boolean = false,
    val data: UserInfo? = null
)

@Serializable
data class UserInfo(
    val realName: String = "",
    val jobNum: String = "",
    val deptName: String = "",
    val leafDept: String = "",
    val userIcon: String = "",
    val idCardNum: String = ""
)

@Serializable
data class ExamResponse(
    val currentPage: Int = 0,
    val totalCount: Int = 0,
    val totalPage: Int = 0,
    val items: List<ExamInfo>? = null
)

@Serializable
data class ExamInfo(
    val kcmc: String = "",      // 课程名称
    val kssj: String = "",      // 考试时间 "2026-07-07(14:20-15:50)"
    val cdmc: String = "",      // 考场名称
    val cdxqmc: String = "",    // 考场校区
    val ksfs: String = "",      // 考试方式 "笔试（闭卷）"
    val khfs: String = "",      // 考核方式
    val kch: String = "",       // 课程号
    val xf: String = "",        // 学分
    val xnmc: String = "",      // 学年名称
    val xqmmc: String = "",     // 学期名称
    val sksj: String = "",      // 上课时间
    val kkxy: String = "",      // 开课学院
    val jsxx: String = ""       // 教师信息
) {
    fun getExamDate(): String {
        val match = Regex("""(\d{4}-\d{2}-\d{2})""").find(kssj)
        return match?.groupValues?.get(1) ?: ""
    }

    fun getExamTimeRange(): String {
        val match = Regex("""\((\d{2}:\d{2})-(\d{2}:\d{2})\)""").find(kssj)
        return if (match != null) "${match.groupValues[1]}-${match.groupValues[2]}" else ""
    }
}

/**
 * Simple in-memory cookie jar for OkHttp
 */
private class InMemoryCookieJar : CookieJar {
    private val cookies = mutableMapOf<String, List<Cookie>>()

    override fun saveFromResponse(url: HttpUrl, newCookies: List<Cookie>) {
        cookies[url.host] = newCookies
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        return cookies[url.host] ?: emptyList()
    }
}
