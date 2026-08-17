package com.example.myapplication.data

data class RecoveryData(
    val fileName: String,
    val content: String,
    val lastCacheTime: Long,
    val isModified: Boolean
)
