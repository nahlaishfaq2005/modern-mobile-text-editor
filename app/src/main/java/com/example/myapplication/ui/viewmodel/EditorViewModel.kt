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

    private val _isWordWrapEnabled = MutableStateFlow(false)
    val isWordWrapEnabled: StateFlow<Boolean> = _isWordWrapEnabled.asStateFlow()

    private val _showSearchReplace = MutableStateFlow(false)
    val showSearchReplace: StateFlow<Boolean> = _showSearchReplace.asStateFlow()

    private val _isReplaceMode = MutableStateFlow(false)
    val isReplaceMode: StateFlow<Boolean> = _isReplaceMode.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

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
        if (_content.value.text != newValue.text) {
            undoStack.push(_content.value)
            redoStack.clear()
            _saveStatus.value = "Unsaved Changes"
        }
        _content.value = newValue
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
        _isReplaceMode.value = isReplace
        _showSearchReplace.value = !_showSearchReplace.value
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
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
            val regex = Regex("\\b${Regex.escape(query)}\\b")
            val newText = _content.value.text.replace(regex, replace)
            onContentChange(_content.value.copy(text = newText))
        } catch (e: Exception) {
            // Fallback to simple replace if regex fails for some reason
            val newText = _content.value.text.replace(query, replace)
            onContentChange(_content.value.copy(text = newText))
        }
    }

    fun replaceNext() {
        val text = _content.value.text
        val query = _searchQuery.value
        val replace = _replaceQuery.value
        if (query.isEmpty()) return

        try {
            val regex = Regex("\\b${Regex.escape(query)}\\b")
            // Search starting from current selection end
            var match = regex.find(text, _content.value.selection.end)

            // Wrap around to start of file if not found
            if (match == null) {
                match = regex.find(text, 0)
            }

            match?.let {
                val range = it.range
                val newText = text.replaceRange(range, replace)
                // Move cursor to the end of the newly replaced text
                val newCursorPos = range.first + replace.length
                onContentChange(
                    _content.value.copy(
                        text = newText,
                        selection = androidx.compose.ui.text.TextRange(newCursorPos)
                    )
                )
            }
        } catch (e: Exception) {
            // Fallback logic for non-word characters or errors
            val index = text.indexOf(query, _content.value.selection.end)
            val finalIndex = if (index != -1) index else text.indexOf(query, 0)
            
            if (finalIndex != -1) {
                val newText = text.replaceRange(finalIndex, finalIndex + query.length, replace)
                onContentChange(
                    _content.value.copy(
                        text = newText,
                        selection = androidx.compose.ui.text.TextRange(finalIndex + replace.length)
                    )
                )
            }
        }
    }
}
