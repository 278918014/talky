package com.lixiangyang.talky.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.lixiangyang.talky.data.local.entity.ScriptEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScriptDao {
    @Query("SELECT * FROM scripts ORDER BY isFavorite DESC, updatedAt DESC")
    fun observeScripts(): Flow<List<ScriptEntity>>

    @Query("SELECT * FROM scripts WHERE category = :category ORDER BY updatedAt DESC")
    fun observeScriptsByCategory(category: String): Flow<List<ScriptEntity>>

    @Query("SELECT * FROM scripts WHERE id = :id")
    fun observeScript(id: Long): Flow<ScriptEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<ScriptEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: ScriptEntity): Long

    @Update
    suspend fun update(item: ScriptEntity)
}
