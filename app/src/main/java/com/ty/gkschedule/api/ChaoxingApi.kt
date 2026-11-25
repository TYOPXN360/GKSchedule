package com.ty.gkschedule.api

import com.ty.gkschedule.R
import org.json.JSONObject

// ponytail: 不在本App登录学习通——课程ID从 ChaoxingSignFaker 的 FETCH_COURSES 结果里拿
object ChaoxingApi {
    data class CxCourse(val name: String, val classId: Int, val courseId: Long)

    // ponytail: 一门本地课可以绑定多门超星课；多绑时跳转前让用户选
    data class CxTarget(val name: String, val classId: Int, val courseId: Long, val fid: Int)

    private const val ACTION_OPEN_SIGN = "org.aquamarine5.brainspark.chaoxingsignfaker.action.OPEN_SIGN"
    private const val FAKER_PACKAGE = "org.aquamarine5.brainspark.chaoxingsignfaker"
    // ponytail: 上次拉取的超学通课名缓存（classId:courseId -> 超学通课名），跳转时当标题用
    @Volatile
    var cxNames: Map<String, String> = emptyMap()

    // ponytail: 本地课名 -> 手动多选绑定的超星课；为空时回退课程行上的单条 ID
    @Volatile
    var cxBindings: Map<String, List<CxTarget>> = emptyMap()

    fun cxKey(classId: Int, courseId: Long) = "$classId:$courseId"
    private const val ACTION_OPEN_EXTERNAL_SETTING =
        "org.aquamarine5.brainspark.chaoxingsignfaker.action.OPEN_SETTING_TO_ALLOW_EXTERNAL_QUERY_PREMISSION"

    // ponytail: 拉不到课程多半是 Faker 侧未允许外部查询，直接把他跳到那个开关页
    fun openAllowExternalQuerySetting(context: android.content.Context): Boolean =
        runCatching {
            context.startActivity(
                android.content.Intent(ACTION_OPEN_EXTERNAL_SETTING).setPackage(FAKER_PACKAGE)
                    .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }.isSuccess

    /** 这门课可跳转的所有超星课程：优先多选绑定，其次课程行上的单个 ID。 */
    fun targetsFor(course: com.ty.gkschedule.data.Course): List<CxTarget> {
        cxBindings[course.name]?.takeIf { it.isNotEmpty() }?.let { return it }
        if (course.chaoxingClassId > 0 && course.chaoxingCourseId > 0L && course.chaoxingFid > 0) {
            return listOf(
                CxTarget(
                    name = cxNames[cxKey(course.chaoxingClassId, course.chaoxingCourseId)] ?: course.name,
                    classId = course.chaoxingClassId,
                    courseId = course.chaoxingCourseId,
                    fid = course.chaoxingFid
                )
            )
        }
        return emptyList()
    }

    // ponytail: 今日页/课表页共用的去签到跳转——缺ID和未安装都在这兜底
    fun goSignOrToast(
        context: android.content.Context,
        course: com.ty.gkschedule.data.Course,
        unmatchedTip: String
    ) {
        val targets = targetsFor(course)
        if (targets.isEmpty()) {
            android.widget.Toast.makeText(context, unmatchedTip, android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        openSign(context, targets.first())
    }

    fun openSign(context: android.content.Context, target: CxTarget) {
        val intent = android.content.Intent(ACTION_OPEN_SIGN).apply {
            setPackage(FAKER_PACKAGE)
            putExtra("classId", target.classId)
            putExtra("courseId", target.courseId)
            putExtra("fid", target.fid)
            // 用超学通自己的课名（拉取时缓存的），别用课表课名
            cxNames[cxKey(target.classId, target.courseId)]?.let {
                putExtra("courseName", it)
            }
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

    fun bindingsToJson(bindings: Map<String, List<CxTarget>>): String {
        val root = JSONObject()
        bindings.forEach { (name, targets) ->
            root.put(
                name,
                org.json.JSONArray().apply {
                    targets.forEach { t ->
                        put(
                            JSONObject().put("name", t.name).put("classId", t.classId)
                                .put("courseId", t.courseId).put("fid", t.fid)
                        )
                    }
                }
            )
        }
        return root.toString()
    }

    fun parseBindings(json: String): Map<String, List<CxTarget>> {
        if (json.isBlank()) return emptyMap()
        val root = JSONObject(json)
        val out = mutableMapOf<String, List<CxTarget>>()
        root.keys().forEach { key ->
            val array = root.optJSONArray(key) ?: return@forEach
            val list = buildList {
                for (i in 0 until array.length()) {
                    val o = array.getJSONObject(i)
                    add(
                        CxTarget(
                            name = o.optString("name", key),
                            classId = o.optInt("classId", 0),
                            courseId = o.optLong("courseId", 0L),
                            fid = o.optInt("fid", 0)
                        )
                    )
                }
            }.filter { it.classId > 0 && it.courseId > 0L && it.fid > 0 }
            if (list.isNotEmpty()) out[key] = list
        }
        return out
    }

    // ponytail: 复用与 provider 相同的 JSON 结构，本地存未匹配列表
    fun toJson(fid: Int, courses: List<CxCourse>): String =
        JSONObject().put("fid", fid).put(
            "courses",
            org.json.JSONArray().apply {
                courses.forEach {
                    put(JSONObject().put("name", it.name).put("classId", it.classId).put("courseId", it.courseId))
                }
            }
        ).toString()

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
