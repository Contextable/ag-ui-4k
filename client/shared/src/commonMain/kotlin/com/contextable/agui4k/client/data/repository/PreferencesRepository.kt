package com.contextable.agui4k.client.data.repository

import com.russhwolf.settings.Settings
import com.russhwolf.settings.coroutines.FlowSettings
import com.russhwolf.settings.coroutines.toFlowSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PreferencesRepository(
    private val settings: Settings
) {
    private val flowSettings: FlowSettings = settings.toFlowSettings()
    
    var isDarkMode: Boolean
        get() = settings.getBoolean(KEY_DARK_MODE, false)
        set(value) = settings.putBoolean(KEY_DARK_MODE, value)
    
    val isDarkModeFlow: Flow<Boolean> = flowSettings
        .getBooleanFlow(KEY_DARK_MODE, false)
    
    var fontSize: Float
        get() = settings.getFloat(KEY_FONT_SIZE, 14f)
        set(value) = settings.putFloat(KEY_FONT_SIZE, value)
    
    val fontSizeFlow: Flow<Float> = flowSettings
        .getFloatFlow(KEY_FONT_SIZE, 14f)
    
    var showTimestamps: Boolean
        get() = settings.getBoolean(KEY_SHOW_TIMESTAMPS, true)
        set(value) = settings.putBoolean(KEY_SHOW_TIMESTAMPS, value)
    
    companion object {
        private const val KEY_DARK_MODE = "dark_mode"
        private const val KEY_FONT_SIZE = "font_size"
        private const val KEY_SHOW_TIMESTAMPS = "show_timestamps"
    }
}