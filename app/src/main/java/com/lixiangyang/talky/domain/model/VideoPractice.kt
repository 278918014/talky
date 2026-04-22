package com.lixiangyang.talky.domain.model

data class VideoPractice(
    val id: Long,
    val title: String,
    val recordedAt: Long,
    val durationSeconds: Int,
    val resolution: String,
    val scriptId: Long?,
    val filePath: String,
    val thumbnailPath: String
)
