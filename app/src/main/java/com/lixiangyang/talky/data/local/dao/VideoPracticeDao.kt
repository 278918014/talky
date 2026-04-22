package com.lixiangyang.talky.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.lixiangyang.talky.data.local.entity.VideoPracticeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VideoPracticeDao {
    @Query("SELECT * FROM video_practices ORDER BY recordedAt DESC")
    fun observePractices(): Flow<List<VideoPracticeEntity>>

    @Query("SELECT * FROM video_practices WHERE id = :id")
    fun observePractice(id: Long): Flow<VideoPracticeEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<VideoPracticeEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: VideoPracticeEntity): Long

    @Query("SELECT COUNT(*) FROM video_practices")
    suspend fun count(): Int
}
