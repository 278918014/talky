package com.lixiangyang.talky.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.lixiangyang.talky.data.local.dao.ScriptDao
import com.lixiangyang.talky.data.local.dao.VideoPracticeDao
import com.lixiangyang.talky.data.local.entity.ScriptEntity
import com.lixiangyang.talky.data.local.entity.VideoPracticeEntity

@Database(
    entities = [ScriptEntity::class, VideoPracticeEntity::class],
    version = 1,
    exportSchema = false
)
abstract class TalkyDatabase : RoomDatabase() {
    abstract fun scriptDao(): ScriptDao
    abstract fun videoPracticeDao(): VideoPracticeDao
}
