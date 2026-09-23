package com.ty.gkschedule.api

import com.ty.gkschedule.R
import org.json.JSONObject

// ponytail: 不在本App登录学习通——课程ID从 ChaoxingSignFaker 的 FETCH_COURSES 结果里拿
object ChaoxingApi {
    data class CxCourse(val name: String, val classId: Int, val courseId: Long)

    private const val ACTION_OPEN_SIGN = "org.aquamarine5.brainspark.chaoxingsignfaker.action.OPEN_SIGN"
    private const val FAKER_PACKAGE = "org.aquamarine5.brainspark.chaoxingsignfaker"

    // ponytail: 今日页/课表页共用的去签到跳转——缺ID和未安装都在这兜底
    fun goSignOrToast(
        context: android.content.Context,
        course: com.ty.gkschedule.data.Course,
        unmatchedTip: String
    ) {
        if (course.chaoxingClassId <= 0 || course.chaoxingCourseId <= 0L || course.chaoxingFid <= 0) {
            android.widget.Toast.makeText(context, unmatchedTip, android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        val intent = android.content.Intent(ACTION_OPEN_SIGN).apply {
            setPackage(FAKER_PACKAGE)
            putExtra("classId", course.chaoxingClassId)
            putExtra("courseId", course.chaoxingCourseId)
            putExtra("fid", course.chaoxingFid)
            putExtra("courseName", course.name)
        }
        runCatching { context.startActivity(intent) }.onFailure {
            android.widget.Toast.makeText(
                context,
                if (it is android.content.ActivityNotFoundException) context.getString(R.string.go_sign_not_installed)
                else context.getString(R.string.go_sign_launch_fail, it.message ?: context.getString(R.string.go_sign_unknown)),
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun parseFetchResult(json: String): Pair<Int, List<CxCourse>> {
        val root = JSONObject(json)
        val fid = root.optInt("fid", 0)
        val courses = root.optJSONArray("courses")
        val list = buildList {
            if (courses != null) for (i in 0 until courses.length()) {
                val course = courses.getJSONObject(i)
                add(
                    CxCourse(
                        name = course.getString("name"),
                        classId = course.getInt("classId"),
                        courseId = course.getLong("courseId")
                    )
                )
            }
        }
        return fid to list
    }
}
