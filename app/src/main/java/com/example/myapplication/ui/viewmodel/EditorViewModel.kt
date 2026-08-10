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
        val newText = _content.value.text.replace(_searchQuery.value, _replaceQuery.value)
        onContentChange(_content.value.copy(text = newText))
    }
    
    fun replaceNext() {
        val text = _content.value.text
        val query = _searchQuery.value
        val replace = _replaceQuery.value
        if (query.isEmpty()) return
        
        val index = text.indexOf(query, _content.value.selection.end)
        if (index != -1) {
            val newText = text.replaceRange(index, index + query.length, replace)
            onContentChange(_content.value.copy(text = newText))
        } else {
            val wrapIndex = text.indexOf(query, 0)
            if (wrapIndex != -1) {
                val newText = text.replaceRange(wrapIndex, wrapIndex + query.length, replace)
                onContentChange(_content.value.copy(text = newText))
            }
        }
    }
}
