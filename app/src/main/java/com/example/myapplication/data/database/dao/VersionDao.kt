package com.example.myapplication.data.database.dao

import androidx.room.*
import com.example.myapplication.data.database.entity.VersionEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for the versions table.
 */
@Dao
interface VersionDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertVersion(version: VersionEntity): Long

    @Query("SELECT * FROM versions WHERE id = :id")
    suspend fun getVersionById(id: Long): VersionEntity?

    @Query("SELECT * FROM versions WHERE fileId = :fileId ORDER BY versionNumber DESC")
    fun getVersionsForFile(fileId: Long): Flow<List<VersionEntity>>

    @Query("SELECT * FROM versions WHERE fileId = :fileId AND versionNumber = :versionNumber")
    suspend fun getVersionByNumber(fileId: Long, versionNumber: Int): VersionEntity?

    @Query("SELECT * FROM versions WHERE fileId = :fileId ORDER BY versionNumber DESC LIMIT 1")
    suspend fun getLatestVersion(fileId: Long): VersionEntity?

    @Query("SELECT * FROM versions WHERE fileId = :fileId ORDER BY versionNumber ASC")
    suspend fun getVersionsListForFile(fileId: Long): List<VersionEntity>

    @Update
    suspend fun updateVersion(version: VersionEntity)

    @Delete
    suspend fun deleteVersion(version: VersionEntity)

    @Query("DELETE FROM versions WHERE fileId = :fileId")
    suspend fun deleteVersionsForFile(fileId: Long)
}
