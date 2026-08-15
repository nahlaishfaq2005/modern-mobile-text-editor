package com.example.myapplication.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entity representing an incremental change (delta/patch) between file versions.
 * Stores only the diff, not the full content.
 */
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
    indices = [
        Index(value = ["versionId"])
    ]
)
data class DeltaEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val versionId: Long,
    val parentVersionId: Long? = null,
    val patchText: String,
    val createdAt: Long = System.currentTimeMillis()
)
