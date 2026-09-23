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
    }
}
