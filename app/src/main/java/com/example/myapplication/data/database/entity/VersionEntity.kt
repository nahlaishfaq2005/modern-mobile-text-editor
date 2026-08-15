package com.example.myapplication.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entity representing a specific version/snapshot of a file.
 * Stores metadata about the version, but not the content itself.
 */
@Entity(
    tableName = "versions",
    foreignKeys = [
        ForeignKey(
            entity = FileEntity::class,
            parentColumns = ["id"],
            childColumns = ["fileId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["fileId"]),
        Index(value = ["fileId", "versionNumber"], unique = true)
    ]
)
data class VersionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val fileId: Long,
    val versionNumber: Int,
    val versionName: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val parentVersionId: Long? = null
)
