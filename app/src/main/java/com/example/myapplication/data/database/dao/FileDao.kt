package com.example.myapplication.data.database.dao

import androidx.room.*
import com.example.myapplication.data.database.entity.FileEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for the files table.
 */
@Dao
interface FileDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFile(file: FileEntity): Long

    @Query("SELECT * FROM files WHERE id = :id")
    suspend fun getFileById(id: Long): FileEntity?

    @Query("SELECT * FROM files WHERE filePath = :path")
    suspend fun getFileByPath(path: String): FileEntity?

    @Query("SELECT * FROM files ORDER BY updatedAt DESC")
    fun getAllFiles(): Flow<List<FileEntity>>

    @Update
    suspend fun updateFile(file: FileEntity)

    @Delete
    suspend fun deleteFile(file: FileEntity)

    @Query("DELETE FROM files WHERE id = :id")
    suspend fun deleteFileById(id: Long)
}
