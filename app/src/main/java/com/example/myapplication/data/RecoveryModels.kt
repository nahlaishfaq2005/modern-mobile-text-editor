package com.example.myapplication.data

import java.io.Serializable

data class RecoveryData(
    val filePath: String?,
    val fileName: String,
    val content: String,
    val lastCacheTime: Long,
    val isModified: Boolean
) : Serializable
