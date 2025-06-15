package com.contextable.agui4k.platform

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.Darwin
import platform.Foundation.NSLog
import platform.Foundation.NSProcessInfo
import platform.UIKit.UIDevice

/**
 * iOS-specific platform implementations for ag-ui-4k.
 */
actual object Platform {
    /**
     * Returns the platform name and version.
     */
    actual val name: String = UIDevice.currentDevice.let {
        "${it.systemName()} ${it.systemVersion()}"
    }

    /**
     * Returns the default HTTP client engine for iOS.
     */
    actual fun httpClientEngine(): HttpClientEngine = Darwin.create()

    /**
     * Checks if the platform supports WebSockets.
     */
    actual val supportsWebSockets: Boolean = true

    /**
     * Gets the number of available processors for concurrent operations.
     */
    actual val availableProcessors: Int = NSProcessInfo.processInfo.processorCount.toInt()
}

/**
 * iOS-specific logging implementation using NSLog.
 */
actual object PlatformLogger {
    actual fun log(level: LogLevel, tag: String, message: String, throwable: Throwable?) {
        val logMessage = buildString {
            append("[${level.name}] ")
            append("$tag: ")
            append(message)
            throwable?.let {
                append("\n")
                append(it.stackTraceToString())
            }
        }

        NSLog("%@", logMessage)
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
