package com.contextable.agui4k.platform

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.Darwin

/**
 * iOS-specific transport platform implementations for ag-ui-4k.
 */
actual object TransportPlatform {
    /**
     * Returns the default HTTP client engine for iOS.
     */
    actual fun httpClientEngine(): HttpClientEngine = Darwin.create()

    /**
     * Checks if the platform supports WebSockets.
     */
    actual val supportsWebSockets: Boolean = true
}