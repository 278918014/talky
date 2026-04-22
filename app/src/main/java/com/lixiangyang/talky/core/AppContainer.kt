package com.lixiangyang.talky.core

import android.content.Context
import androidx.room.Room
import com.lixiangyang.talky.data.local.TalkyDatabase
import com.lixiangyang.talky.data.repository.TalkyRepository

class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val database: TalkyDatabase by lazy {
        Room.databaseBuilder(appContext, TalkyDatabase::class.java, "talky.db")
            .fallbackToDestructiveMigration()
            .build()
    }

    val repository: TalkyRepository by lazy {
        TalkyRepository(
            scriptDao = database.scriptDao(),
            videoPracticeDao = database.videoPracticeDao()
        )
    }
}
