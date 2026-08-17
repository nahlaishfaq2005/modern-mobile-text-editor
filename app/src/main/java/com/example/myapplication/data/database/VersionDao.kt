package com.example.myapplication.data.database

import androidx.room.*

@Dao
interface VersionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVersion(version: VersionEntity): Long

    @Query("SELECT * FROM versions WHERE fileId = :fileId ORDER BY versionNumber ASC")
    suspend fun getVersionsForFile(fileId: String): List<VersionEntity>

    @Query("UPDATE versions SET isCurrent = 0 WHERE fileId = :fileId")
    suspend fun clearCurrentFlag(fileId: String)

    @Query("SELECT * FROM versions WHERE id = :versionId")
    suspend fun getVersionById(versionId: String): VersionEntity?

    @Query("SELECT * FROM versions WHERE fileId = :fileId ORDER BY versionNumber DESC LIMIT 1")
    suspend fun getLatestVersionForFile(fileId: String): VersionEntity?

    @Query("DELETE FROM versions WHERE id = :versionId")
    suspend fun deleteVersion(versionId: String): Int

    @Query("DELETE FROM versions WHERE fileId = :fileId")
    suspend fun deleteAllVersionsForFile(fileId: String): Int

    @Query("SELECT DISTINCT fileId FROM versions")
    suspend fun getAllFileIdsWithVersions(): List<String>

    @Query("UPDATE versions SET name = :newName WHERE id = :versionId")
    suspend fun updateVersionName(versionId: String, newName: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDelta(delta: DeltaEntity): Long

    @Transaction
    suspend fun insertVersionWithDelta(version: VersionEntity, delta: DeltaEntity) {
        insertVersion(version)
        insertDelta(delta)
    }

    @Query("SELECT * FROM deltas WHERE versionId = :versionId")
    suspend fun getDeltaForVersion(versionId: String): DeltaEntity?

    @Query("DELETE FROM deltas WHERE versionId = :versionId")
    suspend fun deleteDeltaForVersion(versionId: String): Int
}
