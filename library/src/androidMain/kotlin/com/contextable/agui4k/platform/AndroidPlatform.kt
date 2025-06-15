package com.contextable.agui4k.platform

import android.os.Build
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.android.Android

/**
 * Android-specific platform implementations for ag-ui-4k.
 */
actual object Platform {
    /**
     * Returns the platform name and version.
     */
    actual val name: String = "Android ${Build.VERSION.SDK_INT}"

    /**
     * Returns the default HTTP client engine for Android.
     */
    actual fun httpClientEngine(): HttpClientEngine = Android.create()

    /**
     * Checks if the platform supports WebSockets.
     */
    actual val supportsWebSockets: Boolean = true

    /**
     * Gets the number of available processors for concurrent operations.
     */
    actual val availableProcessors: Int = Runtime.getRuntime().availableProcessors()
}

/**
 * Android-specific logging implementation.
 */
actual object PlatformLogger {
    actual fun log(level: LogLevel, tag: String, message: String, throwable: Throwable?) {
        when (level) {
            LogLevel.DEBUG -> android.util.Log.d(tag, message, throwable)
            LogLevel.INFO -> android.util.Log.i(tag, message, throwable)
            LogLevel.WARN -> android.util.Log.w(tag, message, throwable)
            LogLevel.ERROR -> android.util.Log.e(tag, message, throwable)
        }
    }
}

/**
 * Log levels for platform logging.
 */
actual enum class LogLevel {
    DEBUG,
    INFO,
    WARN,
    ERROR
}
