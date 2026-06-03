package com.lixiangyang.talky.data.repository

import com.lixiangyang.talky.data.local.dao.ScriptDao
import com.lixiangyang.talky.data.local.dao.VideoPracticeDao
import com.lixiangyang.talky.data.local.entity.ScriptEntity
import com.lixiangyang.talky.data.local.entity.VideoPracticeEntity
import com.lixiangyang.talky.domain.model.DashboardSummary
import com.lixiangyang.talky.domain.model.Script
import com.lixiangyang.talky.domain.model.VideoPractice
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TalkyRepository(
    private val scriptDao: ScriptDao,
    private val videoPracticeDao: VideoPracticeDao
) {
    fun observeScripts(category: String? = null): Flow<List<Script>> {
        val source = if (category.isNullOrBlank() || category == CATEGORY_ALL) {
            scriptDao.observeScripts()
        } else {
            scriptDao.observeScriptsByCategory(category)
        }
        return source.map { items -> items.map { it.toDomain() } }
    }

    fun observeScript(id: Long): Flow<Script> =
        scriptDao.observeScript(id).filterNotNull().map { it.toDomain() }

    fun observePractices(): Flow<List<VideoPractice>> =
        videoPracticeDao.observePractices().map { items -> items.map { it.toDomain() } }

    fun observePractice(id: Long): Flow<VideoPractice> =
        videoPracticeDao.observePractice(id).filterNotNull().map { it.toDomain() }

    fun observeDashboardSummary(): Flow<DashboardSummary> =
        observePractices().map { practices ->
            val now = System.currentTimeMillis()
            val todayKey = dayKey(now)
            val todayCount = practices.count { dayKey(it.recordedAt) == todayKey }
            DashboardSummary(
                todayCount = todayCount,
                totalCount = practices.size,
                latestPracticeLabel = practices.firstOrNull()?.let { formatTimestamp(it.recordedAt) } ?: "还没有练习"
            )
        }

    suspend fun createScript(title: String, category: String, content: String): Long {
        return scriptDao.insert(
            ScriptEntity(
                title = title,
                category = category,
                content = content
            )
        )
    }

    suspend fun createPractice(
        title: String,
        durationSeconds: Int,
        resolution: String,
        filePath: String,
        thumbnailPath: String,
        scriptId: Long? = null
    ): Long {
        val entity = VideoPracticeEntity(
            title = title,
            recordedAt = System.currentTimeMillis(),
            durationSeconds = durationSeconds,
            resolution = resolution,
            filePath = filePath,
            thumbnailPath = thumbnailPath,
            scriptId = scriptId
        )
        return videoPracticeDao.insert(entity)
    }

    suspend fun deletePractice(id: Long) {
        videoPracticeDao.deleteById(id)
    }

    suspend fun deletePractices(ids: List<Long>) {
        if (ids.isNotEmpty()) {
            videoPracticeDao.deleteByIds(ids)
        }
    }

    suspend fun ensureSeedData() {
        val hasScripts = observeScripts().first().isNotEmpty()
        val hasVideos = observePractices().first().isNotEmpty()
        if (!hasScripts) {
            scriptDao.insertAll(seedScripts())
        }
        if (!hasVideos) {
            videoPracticeDao.insertAll(seedPractices())
        }
    }

    private fun ScriptEntity.toDomain() = Script(
        id = id,
        title = title,
        category = category,
        content = content,
        isFavorite = isFavorite,
        updatedAt = updatedAt
    )

    private fun VideoPracticeEntity.toDomain() = VideoPractice(
        id = id,
        title = title,
        recordedAt = recordedAt,
        durationSeconds = durationSeconds,
        resolution = resolution,
        scriptId = scriptId,
        filePath = filePath,
        thumbnailPath = thumbnailPath
    )

    private fun dayKey(timestamp: Long): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return formatter.format(Date(timestamp))
    }

    private fun formatTimestamp(timestamp: Long): String {
        val formatter = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
        return formatter.format(Date(timestamp))
    }

    private fun seedScripts(): List<ScriptEntity> {
        return emptyList()
    }

    private fun seedPractices(): List<VideoPracticeEntity> {
        return emptyList()
    }

    companion object {
        const val CATEGORY_ALL = "全部"
        val categories = listOf(CATEGORY_ALL, "励志", "情感", "职场", "散文", "收藏")
    }
}
