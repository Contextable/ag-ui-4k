package com.contextable.agui4k.platform

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.cio.CIO
import org.slf4j.LoggerFactory

/**
 * JVM-specific platform implementations for ag-ui-4k.
 */
actual object Platform {
    /**
     * Returns the platform name and version.
     */
    actual val name: String = "JVM ${System.getProperty("java.version")}"

    /**
     * Returns the default HTTP client engine for JVM.
     */
    actual fun httpClientEngine(): HttpClientEngine = CIO.create()

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
 * JVM-specific logging implementation using SLF4J.
 */
actual object PlatformLogger {
    actual fun log(level: LogLevel, tag: String, message: String, throwable: Throwable?) {
        val logger = LoggerFactory.getLogger(tag)

        when (level) {
            LogLevel.DEBUG -> logger.debug(message, throwable)
            LogLevel.INFO -> logger.info(message, throwable)
            LogLevel.WARN -> logger.warn(message, throwable)
            LogLevel.ERROR -> logger.error(message, throwable)
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
