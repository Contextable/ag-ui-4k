package com.contextable.agui4k.client

import com.contextable.agui4k.core.types.*
import com.contextable.agui4k.transport.*

/**
 * A simple builder for creating and configuring AG-UI clients.
 * 
 * Simplified to focus on essential transport configuration only.
 */
class ClientBuilder {
    
    private var transport: ClientTransport? = null
    private var transportConfig: HttpClientTransportConfig? = null
    
    /**
     * Configures HTTP transport with the specified URL and options.
     */
    fun httpTransport(
        url: String,
        configure: HttpClientTransportConfig.() -> HttpClientTransportConfig = { this }
    ): ClientBuilder {
        val config = HttpClientTransportConfig(url = url).configure()
        this.transportConfig = config
        this.transport = HttpClientTransport(config)
        return this
    }
    
    /**
     * Configures HTTP transport with authentication headers.
     */
    fun httpTransportWithAuth(
        url: String,
        authHeaders: Map<String, String>,
        configure: HttpClientTransportConfig.() -> HttpClientTransportConfig = { this }
    ): ClientBuilder {
        val config = HttpClientTransportConfig(
            url = url,
            headers = authHeaders
        ).configure()
        this.transportConfig = config
        this.transport = HttpClientTransport(config)
        return this
    }
    
    /**
     * Configures HTTP transport with bearer token authentication.
     */
    fun httpTransportWithBearer(
        url: String,
        token: String,
        configure: HttpClientTransportConfig.() -> HttpClientTransportConfig = { this }
    ): ClientBuilder {
        val authHeaders = mapOf("Authorization" to "Bearer $token")
        return httpTransportWithAuth(url, authHeaders, configure)
    }
    
    /**
     * Configures HTTP transport with API key authentication.
     */
    fun httpTransportWithApiKey(
        url: String,
        apiKey: String,
        headerName: String = "X-API-Key",
        configure: HttpClientTransportConfig.() -> HttpClientTransportConfig = { this }
    ): ClientBuilder {
        val authHeaders = mapOf(headerName to apiKey)
        return httpTransportWithAuth(url, authHeaders, configure)
    }
    
    /**
     * Sets a custom transport implementation.
     */
    fun transport(transport: ClientTransport): ClientBuilder {
        this.transport = transport
        return this
    }
    
    
    /**
     * Builds the agent client.
     */
    fun build(): AgentClient {
        val config = transportConfig ?: throw IllegalStateException("Transport must be configured")
        
        return AgentClient(config.url) {
            // Apply headers from transport config
            headers.putAll(config.headers)
        }
    }
}

/**
 * Convenience function to start building a client.
 */
fun clientBuilder(): ClientBuilder = ClientBuilder()

/**
 * Convenience functions for common client configurations.
 */
object Clients {
    
    /**
     * Creates a simple HTTP client.
     */
    fun simple(
        url: String, 
        authHeaders: Map<String, String> = emptyMap()
    ): AgentClient {
        return clientBuilder()
            .httpTransportWithAuth(url, authHeaders)
            .build()
    }
    
    /**
     * Creates a client with bearer token authentication.
     */
    fun withBearer(
        url: String, 
        token: String
    ): AgentClient {
        return clientBuilder()
            .httpTransportWithBearer(url, token)
            .build()
    }
    
    /**
     * Creates a client with API key authentication.
     */
    fun withApiKey(
        url: String, 
        apiKey: String,
        headerName: String = "X-API-Key"
    ): AgentClient {
        return clientBuilder()
            .httpTransportWithApiKey(url, apiKey, headerName)
            .build()
    }
}