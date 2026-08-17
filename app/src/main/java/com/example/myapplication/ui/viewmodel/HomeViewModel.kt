package com.example.myapplication.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.RecentFile
import com.example.myapplication.data.RecentFileRepository
import com.example.myapplication.data.FileRepository
import com.example.myapplication.data.RoomVersionRepository
import com.example.myapplication.data.database.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

import java.util.UUID

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = RecentFileRepository(application)
    private val fileRepository = FileRepository(application)
    private val database = AppDatabase.getDatabase(application)
    private val versionRepository = RoomVersionRepository(database.versionDao())

    private val _lastOpenedFile = MutableStateFlow<RecentFile?>(null)
    val lastOpenedFile: StateFlow<RecentFile?> = _lastOpenedFile.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val recentFiles: StateFlow<List<RecentFile>> = combine(
        repository.recentFiles,
        _searchQuery
    ) { files, query ->
        if (query.isBlank()) {
            files
        } else {
            files.filter { it.name.contains(query, ignoreCase = true) }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onSearchQueryChange(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun addRecentFile(name: String, type: String, path: String) {
        val newFile = RecentFile(UUID.randomUUID().toString(), name, type, "Just now", path)
        repository.addRecentFile(name, type, path)
        _lastOpenedFile.value = newFile
    }

    fun setLastOpenedFile(file: RecentFile) {
        _lastOpenedFile.value = file
    }

    fun deleteFile(file: RecentFile) {
        viewModelScope.launch(Dispatchers.IO) {
            // 1. Delete the physical file
            fileRepository.deleteFile(file.name)
            
            // 2. Remove from recent files
            repository.removeRecentFile(file.name)
            
            // 3. Delete versions and deltas from DB
            versionRepository.deleteAllVersions(file.name)
            
            // 4. Reset last opened if it was the deleted file
            if (_lastOpenedFile.value?.name == file.name) {
                _lastOpenedFile.value = null
            }
        }
    }
}
