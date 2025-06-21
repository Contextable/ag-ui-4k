package com.contextable.agui4k.client

import com.contextable.agui4k.core.types.*
import com.contextable.agui4k.transport.*
import kotlinx.coroutines.flow.Flow

/**
 * A fluent builder for creating and configuring AG-UI clients.
 * 
 * This builder provides a convenient way to set up clients with various
 * transport options, state management, and event processing configurations.
 */
class ClientBuilder {
    
    private var transport: ClientTransport? = null
    private var stateManager: ClientStateManager? = null
    private var statefulConfig: StatefulClientConfig? = null
    private var eventPatterns: List<Pair<EventPattern, suspend (BaseEvent, PatternContext) -> BaseEvent?>> = emptyList()
    private var eventOrderingConfig: EventOrderingConfig? = null
    
    /**
     * Configures HTTP transport with the specified URL and options.
     */
    fun httpTransport(
        url: String,
        configure: HttpClientTransportConfig.() -> HttpClientTransportConfig = { this }
    ): ClientBuilder {
        val config = HttpClientTransportConfig(url = url).configure()
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
     * Configures in-memory state management (default).
     */
    fun inMemoryState(): ClientBuilder {
        this.stateManager = InMemoryClientStateManager()
        return this
    }
    
    /**
     * Sets a custom state manager.
     */
    fun stateManager(stateManager: ClientStateManager): ClientBuilder {
        this.stateManager = stateManager
        return this
    }
    
    /**
     * Configures stateful client behavior.
     */
    fun statefulConfig(
        contextStrategy: ContextStrategy = ContextStrategy.SINGLE_MESSAGE,
        maxHistoryMessages: Int? = null
    ): ClientBuilder {
        this.statefulConfig = StatefulClientConfig(
            contextStrategy = contextStrategy,
            maxHistoryMessages = maxHistoryMessages
        )
        return this
    }
    
    /**
     * Adds event patterns for processing.
     */
    fun eventPatterns(
        patterns: List<Pair<EventPattern, suspend (BaseEvent, PatternContext) -> BaseEvent?>>
    ): ClientBuilder {
        this.eventPatterns = patterns
        return this
    }
    
    /**
     * Adds a single event pattern.
     */
    fun eventPattern(
        pattern: EventPattern,
        processor: suspend (BaseEvent, PatternContext) -> BaseEvent?
    ): ClientBuilder {
        this.eventPatterns = this.eventPatterns + (pattern to processor)
        return this
    }
    
    /**
     * Configures event ordering.
     */
    fun eventOrdering(
        maxBufferSize: Int = 1000,
        maxWaitTimeMs: Long = 5000L,
        strictOrdering: Boolean = false
    ): ClientBuilder {
        this.eventOrderingConfig = EventOrderingConfig(
            maxBufferSize = maxBufferSize,
            maxWaitTimeMs = maxWaitTimeMs,
            strictOrdering = strictOrdering
        )
        return this
    }
    
    /**
     * Builds a stateless client.
     */
    fun buildStateless(): StatelessClient {
        val finalTransport = transport ?: throw IllegalStateException("Transport must be configured")
        return StatelessClient(finalTransport)
    }
    
    /**
     * Builds a stateful client.
     */
    fun buildStateful(): StatefulClient {
        val finalTransport = transport ?: throw IllegalStateException("Transport must be configured")
        val finalStateManager = stateManager ?: InMemoryClientStateManager()
        val finalConfig = statefulConfig ?: StatefulClientConfig()
        
        return StatefulClient(finalTransport, finalStateManager, finalConfig)
    }
    
    /**
     * Builds a pattern-aware client that wraps another client with event processing.
     */
    fun buildPatternAware(): PatternAwareClient {
        val baseClient = buildStateful() // Pattern-aware clients are typically stateful
        return PatternAwareClient(baseClient, eventPatterns, eventOrderingConfig)
    }
}

/**
 * A client wrapper that adds pattern-based event processing to any base client.
 */
class PatternAwareClient(
    private val baseClient: AbstractClient,
    private val patterns: List<Pair<EventPattern, suspend (BaseEvent, PatternContext) -> BaseEvent?>>,
    private val orderingConfig: EventOrderingConfig? = null
) {
    
    private val patternProcessor = if (patterns.isNotEmpty()) {
        PatternAwareEventProcessor(patterns)
    } else null
    
    private val orderingProcessor = orderingConfig?.let { 
        EventOrderingProcessor(it) 
    }
    
    /**
     * Starts a conversation with pattern-aware event processing.
     */
    suspend fun startConversation(
        message: Message,
        threadId: String? = null
    ): Flow<BaseEvent> {
        var eventFlow = baseClient.startConversation(message, threadId)
        
        // Apply pattern processing if configured
        patternProcessor?.let { processor ->
            eventFlow = processor.processEvents(eventFlow)
        }
        
        // Apply event ordering if configured
        orderingProcessor?.let { processor ->
            eventFlow = processor.processEvents(eventFlow)
        }
        
        return eventFlow
    }
    
    /**
     * Delegates to the underlying stateful client if available.
     */
    suspend fun continueConversation(
        content: String,
        threadId: String? = null,
        systemContext: String? = null,
        contextStrategy: ContextStrategy? = null
    ): Flow<BaseEvent> {
        if (baseClient is StatefulClient) {
            var eventFlow = baseClient.continueConversation(content, threadId, systemContext, contextStrategy)
            
            // Apply pattern processing if configured
            patternProcessor?.let { processor ->
                eventFlow = processor.processEvents(eventFlow)
            }
            
            // Apply event ordering if configured
            orderingProcessor?.let { processor ->
                eventFlow = processor.processEvents(eventFlow)
            }
            
            return eventFlow
        } else {
            throw UnsupportedOperationException("Base client does not support stateful operations")
        }
    }
    
    /**
     * Provides access to the underlying client for advanced operations.
     */
    fun getBaseClient(): AbstractClient = baseClient
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
     * Creates a simple stateless HTTP client.
     */
    fun stateless(url: String, authHeaders: Map<String, String> = emptyMap()): StatelessClient {
        return clientBuilder()
            .httpTransportWithAuth(url, authHeaders)
            .buildStateless()
    }
    
    /**
     * Creates a stateful HTTP client with in-memory state.
     */
    fun stateful(
        url: String, 
        authHeaders: Map<String, String> = emptyMap(),
        contextStrategy: ContextStrategy = ContextStrategy.SINGLE_MESSAGE
    ): StatefulClient {
        return clientBuilder()
            .httpTransportWithAuth(url, authHeaders)
            .inMemoryState()
            .statefulConfig(contextStrategy)
            .buildStateful()
    }
    
    /**
     * Creates a stateful HTTP client with bearer token authentication.
     */
    fun statefulWithBearer(
        url: String, 
        token: String,
        contextStrategy: ContextStrategy = ContextStrategy.SINGLE_MESSAGE
    ): StatefulClient {
        return clientBuilder()
            .httpTransportWithBearer(url, token)
            .inMemoryState()
            .statefulConfig(contextStrategy)
            .buildStateful()
    }
    
    /**
     * Creates a pattern-aware client with common event processing patterns.
     */
    fun patternAware(
        url: String, 
        authHeaders: Map<String, String> = emptyMap()
    ): PatternAwareClient {
        return clientBuilder()
            .httpTransportWithAuth(url, authHeaders)
            .inMemoryState()
            .eventPattern(EventPatterns.textMessageStart()) { event, _ -> event }
            .eventPattern(EventPatterns.anyToolCallCompletion()) { event, _ -> event }
            .eventOrdering(maxBufferSize = 500, maxWaitTimeMs = 3000L)
            .buildPatternAware()
    }
}