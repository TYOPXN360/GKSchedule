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
     * Subscribe to SSE to get clientId for QR code login.
     * SSE is a streaming protocol — must read line by line, not wait for full response.
     */
    fun getSseClientId(): String? = try {
        val request = Request.Builder()
            .url("$CAS_API/sse/subscribe")
            .get()
            .header("Accept", "text/event-stream")
            .build()
        val response = client.newCall(request).execute()
        val reader = response.body?.byteStream()?.bufferedReader()
            ?: throw Exception("Empty SSE response")
        var clientId: String? = null
        // Read first few lines looking for "data: <clientId>"
        var linesRead = 0
        reader.useLines { lines ->
            for (line in lines) {
                linesRead++
                android.util.Log.d("GdustApi", "SSE line: $line")
                if (line.startsWith("data:")) {
                    val data = line.removePrefix("data:").trim()
                    if (data.isNotEmpty() && data != "[DONE]") {
                        clientId = data
                        break
                    }
                }
                if (linesRead > 20) break // safety limit
            }
        }
        android.util.Log.d("GdustApi", "SSE clientId: $clientId")
        clientId
    } catch (e: Exception) {
        android.util.Log.e("GdustApi", "getSseClientId failed: ${e.message}")
        null
    }

    /**
     * Check if the QR code login has been completed.
     * After user scans QR, CAS creates a ticket.
     * We poll the loginByAccount endpoint with the clientId.
     */
    fun checkSseResult(clientId: String): String? = try {
        // Try to exchange clientId for a ticket
        val request = Request.Builder()
            .url("$CAS_API/cas/checkPassword")
            .get()
            .build()
        val response = client.newCall(request).execute()
        val body = response.body?.string() ?: ""
        android.util.Log.d("GdustApi", "checkSseResult: ${body.take(200)}")
        // If we got a ticket in the response or redirect
        if (body.contains("GKCAS")) {
            Regex("GKCAS[a-zA-Z0-9]+").find(body)?.value
        } else null
    } catch (_: Exception) { null }

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
