package com.contextable.agui4k.client.data.repository

import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PreferencesRepository(
    private val settings: Settings
) {
    private val _isDarkMode = MutableStateFlow(settings.getBoolean(KEY_DARK_MODE, false))
    val isDarkModeFlow: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    var isDarkMode: Boolean
        get() = settings.getBoolean(KEY_DARK_MODE, false)
        set(value) {
            settings.putBoolean(KEY_DARK_MODE, value)
            _isDarkMode.value = value
        }

    private val _fontSize = MutableStateFlow(settings.getFloat(KEY_FONT_SIZE, 14f))
    val fontSizeFlow: StateFlow<Float> = _fontSize.asStateFlow()

    var fontSize: Float
        get() = settings.getFloat(KEY_FONT_SIZE, 14f)
        set(value) {
            settings.putFloat(KEY_FONT_SIZE, value)
            _fontSize.value = value
        }

    var showTimestamps: Boolean
        get() = settings.getBoolean(KEY_SHOW_TIMESTAMPS, true)
        set(value) = settings.putBoolean(KEY_SHOW_TIMESTAMPS, value)

    companion object {
        private const val KEY_DARK_MODE = "dark_mode"
        private const val KEY_FONT_SIZE = "font_size"
        private const val KEY_SHOW_TIMESTAMPS = "show_timestamps"
    }
}