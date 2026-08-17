package com.example.myapplication.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val sharedPreferences = application.getSharedPreferences("settings", Context.MODE_PRIVATE)

    private val _theme = MutableStateFlow(sharedPreferences.getString("theme", "Dark") ?: "Dark")
    val theme: StateFlow<String> = _theme.asStateFlow()

    private val _fontSize = MutableStateFlow(sharedPreferences.getInt("font_size", 14))
    val fontSize: StateFlow<Int> = _fontSize.asStateFlow()

    private val _fontFamily = MutableStateFlow(sharedPreferences.getString("font_family", "JetBrains Mono") ?: "JetBrains Mono")
    val fontFamily: StateFlow<String> = _fontFamily.asStateFlow()

    private val _isWordWrapEnabled = MutableStateFlow(sharedPreferences.getBoolean("word_wrap", true))
    val isWordWrapEnabled: StateFlow<Boolean> = _isWordWrapEnabled.asStateFlow()

    private val _autosaveInterval = MutableStateFlow(sharedPreferences.getLong("autosave_interval", 10))
    val autosaveInterval: StateFlow<Long> = _autosaveInterval.asStateFlow()

    fun setTheme(theme: String) {
        _theme.value = theme
        sharedPreferences.edit().putString("theme", theme).apply()
    }

    fun setFontSize(size: Int) {
        _fontSize.value = size
        sharedPreferences.edit().putInt("font_size", size).apply()
    }

    fun setFontFamily(family: String) {
        _fontFamily.value = family
        sharedPreferences.edit().putString("font_family", family).apply()
    }

    fun setWordWrap(enabled: Boolean) {
        _isWordWrapEnabled.value = enabled
        sharedPreferences.edit().putBoolean("word_wrap", enabled).apply()
    }

    fun setAutosaveInterval(seconds: Long) {
        _autosaveInterval.value = seconds
        sharedPreferences.edit().putLong("autosave_interval", seconds).apply()
    }
}
