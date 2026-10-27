package com.nezuko.ai.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Dao
interface NezukoDao {
    @Query("SELECT * FROM messages ORDER BY createdAt ASC")
    fun messages(): Flow<List<MessageEntity>>
    @Insert suspend fun addMessage(message: MessageEntity): Long
    @Query("DELETE FROM messages") suspend fun clearMessages()

    @Query("SELECT * FROM memories ORDER BY pinned DESC, createdAt DESC")
    fun memories(): Flow<List<MemoryEntity>>
    @Query("SELECT * FROM memories WHERE content LIKE '%' || :query || '%' ORDER BY pinned DESC, createdAt DESC")
    fun searchMemories(query: String): Flow<List<MemoryEntity>>
    @Insert suspend fun addMemory(memory: MemoryEntity): Long
    @Update suspend fun updateMemory(memory: MemoryEntity)
    @Query("DELETE FROM memories WHERE id = :id") suspend fun deleteMemory(id: Long)
    @Query("DELETE FROM memories") suspend fun clearMemories()

    @Query("SELECT * FROM tasks ORDER BY completed ASC, dueAt ASC")
    fun tasks(): Flow<List<TaskEntity>>
    @Insert suspend fun addTask(task: TaskEntity): Long
    @Update suspend fun updateTask(task: TaskEntity)
    @Query("DELETE FROM tasks WHERE id = :id") suspend fun deleteTask(id: Long)
}

@Database(entities = [MessageEntity::class, MemoryEntity::class, TaskEntity::class], version = 1, exportSchema = false)
abstract class NezukoDatabase : RoomDatabase() {
    abstract fun dao(): NezukoDao
    companion object {
        @Volatile private var INSTANCE: NezukoDatabase? = null
        fun get(context: Context): NezukoDatabase = INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(context.applicationContext, NezukoDatabase::class.java, "nezuko.db")
                .build().also { INSTANCE = it }
        }
    }
}
