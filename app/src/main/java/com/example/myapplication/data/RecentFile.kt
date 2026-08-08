package com.example.myapplication.data

data class RecentFile(
    val id: String,
    val name: String,
    val type: String, // "Kotlin", "Markdown", etc.
    val lastEdited: String,
    val path: String
)
