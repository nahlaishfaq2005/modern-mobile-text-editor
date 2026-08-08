package com.example.myapplication.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.RecentFile
import com.example.myapplication.data.RecentFileRepository
import kotlinx.coroutines.flow.*

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = RecentFileRepository(application)
    // varible val wht i sit a vrible??=is recent fiel buititnthing otrr ht tthf
    private val _searchQuery = MutableStateFlow("")
    //mutble??
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    //

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
        repository.addRecentFile(name, type, path)
    }
}
