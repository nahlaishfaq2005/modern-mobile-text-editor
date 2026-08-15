package com.example.myapplication.data.database.dao

import androidx.room.*
import com.example.myapplication.data.database.entity.DeltaEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for the deltas table.
 */
@Dao
interface DeltaDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDelta(delta: DeltaEntity): Long

    @Query("SELECT * FROM deltas WHERE id = :id")
    suspend fun getDeltaById(id: Long): DeltaEntity?

    @Query("SELECT * FROM deltas WHERE versionId = :versionId")
    suspend fun getDeltaForVersion(versionId: Long): DeltaEntity?

    @Query("""
        SELECT d.* FROM deltas d
        JOIN versions v ON d.versionId = v.id
        WHERE v.fileId = :fileId
        ORDER BY v.versionNumber ASC
    """)
    fun getDeltasForFile(fileId: Long): Flow<List<DeltaEntity>>

    @Update
    suspend fun updateDelta(delta: DeltaEntity)

    @Delete
    suspend fun deleteDelta(delta: DeltaEntity)

    @Query("DELETE FROM deltas WHERE versionId = :versionId")
    suspend fun deleteDeltasForVersion(versionId: Long)
}
