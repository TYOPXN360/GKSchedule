package com.ty.gkschedule

import android.app.Application
import android.content.Context

class GKScheduleApp : Application() {
    companion object {
        lateinit var context: Context
    }

    override fun onCreate() {
        super.onCreate()
        context = this
        // ponytail: 每次冷启动补排当天——用户划掉实况通知后不再回来；重排是幂等的
        // （进行中的课会在2秒内重发进度首帧并重新挂上分钟链）
        Thread {
            runCatching { com.ty.gkschedule.notification.ReminderScheduler.scheduleTodayFromStore(this) }
        }.start()
    }
}
