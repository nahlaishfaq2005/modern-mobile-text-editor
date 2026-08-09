package com.example.myapplication.ui.components.editor

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle

object SyntaxHighlighter {
    private val kotlinKeywords = setOf(
        "package", "import", "class", "interface", "fun", "val", "var",
        "if", "else", "when", "for", "while", "return", "object",
        "override", "private", "public", "internal", "protected",
        "data", "sealed", "enum", "in", "is", "as", "null", "true", "false",
        "this", "super", "typealias", "constructor", "init", "companion"
    )

    private val colors = object {
        val keyword = Color(0xFFA56F63) // PrimaryAccent
        val string = Color(0xFFD99B7F) // SecondaryAccent
        val comment = Color(0xFF9AA2AA) // TextHint
        val annotation = Color(0xFF8B5CF6) // Purple
        val type = Color(0xFF64B5F6) // Light Blue
        val number = Color(0xFFCE93D8) // Light Purple
    }

    fun highlightKotlin(text: String): AnnotatedString {
        return buildAnnotatedString {
            val words = text.split(Regex("(?=[^a-zA-Z0-9_])|(?<=[^a-zA-Z0-9_])"))
            var currentPos = 0
            
            // Very basic regex-based highlighting for strings and comments first might be better
            // but for simplicity let's stick to this or use a more robust regex approach.
            
            val regex = Regex(
                "(//.*)|(/\\*.*?\\*/)|(\".*?\")|(@[a-zA-Z0-9_]+)|(\\b[0-9]+\\b)|(\\b[a-zA-Z_][a-zA-Z0-9_]*\\b)",
                RegexOption.DOT_MATCHES_ALL
            )
            
            var lastMatchEnd = 0
            regex.findAll(text).forEach { match ->
                append(text.substring(lastMatchEnd, match.range.first))
                
                val matchText = match.value
                when {
                    matchText.startsWith("//") || matchText.startsWith("/*") -> {
                        withStyle(SpanStyle(color = colors.comment)) { append(matchText) }
                    }
                    matchText.startsWith("\"") -> {
                        withStyle(SpanStyle(color = colors.string)) { append(matchText) }
                    }
                    matchText.startsWith("@") -> {
                        withStyle(SpanStyle(color = colors.annotation)) { append(matchText) }
                    }
                    matchText.all { it.isDigit() } -> {
                        withStyle(SpanStyle(color = colors.number)) { append(matchText) }
                    }
                    kotlinKeywords.contains(matchText) -> {
                        withStyle(SpanStyle(color = colors.keyword, fontWeight = FontWeight.Bold)) { append(matchText) }
                    }
                    matchText.firstOrNull()?.isUpperCase() == true -> {
                        withStyle(SpanStyle(color = colors.type)) { append(matchText) }
                    }
                    else -> append(matchText)
                }
                lastMatchEnd = match.range.last + 1
            }
            append(text.substring(lastMatchEnd))
        }
    }

    fun highlightMarkdown(text: String): AnnotatedString {
        return buildAnnotatedString {
            val regex = Regex(
                "(^#+.*$)|(\\*\\*.*?\\*\\*)|(\\*.*?\\*)|(\\[.*?\\]\\(.*?\\))|(^\\s*[-*+]\\s+.*$)",
                RegexOption.MULTILINE
            )
            
            var lastMatchEnd = 0
            regex.findAll(text).forEach { match ->
                append(text.substring(lastMatchEnd, match.range.first))
                
                val matchText = match.value
                when {
                    matchText.startsWith("#") -> {
                        withStyle(SpanStyle(color = colors.keyword, fontWeight = FontWeight.Bold)) { append(matchText) }
                    }
                    matchText.startsWith("**") -> {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(matchText) }
                    }
                    matchText.startsWith("*") -> {
                        withStyle(SpanStyle(fontWeight = FontWeight.Medium)) { append(matchText) }
                    }
                    matchText.startsWith("[") -> {
                        withStyle(SpanStyle(color = colors.type)) { append(matchText) }
                    }
                    matchText.trim().startsWith("-") || matchText.trim().startsWith("*") || matchText.trim().startsWith("+") -> {
                        withStyle(SpanStyle(color = colors.annotation)) { append(matchText) }
                    }
                    else -> append(matchText)
                }
                lastMatchEnd = match.range.last + 1
            }
            append(text.substring(lastMatchEnd))
        }
    }
}
