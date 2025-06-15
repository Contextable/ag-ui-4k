package com.contextable.agui4k.platform

import io.ktor.client.engine.HttpClientEngine

/**
 * Platform-specific implementations for ag-ui-4k.
 * Each platform must provide actual implementations of these interfaces.
 */
expect object Platform {
    /**
     * Returns the platform name and version.
     */
    val name: String

    /**
     * Returns the default HTTP client engine for the platform.
     */
    fun httpClientEngine(): HttpClientEngine

    /**
     * Checks if the platform supports WebSockets.
     */
    val supportsWebSockets: Boolean

    /**
     * Gets the number of available processors for concurrent operations.
     */
    val availableProcessors: Int
}

/**
 * Platform-specific logging implementation.
 */
expect object PlatformLogger {
    /**
     * Logs a message with the specified level, tag, and optional throwable.
     */
    fun log(level: LogLevel, tag: String, message: String, throwable: Throwable? = null)
}

/**
 * Log levels for platform logging.
 */
expect enum class LogLevel {
    DEBUG,
    INFO,
    WARN,
    ERROR
}

/**
 * Convenience logging functions.
 */
object Logger {
    fun debug(tag: String, message: String, throwable: Throwable? = null) {
        PlatformLogger.log(LogLevel.DEBUG, tag, message, throwable)
    }

    fun info(tag: String, message: String, throwable: Throwable? = null) {
        PlatformLogger.log(LogLevel.INFO, tag, message, throwable)
    }

    fun warn(tag: String, message: String, throwable: Throwable? = null) {
        PlatformLogger.log(LogLevel.WARN, tag, message, throwable)
    }

    fun error(tag: String, message: String, throwable: Throwable? = null) {
        PlatformLogger.log(LogLevel.ERROR, tag, message, throwable)
    }
}

/**
 * Gets the current platform information.
 */
fun currentPlatform(): String = Platform.name

/**
 * Checks if WebSockets are supported on the current platform.
 */
fun isWebSocketSupported(): Boolean = Platform.supportsWebSockets
