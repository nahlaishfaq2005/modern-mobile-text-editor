package com.example.myapplication.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class RecentFileRepository(context: Context) {
    private val sharedPreferences = context.getSharedPreferences("recent_files", Context.MODE_PRIVATE)
    private val gson = Gson()
    
    private val _recentFiles = MutableStateFlow<List<RecentFile>>(loadFiles())
    val recentFiles: StateFlow<List<RecentFile>> = _recentFiles.asStateFlow()

    private fun loadFiles(): List<RecentFile> {
        val json = sharedPreferences.getString("files_list", null) ?: return listOf(
            RecentFile(UUID.randomUUID().toString(), "MainActivity.kt", "Kotlin", "10m ago", "sample1"),
            RecentFile(UUID.randomUUID().toString(), "Notes.md", "Markdown", "3h ago", "sample2"),
            RecentFile(UUID.randomUUID().toString(), "README.md", "Markdown", "3h ago", "sample3")
        )
        val type = object : TypeToken<List<RecentFile>>() {}.type
        return gson.fromJson(json, type)
    }

    private fun saveFiles(files: List<RecentFile>) {
        val json = gson.toJson(files)
        sharedPreferences.edit().putString("files_list", json).apply()
    }

    fun addRecentFile(name: String, type: String, path: String) {
        val newList = _recentFiles.value.toMutableList()
        newList.removeAll { it.name == name && it.type == type }
        newList.add(0, RecentFile(UUID.randomUUID().toString(), name, type, "Just now", path))
        val finalList = newList.take(10)
        _recentFiles.value = finalList
        saveFiles(finalList)
    }

    fun removeRecentFile(name: String) {
        val newList = _recentFiles.value.toMutableList()
        newList.removeAll { it.name == name }
        _recentFiles.value = newList
        saveFiles(newList)
    }
}
