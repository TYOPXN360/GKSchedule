package com.ty.gkschedule.api

import org.json.JSONObject

// ponytail: 不在本App登录学习通——课程ID从 ChaoxingSignFaker 的 FETCH_COURSES 结果回传
object ChaoxingApi {
    data class CxCourse(val name: String, val classId: Int, val courseId: Long)

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
