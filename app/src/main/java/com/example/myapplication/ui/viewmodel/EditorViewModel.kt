package com.example.myapplication.ui.viewmodel

import android.app.Application
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.*
import com.example.myapplication.data.database.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Stack

class EditorViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = FileRepository(application)
    private val recoveryManager = RecoveryManager(application)
    private val recentFileRepository = RecentFileRepository(application)
    
    // Task 31: Use Room database for versioning
    private val database = AppDatabase.getDatabase(application)
    private val versionRepository: VersionRepository = RoomVersionRepository(database.versionDao())
    private val deltaRepository: DeltaRepository = RoomDeltaRepository(database.versionDao())
    private val diffManager = DiffManager()
    
    private val versionManager = VersionManager(versionRepository, deltaRepository, diffManager)
    
    private var _currentFileName = MutableStateFlow("")
    val currentFileName: StateFlow<String> = _currentFileName.asStateFlow()

    private var recoveryJob: Job? = null

    private val _content = mutableStateOf(TextFieldValue(""))
    val content: State<TextFieldValue> = _content
    
    // UI state for Save As dialog
    private val _showSaveAsDialog = MutableStateFlow(false)
    val showSaveAsDialog: StateFlow<Boolean> = _showSaveAsDialog.asStateFlow()

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

    private val _isLocked = MutableStateFlow(false)
    val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()

    private val _isPreviewMode = MutableStateFlow(false)
    val isPreviewMode: StateFlow<Boolean> = _isPreviewMode.asStateFlow()

    private val _saveStatus = MutableStateFlow("Saved")
    val saveStatus: StateFlow<String> = _saveStatus.asStateFlow()

    private val _pendingRecovery = MutableStateFlow<RecoveryData?>(null)
    val pendingRecovery: StateFlow<RecoveryData?> = _pendingRecovery.asStateFlow()

    private val _versions = MutableStateFlow<List<Version>>(emptyList())
    val versions: StateFlow<List<Version>> = _versions.asStateFlow()

    private val _filesWithVersions = MutableStateFlow<List<Pair<String, Int>>>(emptyList())
    val filesWithVersions: StateFlow<List<Pair<String, Int>>> = _filesWithVersions.asStateFlow()

    fun loadFile(fileName: String) {
        if (_currentFileName.value == fileName && _content.value.text.isNotEmpty()) return

        _currentFileName.value = fileName
        loadVersions()
        loadFilesWithVersions()
        
        // Task 21: Check for recovery data
        if (recoveryManager.hasRecoveryData(fileName)) {
            val recoveryData = recoveryManager.getRecoveryData(fileName)
            val currentFileContent = repository.loadFile(fileName)
            if (recoveryData != null && recoveryData.content != currentFileContent) {
                _pendingRecovery.value = recoveryData
            } else {
                // If it's the same, just delete the recovery file
                recoveryManager.deleteRecoveryFile(fileName)
                loadNormalFile(fileName)
            }
        } else {
            loadNormalFile(fileName)
        }
    }

    private fun loadNormalFile(fileName: String) {
        val loadedContent = repository.loadFile(fileName)
        _content.value = TextFieldValue(loadedContent)
        undoStack.clear()
        redoStack.clear()
        _saveStatus.value = "Saved"
        startRecoveryJob()
    }

    fun recoverUnsavedWork(data: RecoveryData) {
        recoveryManager.deleteRecoveryFile(data.fileName)
        _content.value = TextFieldValue(data.content)
        _saveStatus.value = "Recovered"
        _pendingRecovery.value = null
        startRecoveryJob()
    }

    fun discardRecovery(fileName: String) {
        recoveryManager.deleteRecoveryFile(fileName)
        _pendingRecovery.value = null
        loadNormalFile(fileName)
    }

    private fun startRecoveryJob() {
        recoveryJob?.cancel()
        recoveryJob = viewModelScope.launch {
            while (true) {
                delay(RECOVERY_INTERVAL)
                cacheCurrentFile()
            }
        }
    }

    private fun cacheCurrentFile() {
        if (_currentFileName.value.isNotEmpty()) {
            val isModified = _saveStatus.value == "Unsaved Changes" || _saveStatus.value == "Recovered"
            recoveryManager.cacheActiveFile(
                fileName = _currentFileName.value,
                content = _content.value.text,
                isModified = isModified
            )
        }
    }

    /**
     * Task 4: Save the current file.
     * If the file is new/untitled, triggers the Save As flow.
     */
    fun saveFile(fileName: String) {
        if (fileName.isEmpty() || fileName == "Untitled") {
            _showSaveAsDialog.value = true
            return
        }

        val success = repository.saveFile(fileName, _content.value.text)
        if (success) {
            _saveStatus.value = "Saved"
            // Task 19: Delete recovery file after successful save
            recoveryManager.deleteRecoveryFile(fileName)
            
            // Task 23: Create a manually saved version
            createVersion(fileName, "Manually saved", false)
            
            // Task 4: Update recent files
            val type = getFileTypeFromName(fileName)
            recentFileRepository.addRecentFile(fileName, type, fileName)
        } else {
            _saveStatus.value = "Error Saving"
        }
    }

    /**
     * Task 5: Save As functionality.
     * Saves the current editor content as a new file with a different name.
     */
    fun saveFileAs(newName: String): Boolean {
        if (newName.isBlank()) return false
        
        val oldName = _currentFileName.value
        val success = repository.saveFile(newName, _content.value.text)
        
        if (success) {
            // If there was an old file and the name has changed, delete the old one
            if (oldName.isNotEmpty() && oldName != newName) {
                repository.deleteFile(oldName)
                recoveryManager.deleteRecoveryFile(oldName)
                
                // Remove the old file from recent files so it doesn't show up as a duplicate
                recentFileRepository.removeRecentFile(oldName)
            }
            
            // Update active file state
            _currentFileName.value = newName
            _saveStatus.value = "Saved"
            
            // Task 4: Add the new name to recent files
            val type = getFileTypeFromName(newName)
            recentFileRepository.addRecentFile(newName, type, newName)
            
            // Reset recovery tracking for the new filename
            startRecoveryJob()
            
            return true
        }
        return false
    }

    fun setShowSaveAsDialog(show: Boolean) {
        _showSaveAsDialog.value = show
    }

    private fun getFileTypeFromName(name: String): String {
        return when {
            name.endsWith(".kt") -> "Kotlin"
            name.endsWith(".md") -> "Markdown"
            else -> "Plain Text"
        }
    }

    fun createVersion(fileName: String, name: String? = null, isAutoSaved: Boolean = false) {
        viewModelScope.launch {
            val result = versionManager.createVersion(fileName, _content.value.text, name, isAutoSaved)
            if (result is VersionManager.VersionResult.Success) {
                loadVersions()
                loadFilesWithVersions()
            }
        }
    }

    fun loadVersions() {
        if (_currentFileName.value.isNotEmpty()) {
            viewModelScope.launch(Dispatchers.IO) {
                val history = versionManager.getVersionHistory(_currentFileName.value)
                _versions.value = history
            }
        }
    }

    fun loadFilesWithVersions() {
        viewModelScope.launch(Dispatchers.IO) {
            val files = versionManager.getAllFilesWithVersions()
            val filesWithCounts = files.map { fileName ->
                fileName to versionManager.getVersionCount(fileName)
            }
            _filesWithVersions.value = filesWithCounts
        }
    }

    fun deleteVersion(versionId: String) {
        viewModelScope.launch {
            val version = versionRepository.getVersion(versionId)
            if (version?.isCurrent == true) {
                val allVersions = versionRepository.getVersions(version.fileId)
                if (allVersions.size <= 1) {
                    // Cannot delete the only version
                    return@launch
                }
                // Make the next latest version current
                val nextLatest = allVersions.filter { it.id != versionId }.maxByOrNull { it.versionNumber }
                if (nextLatest != null) {
                    versionRepository.saveVersion(nextLatest.copy(isCurrent = true))
                }
            }
            versionManager.deleteVersion(versionId)
            loadVersions()
            loadFilesWithVersions()
        }
    }

    fun renameVersion(versionId: String, newName: String) {
        viewModelScope.launch {
            versionManager.renameVersion(versionId, newName)
            loadVersions()
        }
    }

    suspend fun getVersionContent(versionId: String): String {
        return versionManager.reconstructContent(_currentFileName.value, versionId)
    }

    suspend fun getVersionCount(fileName: String): Int {
        return versionManager.getVersionCount(fileName)
    }

    suspend fun getDiff(oldVersionId: String, newVersionId: String): List<DiffLine> {
        val oldContent = versionManager.reconstructContent(_currentFileName.value, oldVersionId)
        val newContent = versionManager.reconstructContent(_currentFileName.value, newVersionId)
        return diffManager.getDiffResult(oldContent, newContent)
    }

    suspend fun getDiffWithCurrent(versionId: String): List<DiffLine> {
        val oldContent = versionManager.reconstructContent(_currentFileName.value, versionId)
        val newContent = _content.value.text
        return diffManager.getDiffResult(oldContent, newContent)
    }

    fun restoreVersion(versionId: String) {
        val fileName = _currentFileName.value
        if (fileName.isEmpty()) return
        
        viewModelScope.launch {
            // Step 1: Reconstruct the historical version's complete content
            val targetContent = versionManager.reconstructContent(fileName, versionId)
            
            // Step 2 & 3 & 4 & 5 & 7: Create a NEW CURRENT version (V5) 
            // from the current state (V4) with content of V2.
            // VersionManager.createVersion handles delta calculation from current latest.
            val result = versionManager.createVersion(
                fileId = fileName,
                content = targetContent,
                name = "Restored from version ${getVersionShortId(versionId)}",
                isAutoSaved = false,
                restoreSourceVersionId = versionId
            )
            
            if (result is VersionManager.VersionResult.Success) {
                // Step 6: Update editor content and physical file
                _content.value = TextFieldValue(targetContent)
                repository.saveFile(fileName, targetContent)
                
                _saveStatus.value = "Restored"
                
                // Cleanup recovery
                recoveryManager.deleteRecoveryFile(fileName)
                
                // Refresh history UI
                _versions.value = versionManager.getVersionHistory(fileName)
                val files = versionManager.getAllFilesWithVersions()
                _filesWithVersions.value = files.map { it to versionManager.getVersionCount(it) }
            }
        }
    }

    private suspend fun getVersionShortId(versionId: String): String {
        val version = versionRepository.getVersion(versionId)
        return version?.versionNumber?.toString() ?: versionId.take(8)
    }

    fun deleteCurrentFile() {
        if (_currentFileName.value.isEmpty()) return
        
        val fileName = _currentFileName.value
        viewModelScope.launch {
            // 1. Delete physical file
            repository.deleteFile(fileName)
            // 2. Remove from recent files
            recentFileRepository.removeRecentFile(fileName)
            // 3. Delete versions
            versionRepository.deleteAllVersions(fileName)
            // 4. Reset editor state
            _currentFileName.value = ""
            _content.value = TextFieldValue("")
            _saveStatus.value = "Deleted"
            recoveryJob?.cancel()
            recoveryManager.deleteRecoveryFile(fileName)
        }
    }

    fun onContentChange(newValue: TextFieldValue) {
        if (_isLocked.value) return
        if (_content.value.text != newValue.text) {
            undoStack.push(_content.value)
            redoStack.clear()
            _saveStatus.value = "Unsaved Changes"
        }
        _content.value = newValue
    }

    fun formatContent(fileType: String) {
        if (_isLocked.value) return
        if (fileType == "Kotlin") {
            val formatted = formatKotlin(_content.value.text)
            if (formatted != _content.value.text) {
                onContentChange(_content.value.copy(text = formatted))
            }
        }
    }

    private fun formatKotlin(text: String): String {
        val lines = text.lines()
        val result = StringBuilder()
        var indent = 0
        lines.forEach { line ->
            var trimmed = line.trim()
            // Very basic indentation logic
            if (trimmed.startsWith("}") || trimmed.startsWith(")")) {
                indent = maxOf(0, indent - 1)
            }
            
            result.append("    ".repeat(indent)).append(trimmed).append("\n")
            
            if (trimmed.endsWith("{") || trimmed.endsWith("(")) {
                indent++
            }
        }
        return result.toString().trimEnd()
    }

    override fun onCleared() {
        recoveryJob?.cancel()
    }

    companion object {
        private const val RECOVERY_INTERVAL = 1_000L
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

    fun toggleLock() {
        _isLocked.value = !_isLocked.value
    }

    fun togglePreviewMode() {
        _isPreviewMode.value = !_isPreviewMode.value
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
