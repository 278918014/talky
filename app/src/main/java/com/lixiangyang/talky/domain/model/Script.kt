package com.lixiangyang.talky.domain.model

data class Script(
    val id: Long,
    val title: String,
    val category: String,
    val content: String,
    val isFavorite: Boolean,
    val updatedAt: Long
)
