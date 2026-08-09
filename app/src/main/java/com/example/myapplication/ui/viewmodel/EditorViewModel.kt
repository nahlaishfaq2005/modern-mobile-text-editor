package com.example.myapplication.ui.viewmodel

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Stack

class EditorViewModel : ViewModel() {
    private val _content = mutableStateOf(TextFieldValue(""))
    val content: State<TextFieldValue> = _content

    private val undoStack = Stack<TextFieldValue>()
    private val redoStack = Stack<TextFieldValue>()

    private val _isWordWrapEnabled = MutableStateFlow(false)
    val isWordWrapEnabled: StateFlow<Boolean> = _isWordWrapEnabled.asStateFlow()

    private val _showSearchReplace = MutableStateFlow(false)
    val showSearchReplace: StateFlow<Boolean> = _showSearchReplace.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _replaceQuery = MutableStateFlow("")
    val replaceQuery: StateFlow<String> = _replaceQuery.asStateFlow()

    fun onContentChange(newValue: TextFieldValue) {
        if (_content.value.text != newValue.text) {
            undoStack.push(_content.value)
            redoStack.clear()
        }
        _content.value = newValue
    }

    fun undo() {
        if (undoStack.isNotEmpty()) {
            redoStack.push(_content.value)
            _content.value = undoStack.pop()
        }
    }

    fun redo() {
        if (redoStack.isNotEmpty()) {
            undoStack.push(_content.value)
            _content.value = redoStack.pop()
        }
    }

    fun toggleWordWrap() {
        _isWordWrapEnabled.value = !_isWordWrapEnabled.value
    }

    fun toggleSearchReplace() {
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
            // Wrap around
            val wrapIndex = text.indexOf(query, 0)
            if (wrapIndex != -1) {
                val newText = text.replaceRange(wrapIndex, wrapIndex + query.length, replace)
                onContentChange(_content.value.copy(text = newText))
            }
        }
    }
}
