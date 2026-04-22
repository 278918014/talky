package com.lixiangyang.talky

import android.app.Application
import com.lixiangyang.talky.core.AppContainer

class TalkyApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
