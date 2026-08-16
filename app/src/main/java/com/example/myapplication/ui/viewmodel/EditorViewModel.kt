package com.example.myapplication.ui.viewmodel

import android.app.Application
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.AndroidViewModel
import com.example.myapplication.data.FileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Stack

class EditorViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = FileRepository(application)
    
    private val _content = mutableStateOf(TextFieldValue(""))
    val content: State<TextFieldValue> = _content

    private val undoStack = Stack<TextFieldValue>()
    private val redoStack = Stack<TextFieldValue>()

    private val _isWordWrapEnabled = MutableStateFlow(true)
    val isWordWrapEnabled: StateFlow<Boolean> = _isWordWrapEnabled.asStateFlow()

    private val _showSearchReplace = MutableStateFlow(false)
    val showSearchReplace: StateFlow<Boolean> = _showSearchReplace.asStateFlow()

    private val _isReplaceMode = MutableStateFlow(false)
    val isReplaceMode: StateFlow<Boolean> = _isReplaceMode.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<IntRange>>(emptyList())
    val searchResults: StateFlow<List<IntRange>> = _searchResults.asStateFlow()

    private val _currentResultIndex = MutableStateFlow(-1)
    val currentResultIndex: StateFlow<Int> = _currentResultIndex.asStateFlow()

    private val _replaceQuery = MutableStateFlow("")
    val replaceQuery: StateFlow<String> = _replaceQuery.asStateFlow()

    private val _saveStatus = MutableStateFlow("Saved")
    val saveStatus: StateFlow<String> = _saveStatus.asStateFlow()

    fun loadFile(fileName: String) {
        val loadedContent = repository.loadFile(fileName)
        _content.value = TextFieldValue(loadedContent)
        undoStack.clear()
        redoStack.clear()
        _saveStatus.value = "Saved"
    }

    fun saveFile(fileName: String) {
        val success = repository.saveFile(fileName, _content.value.text)
        if (success) {
            _saveStatus.value = "Saved"
        } else {
            _saveStatus.value = "Error Saving"
        }
    }

    fun onContentChange(newValue: TextFieldValue) {
        val oldText = _content.value.text
        val newText = newValue.text
        if (oldText != newText) {
            undoStack.push(_content.value)
            redoStack.clear()
            _saveStatus.value = "Unsaved Changes"
            _content.value = newValue
            if (_showSearchReplace.value) {
                updateSearchResults(_searchQuery.value)
            }
        } else {
            _content.value = newValue
        }
    }

    fun undo() {
        if (undoStack.isNotEmpty()) {
            redoStack.push(_content.value)
            _content.value = undoStack.pop()
            _saveStatus.value = "Unsaved Changes"
        }
    }

    fun redo() {
        if (redoStack.isNotEmpty()) {
            undoStack.push(_content.value)
            _content.value = redoStack.pop()
            _saveStatus.value = "Unsaved Changes"
        }
    }

    fun toggleWordWrap() {
        _isWordWrapEnabled.value = !_isWordWrapEnabled.value
    }

    fun toggleSearchReplace(isReplace: Boolean = false) {
        if (_showSearchReplace.value && _isReplaceMode.value == isReplace) {
            _showSearchReplace.value = false
        } else {
            _showSearchReplace.value = true
            _isReplaceMode.value = isReplace
        }
        
        if (!_showSearchReplace.value) {
            _searchQuery.value = ""
            _searchResults.value = emptyList()
            _currentResultIndex.value = -1
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
        updateSearchResults(query)
    }

    private fun updateSearchResults(query: String) {
        if (query.isEmpty()) {
            _searchResults.value = emptyList()
            _currentResultIndex.value = -1
            return
        }

        val text = _content.value.text
        val results = mutableListOf<IntRange>()
        
        try {
            // Use word boundaries \b to ensure only whole words are matched
            val regex = Regex("\\b${Regex.escape(query)}\\b", RegexOption.IGNORE_CASE)
            regex.findAll(text).forEach { match ->
                // Store as IntRange(start, exclusive_end) to match previous logic
                results.add(IntRange(match.range.first, match.range.last + 1))
            }
        } catch (e: Exception) {
            // Fallback to simple search if regex fails
            var index = text.indexOf(query, 0, ignoreCase = true)
            while (index != -1) {
                results.add(IntRange(index, index + query.length))
                index = text.indexOf(query, index + query.length, ignoreCase = true)
            }
        }
        
        _searchResults.value = results
        if (results.isNotEmpty()) {
            val currentStart = _content.value.selection.start
            val existingIdx = results.indexOfFirst { it.first >= currentStart }
            _currentResultIndex.value = if (existingIdx != -1) existingIdx else 0
        } else {
            _currentResultIndex.value = -1
        }
    }

    fun nextSearchResult() {
        if (_searchResults.value.isEmpty()) return
        val nextIndex = (_currentResultIndex.value + 1) % _searchResults.value.size
        _currentResultIndex.value = nextIndex
        scrollToMatch(nextIndex)
    }

    fun previousSearchResult() {
        if (_searchResults.value.isEmpty()) return
        val prevIndex = if (_currentResultIndex.value <= 0) _searchResults.value.size - 1 else _currentResultIndex.value - 1
        _currentResultIndex.value = prevIndex
        scrollToMatch(prevIndex)
    }

    private fun scrollToMatch(index: Int) {
        val range = _searchResults.value[index]
        _content.value = _content.value.copy(
            selection = androidx.compose.ui.text.TextRange(range.first, range.last)
        )
    }

    fun onReplaceQueryChange(query: String) {
        _replaceQuery.value = query
    }

    fun replaceAll() {
        val query = _searchQuery.value
        val replace = _replaceQuery.value
        if (query.isEmpty()) return

        try {
            // Use word boundaries \b to ensure only whole words are replaced
            val regex = Regex("\\b${Regex.escape(query)}\\b", RegexOption.IGNORE_CASE)
            val newText = _content.value.text.replace(regex, replace)
            onContentChange(_content.value.copy(text = newText))
        } catch (e: Exception) {
            // Fallback to simple replace if regex fails for some reason
            val newText = _content.value.text.replace(query, replace)
            onContentChange(_content.value.copy(text = newText))
        }
    }

    fun replaceNext() {
        val query = _searchQuery.value
        val replace = _replaceQuery.value
        if (query.isEmpty() || _searchResults.value.isEmpty() || _currentResultIndex.value == -1) return

        val text = _content.value.text
        val range = _searchResults.value[_currentResultIndex.value]
        
        val newText = text.replaceRange(range.first, range.last, replace)
        val newCursorPos = range.first + replace.length
        
        onContentChange(
            _content.value.copy(
                text = newText,
                selection = androidx.compose.ui.text.TextRange(newCursorPos)
            )
        )
    }
}
