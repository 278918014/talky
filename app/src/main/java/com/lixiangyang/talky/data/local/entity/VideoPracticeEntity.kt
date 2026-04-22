package com.lixiangyang.talky.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "video_practices")
data class VideoPracticeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val recordedAt: Long,
    val durationSeconds: Int,
    val resolution: String,
    val scriptId: Long? = null,
    val filePath: String = "",
    val thumbnailPath: String = ""
)
