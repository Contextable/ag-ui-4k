package com.contextable.agui4k.client.util

import android.content.Context
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings

private lateinit var appContext: Context

fun initializeAndroid(context: Context) {
    appContext = context.applicationContext
}

actual fun getPlatformSettings(): Settings {
    val sharedPreferences = appContext.getSharedPreferences("agui4k_prefs", Context.MODE_PRIVATE)
    return SharedPreferencesSettings(sharedPreferences)
}

actual fun getPlatformName(): String = "Android"
