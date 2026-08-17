package com.example.myapplication.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "versions",
    indices = [Index(value = ["fileId"])]
)
data class VersionEntity(
    @PrimaryKey val id: String,
    val fileId: String,
    val versionNumber: Int,
    val name: String?,
    val timestamp: Long,
    val parentVersionId: String?,
    val restoreSourceVersionId: String?,
    val isCurrent: Boolean,
    val isAutoSaved: Boolean
)

@Entity(
    tableName = "deltas",
    foreignKeys = [
        ForeignKey(
            entity = VersionEntity::class,
            parentColumns = ["id"],
            childColumns = ["versionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["versionId"])]
)
data class DeltaEntity(
    @PrimaryKey val id: String,
    val versionId: String,
    val patch: String
)
