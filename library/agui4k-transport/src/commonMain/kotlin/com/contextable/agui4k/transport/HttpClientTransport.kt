package com.contextable.agui4k.transport

import com.contextable.agui4k.core.types.*
import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.plugins.sse.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.*
import kotlinx.datetime.Clock
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import mu.KotlinLogging
import kotlin.random.Random

private val logger = KotlinLogging.logger {}

/**
 * Configuration for HTTP client transport.
 */
data class HttpClientTransportConfig(
    val url: String,
    val headers: Map<String, String> = emptyMap(),
    val requestTimeoutMillis: Long = 600_000L, // 10 minutes
    val connectTimeoutMillis: Long = 30_000L,  // 30 seconds
    val socketTimeoutMillis: Long = 600_000L,  // 10 minutes
    val retryPolicy: RetryPolicy = ExponentialBackoffRetryPolicy()
)

/**
 * HTTP client transport implementation using Server-Sent Events (SSE) for streaming.
 * 
 * This transport supports:
 * - Streaming responses via SSE
 * - Regular JSON responses (fallback)
 * - Custom headers for authentication
 * - Configurable timeouts
 * - Automatic error handling and retry logic
 */
class HttpClientTransport(
    private val config: HttpClientTransportConfig
) : ClientTransport {
    
    private val json = AgUiJson
    
    private val httpClient = HttpClient {
        install(ContentNegotiation) {
            json(this@HttpClientTransport.json)
        }
        
        install(Logging) {
            logger = Logger.DEFAULT
            level = LogLevel.INFO
        }
        
        install(SSE)
        
        install(HttpTimeout) {
            requestTimeoutMillis = config.requestTimeoutMillis
            connectTimeoutMillis = config.connectTimeoutMillis
            socketTimeoutMillis = config.socketTimeoutMillis
        }
        
        defaultRequest {
            headers {
                config.headers.forEach { (key, value) ->
                    append(key, value)
                }
            }
        }
    }
    
    override suspend fun startRun(
        messages: List<Message>, 
        threadId: String?, 
        runId: String?, 
        state: JsonElement?,
        tools: List<Tool>?,
        context: List<Context>?,
        forwardedProps: JsonElement?
    ): RunSession {
        // Use provided thread ID or generate a new one
        val finalThreadId = threadId ?: generateThreadId()
        
        // Create RunAgentInput with all parameters
        val input = RunAgentInput(
            messages = messages,
            threadId = finalThreadId,
            runId = runId ?: "run_${Clock.System.now().toEpochMilliseconds()}_${Random.nextInt(1000, 9999)}", // Use provided runId or generate one
            state = state ?: JsonObject(emptyMap()),
            tools = tools ?: emptyList(),
            context = context ?: emptyList(),
            forwardedProps = forwardedProps ?: JsonObject(emptyMap())
        )
        
        return HttpRunSession(httpClient, config.url, input, json, config.retryPolicy)
    }
    
    private fun generateThreadId(): String = "thread_${Clock.System.now().toEpochMilliseconds()}"
    private fun generateRunId(): String = "run_${Clock.System.now().toEpochMilliseconds()}"
}

/**
 * HTTP-based run session implementation.
 */
private class HttpRunSession(
    private val httpClient: HttpClient,
    private val url: String,
    private val initialInput: RunAgentInput,
    private val json: Json,
    private val retryPolicy: RetryPolicy
) : RunSession {
    
    private val _isActive = MutableStateFlow(true)
    override val isActive: StateFlow<Boolean> = _isActive.asStateFlow()
    
    private var currentJob: Job? = null
    private val eventChannel = Channel<BaseEvent>(Channel.UNLIMITED)
    
    override val events: Flow<BaseEvent> = channelFlow {
        currentJob = coroutineContext.job
        
        try {
            // Start the run with retry logic
            retryWithPolicy(retryPolicy) { attemptNumber ->
                if (attemptNumber > 1) {
                    logger.info { "Retrying HTTP request (attempt $attemptNumber)" }
                }
                executeHttpRequest(initialInput)
            }
            
            // Emit all events from the channel
            for (event in eventChannel) {
                send(event)
                
                // Check for run completion
                when (event) {
                    is RunFinishedEvent, is RunErrorEvent -> {
                        _isActive.value = false
                        break
                    }
                    else -> {}
                }
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            logger.debug { "Run cancelled" }
            throw e
        } catch (e: Exception) {
            logger.error(e) { "Error during run: ${e.message}" }
            _isActive.value = false
            
            // Emit error event if we haven't already
            try {
                send(RunErrorEvent(
                    message = e.message ?: "Unknown error",
                    code = when (e) {
                        is HttpRequestTimeoutException -> "TIMEOUT_ERROR"
                        is SerializationException -> "SERIALIZATION_ERROR"
                        is TransportTimeoutException -> "TIMEOUT_ERROR"
                        is TransportConnectionException -> "CONNECTION_ERROR"
                        is TransportParsingException -> "PARSING_ERROR"
                        is HttpClientTransportException -> "HTTP_ERROR"
                        is TransportRetryExhaustedException -> "RETRY_EXHAUSTED"
                        else -> "TRANSPORT_ERROR"
                    }
                ))
            } catch (_: Exception) {
                // Channel might be closed
            }
            throw e
        } finally {
            _isActive.value = false
            eventChannel.close()
            currentJob = null
        }
    }
    
    override suspend fun sendMessage(message: Message) {
        if (!isActive.value) {
            throw RunSessionClosedException("Cannot send message: run session is not active")
        }
        
        // For tool messages, we need to send them as part of the ongoing conversation
        // This creates a new request with the additional message
        val input = RunAgentInput(
            messages = listOf(message),
            threadId = initialInput.threadId,
            runId = initialInput.runId
        )
        
        try {
            retryWithPolicy(retryPolicy) { attemptNumber ->
                if (attemptNumber > 1) {
                    logger.info { "Retrying send message (attempt $attemptNumber)" }
                }
                executeHttpRequest(input)
            }
        } catch (e: Exception) {
            logger.error(e) { "Failed to send message: ${e.message}" }
            throw e
        }
    }
    
    override suspend fun close() {
        if (_isActive.value) {
            _isActive.value = false
            currentJob?.cancel()
            eventChannel.close()
        }
    }
    
    private suspend fun executeHttpRequest(input: RunAgentInput) {
        try {
            httpClient.sse(
                urlString = this@HttpRunSession.url,
                request = {
                    method = HttpMethod.Post
                    contentType(ContentType.Application.Json)
                    setBody(input)
                }
            ) {
                // Process incoming SSE events
                incoming.collect { event ->
                    event.data?.let { data ->
                        val parsedEvent = parseEvent(data)
                        if (parsedEvent != null) {
                            eventChannel.send(parsedEvent)
                        }
                    }
                }
            }
        } catch (e: HttpRequestTimeoutException) {
            throw TransportTimeoutException(
                "HTTP request timed out: ${e.message}",
                0L, // timeoutMillis is private in Ktor
                e
            )
        } catch (e: Exception) {
            if (e is HttpClientTransportException || e is TransportException) {
                throw e
            } else {
                throw TransportConnectionException(
                    "HTTP connection failed: ${e.message}",
                    e
                )
            }
        }
    }
    
    
    private fun parseEvent(data: String): BaseEvent? {
        return try {
            json.decodeFromString(data)
        } catch (e: SerializationException) {
            logger.warn(e) { "Failed to parse event: $data" }
            null
        }
    }
    
}


