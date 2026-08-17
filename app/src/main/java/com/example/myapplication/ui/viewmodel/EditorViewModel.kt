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

private enum class TokenType {
    WORD, SYMBOL, OPERATOR, STRING, COMMENT, WHITESPACE, KEYWORD
}

private data class Token(val type: TokenType, val text: String)

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
    
    private var lastSavedContent: String = "" // Track content to avoid redundant saves and versions
    
    // UI state for Save As dialog
    private val _showSaveAsDialog = MutableStateFlow(false)
    val showSaveAsDialog: StateFlow<Boolean> = _showSaveAsDialog.asStateFlow()

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

    private val _isReadOnly = MutableStateFlow(false)
    val isReadOnly: StateFlow<Boolean> = _isReadOnly.asStateFlow()

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
        lastSavedContent = loadedContent
        undoStack.clear()
        redoStack.clear()
        _saveStatus.value = "Saved"
        startRecoveryJob()
        // Reset read-only state when a new file is loaded
        _isReadOnly.value = false
    }

    fun toggleReadOnly() {
        _isReadOnly.value = !_isReadOnly.value
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
            if (isModified) {
                recoveryManager.cacheActiveFile(
                    fileName = _currentFileName.value,
                    content = _content.value.text,
                    isModified = isModified
                )
            }
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

        // If no changes, skip saving and version creation
        if (_saveStatus.value == "Saved") {
            return
        }

        // Now createVersion handles physical save, recovery cleanup, and status updates
        createVersion(fileName, "Manually saved", false)
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
            lastSavedContent = _content.value.text
            
            // Task 4: Add the new name to recent files
            val type = getFileTypeFromName(newName)
            recentFileRepository.addRecentFile(newName, type, newName)
            
            // Create initial version for the new file
            createVersion(newName, "Initial version (Save As)", false)
            
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
            val currentText = _content.value.text
            val result = versionManager.createVersion(fileName, currentText, name, isAutoSaved)
            
            // If it's a manual/named version (not autosave), we must ensure the physical file 
            // is synced and recovery cache is cleared, even if the version was a duplicate.
            if (!isAutoSaved) {
                val success = repository.saveFile(fileName, currentText)
                if (success) {
                    _saveStatus.value = "Saved"
                    lastSavedContent = currentText
                    recoveryManager.deleteRecoveryFile(fileName)
                    
                    // Update recent files
                    val type = getFileTypeFromName(fileName)
                    recentFileRepository.addRecentFile(fileName, type, fileName)
                } else {
                    _saveStatus.value = "Error Saving"
                }
            }

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

    fun formatCode() {
        val currentText = _content.value.text
        if (currentText.isBlank()) return

        val formatted = formatKotlin(currentText)
        if (formatted != currentText) {
            onContentChange(_content.value.copy(text = formatted))
        }
    }

    fun formatContent(fileType: String) {
        if (_isLocked.value || _isReadOnly.value) return
        if (fileType == "Kotlin") {
            val formatted = formatKotlin(_content.value.text)
            if (formatted != _content.value.text) {
                onContentChange(_content.value.copy(text = formatted))
            }
        }
    }

    private fun formatKotlin(code: String): String {
        try {
            val result = StringBuilder()
            var indentLevel = 0
            val tokens = getTokens(code)
            
            var i = 0
            while (i < tokens.size) {
                val token = tokens[i]
                val type = token.type
                val text = token.text

                when (type) {
                    TokenType.KEYWORD -> {
                        val topLevelKeywords = setOf("fun", "class", "object", "interface", "typealias")
                        if (indentLevel == 0 && topLevelKeywords.contains(text) && result.isNotEmpty() && !result.endsWith("\n\n")) {
                            if (!result.endsWith("\n")) result.append("\n")
                            result.append("\n")
                        }
                        
                        if (result.isNotEmpty() && !result.endsWith(" ") && !result.endsWith("\n") && !result.endsWith("(")) {
                            result.append(" ")
                        }
                        result.append(text)
                        val next = nextNonSpace(tokens, i)
                        if (next != null && next.text == "(") {
                            result.append(" ")
                        }
                    }
                    TokenType.SYMBOL -> {
                        when (text) {
                            "{" -> {
                                if (result.isNotEmpty() && !result.endsWith(" ") && !result.endsWith("\n")) {
                                    result.append(" ")
                                }
                                result.append("{\n")
                                indentLevel++
                                result.append("    ".repeat(indentLevel))
                            }
                            "}" -> {
                                if (result.endsWith("    ")) {
                                    result.setLength(result.length - 4)
                                }
                                if (!result.endsWith("\n")) {
                                    result.append("\n")
                                }
                                
                                indentLevel = (indentLevel - 1).coerceAtLeast(0)
                                if (!result.endsWith("    ".repeat(indentLevel))) {
                                    result.append("    ".repeat(indentLevel))
                                }
                                result.append("}")
                                
                                val next = nextNonSpace(tokens, i)
                                if (next != null && (next.text == "else" || next.text == "catch" || next.text == "finally")) {
                                    result.append(" ")
                                } else {
                                    result.append("\n")
                                    result.append("    ".repeat(indentLevel))
                                }
                            }
                            ";" -> {
                                // Semicolons are mostly optional in Kotlin. We can convert them to newlines.
                                val next = nextNonSpace(tokens, i)
                                if (next != null && next.text != "}") {
                                    if (!result.endsWith("\n")) result.append("\n")
                                    result.append("    ".repeat(indentLevel))
                                }
                            }
                            "," -> {
                                result.append(", ")
                            }
                            ":" -> {
                                // Simple heuristic for types and named arguments
                                result.append(": ")
                            }
                            else -> result.append(text)
                        }
                    }
                    TokenType.OPERATOR -> {
                        val noSpaceOps = setOf(".", "!!", "::", "?.", "?:", "?.")
                        if (noSpaceOps.contains(text)) {
                            result.append(text)
                        } else if (text == "<" || text == ">") {
                            // Heuristic for generics vs comparison
                            val prev = lastNonSpaceToken(tokens, i)
                            val next = nextNonSpace(tokens, i)
                            if (prev != null && prev.type == TokenType.WORD && prev.text.firstOrNull()?.isUpperCase() == true) {
                                // Likely generic
                                result.append(text)
                            } else {
                                if (result.isNotEmpty() && !result.endsWith(" ") && !result.endsWith("\n")) result.append(" ")
                                result.append(text)
                                if (next != null && !next.text.startsWith(" ")) result.append(" ")
                            }
                        } else {
                            if (result.isNotEmpty() && !result.endsWith(" ") && !result.endsWith("\n")) {
                                result.append(" ")
                            }
                            result.append(text)
                            val next = nextNonSpace(tokens, i)
                            if (next != null && next.text != ";" && next.text != "," && next.text != ")") {
                                result.append(" ")
                            }
                        }
                    }
                    TokenType.STRING, TokenType.COMMENT, TokenType.WORD -> {
                        if (type == TokenType.WORD && result.isNotEmpty() && !result.endsWith(" ") && !result.endsWith("\n") && !result.endsWith("(") && !result.endsWith(".") && !result.endsWith("?") && !result.endsWith("!")) {
                            // Check if current text is a number and previous is an operator like -
                            result.append(" ")
                        }
                        result.append(text)
                        if (type == TokenType.COMMENT && text.startsWith("//")) {
                            result.append("\n")
                            result.append("    ".repeat(indentLevel))
                        }
                    }
                    TokenType.WHITESPACE -> {
                        if (text.contains("\n\n")) {
                            result.append("\n\n")
                            result.append("    ".repeat(indentLevel))
                        }
                    }
                }
                i++
            }

            return finalizeFormatting(result.toString())
        } catch (e: Exception) {
            return code
        }
    }

    private fun lastNonSpaceToken(tokens: List<Token>, current: Int): Token? {
        for (j in current - 1 downTo 0) {
            if (tokens[j].type != TokenType.WHITESPACE) return tokens[j]
        }
        return null
    }

    private fun nextNonSpace(tokens: List<Token>, current: Int): Token? {
        for (j in current + 1 until tokens.size) {
            if (tokens[j].type != TokenType.WHITESPACE) return tokens[j]
        }
        return null
    }

    private fun finalizeFormatting(code: String): String {
        return code.lines()
            .map { it.trimEnd() }
            .filterIndexed { index, line -> 
                // Remove lines that are just whitespace introduced by our logic but don't follow a { or ;
                line.isNotBlank() || (index > 0 && code.lines()[index-1].isBlank())
            }
            .joinToString("\n")
            .trim()
    }

    private fun getTokens(code: String): List<Token> {
        val tokens = mutableListOf<Token>()
        var i = 0
        val keywords = setOf("package", "import", "class", "interface", "object", "fun", "val", "var", "if", "else", "for", "while", "do", "when", "try", "catch", "finally", "return", "throw", "break", "continue", "as", "is", "in", "this", "super", "typealias", "typeof")

        while (i < code.length) {
            val c = code[i]
            
            // Whitespace
            if (c.isWhitespace()) {
                val start = i
                while (i < code.length && code[i].isWhitespace()) i++
                tokens.add(Token(TokenType.WHITESPACE, code.substring(start, i)))
                continue
            }

            // String literals
            if (c == '\"') {
                val start = i
                if (i + 2 < code.length && code[i+1] == '\"' && code[i+2] == '\"') {
                    i += 3
                    while (i + 2 < code.length && !(code[i] == '\"' && code[i+1] == '\"' && code[i+2] == '\"')) i++
                    i += 3
                } else {
                    i++
                    while (i < code.length && code[i] != '\"') {
                        if (code[i] == '\\' && i + 1 < code.length) i++
                        i++
                    }
                    i++
                }
                tokens.add(Token(TokenType.STRING, code.substring(start, i.coerceAtMost(code.length))))
                continue
            }

            // Comments
            if (c == '/' && i + 1 < code.length) {
                if (code[i+1] == '/') {
                    val start = i
                    while (i < code.length && code[i] != '\n') i++
                    tokens.add(Token(TokenType.COMMENT, code.substring(start, i)))
                    continue
                } else if (code[i+1] == '*') {
                    val start = i
                    i += 2
                    while (i + 1 < code.length && !(code[i] == '*' && code[i+1] == '/')) i++
                    i += 2
                    tokens.add(Token(TokenType.COMMENT, code.substring(start, i.coerceAtMost(code.length))))
                    continue
                }
            }

            // Symbols
            if ("{}()[];,.:".contains(c)) {
                var text = c.toString()
                if (c == '?' && i + 1 < code.length && ".:".contains(code[i+1])) {
                    // Handled as operator
                } else if (c == ':' && i + 1 < code.length && code[i+1] == ':') {
                    text = "::"
                    i++
                } else {
                    tokens.add(Token(TokenType.SYMBOL, text))
                    i++
                    continue
                }
            }

            // Operators
            val operators = "+-*/%=!><&|?->"
            if (operators.contains(c)) {
                val start = i
                i++
                while (i < code.length && operators.contains(code[i])) {
                    // Avoid merging -> with other ops if necessary, but Kotlin has many multi-char ops
                    i++
                }
                val opText = code.substring(start, i)
                tokens.add(Token(TokenType.OPERATOR, opText))
                continue
            }

            // Words (Identifiers, Keywords, Numbers)
            if (c.isLetterOrDigit() || c == '_' || c == '`') {
                val start = i
                if (c == '`') {
                    i++
                    while (i < code.length && code[i] != '`') i++
                    i++
                } else {
                    while (i < code.length && (code[i].isLetterOrDigit() || code[i] == '_')) i++
                }
                val word = code.substring(start, i)
                if (keywords.contains(word)) {
                    tokens.add(Token(TokenType.KEYWORD, word))
                } else {
                    tokens.add(Token(TokenType.WORD, word))
                }
                continue
            }

            // Any other char
            tokens.add(Token(TokenType.SYMBOL, c.toString()))
            i++
        }
        return tokens
    }

    fun onContentChange(newValue: TextFieldValue) {
        if (_isLocked.value || _isReadOnly.value) return
        val oldText = _content.value.text
        val newText = newValue.text
        if (oldText != newText) {
            undoStack.push(_content.value)
            redoStack.clear()
            
            // Check if it matches last saved state
            if (newText == lastSavedContent) {
                _saveStatus.value = "Saved"
            } else {
                _saveStatus.value = "Unsaved Changes"
            }
            
            _content.value = newValue
            
            if (_showSearchReplace.value) {
                updateSearchResults(_searchQuery.value)
            }
        } else {
            _content.value = newValue
        }
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
            val popped = undoStack.pop()
            _content.value = popped
            
            if (popped.text == lastSavedContent) {
                _saveStatus.value = "Saved"
            } else {
                _saveStatus.value = "Unsaved Changes"
            }
        }
    }

    fun redo() {
        if (redoStack.isNotEmpty()) {
            undoStack.push(_content.value)
            val popped = redoStack.pop()
            _content.value = popped
            
            if (popped.text == lastSavedContent) {
                _saveStatus.value = "Saved"
            } else {
                _saveStatus.value = "Unsaved Changes"
            }
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
