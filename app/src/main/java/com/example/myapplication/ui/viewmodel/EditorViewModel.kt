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

private enum class TokenType {
    WORD, SYMBOL, OPERATOR, STRING, COMMENT, WHITESPACE, KEYWORD
}

private data class Token(val type: TokenType, val text: String)

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

    fun formatCode() {
        val currentText = _content.value.text
        if (currentText.isBlank()) return

        val formatted = formatKotlin(currentText)
        if (formatted != currentText) {
            onContentChange(_content.value.copy(text = formatted))
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
        val oldText = _content.value.text
        val newText = newValue.text
        if (oldText != newText) {
            undoStack.push(_content.value)
            redoStack.clear()
            _saveStatus.value = "Unsaved"
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
