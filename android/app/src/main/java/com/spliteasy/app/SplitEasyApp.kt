package com.spliteasy.app

import android.app.Application
import android.content.Context

class SplitEasyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        appContext = applicationContext
    }

    companion object {
        lateinit var appContext: Context
            private set
    }
}
