package com.example.myapplication.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity representing a file's metadata in the database.
 * The actual file content is stored in the file system.
 */
@Entity(tableName = "files")
data class FileEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val fileName: String,
    val filePath: String,
    val encoding: String = "UTF-8",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
