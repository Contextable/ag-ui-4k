package com.contextable.agui4k.platform

import io.ktor.client.engine.HttpClientEngine

/**
 * Platform-specific implementations for ag-ui-4k transport functionality.
 * Each platform must provide actual implementations of these interfaces.
 */
expect object TransportPlatform {
    /**
     * Returns the default HTTP client engine for the platform.
     */
    fun httpClientEngine(): HttpClientEngine

    /**
     * Checks if the platform supports WebSockets.
     */
    val supportsWebSockets: Boolean
}

/**
 * Checks if WebSockets are supported on the current platform.
 */
fun isWebSocketSupported(): Boolean = TransportPlatform.supportsWebSockets