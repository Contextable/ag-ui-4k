package com.contextable.agui4k.client

import com.contextable.agui4k.core.agent.AbstractAgent
import com.contextable.agui4k.core.agent.AgentConfig
import com.contextable.agui4k.core.types.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.plugins.sse.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.sse.*
import io.ktor.utils.io.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.*
import mu.KotlinLogging
import kotlin.coroutines.cancellation.CancellationException as KotlinCancellationException

private val logger = KotlinLogging.logger {}

/**
 * Configuration for the HttpAgent client.
 */
class HttpAgentConfig(
    val url: String,  // Endpoint URL for the AG-UI agent service
    val headers: Map<String, String> = emptyMap(),  // HTTP headers to include with requests
    agentId: String? = null,
    description: String? = null,
    threadId: String? = null,
    initialMessages: List<Message> = emptyList(),
    initialState: JsonElement? = null
) : AgentConfig(agentId, description, threadId, initialMessages, initialState)

/**
 * HTTP-based client for connecting to remote AG-UI agents.
 * 
 * This implementation uses Server-Sent Events (SSE) for streaming responses
 * from agents and supports cancellation through Kotlin coroutines.
 * 
 * Example usage:
 * ```kotlin
 * val client = HttpAgent(HttpAgentConfig(
 *     url = "https://api.example.com/v1/agent",
 *     headers = mapOf("Authorization" to "Bearer your-api-key")
 * ))
 * 
 * client.runAgent().collect { event ->
 *     when (event) {
 *         is TextMessageContentEvent -> println(event.delta)
 *         // Handle other events...
 *     }
 * }
 * ```
 */
@OptIn(ExperimentalCoroutinesApi::class, ExperimentalSerializationApi::class)
class HttpAgent(
    private val config: HttpAgentConfig
) : AbstractAgent(config) {
    
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        classDiscriminator = "type"
    }
    
    private val httpClient = HttpClient {
        install(ContentNegotiation) {
            json(this@HttpAgent.json)
        }
        
        install(Logging) {
            logger = Logger.DEFAULT
            level = LogLevel.INFO
        }
        
        install(SSE)
        
        install(HttpTimeout) {
            requestTimeoutMillis = 600_000 // 10 minutes
            connectTimeoutMillis = 30_000  // 30 seconds
            socketTimeoutMillis = 600_000  // 10 minutes for streaming
        }
        
        defaultRequest {
            headers {
                config.headers.forEach { (key, value) ->
                    append(key, value)
                }
            }
        }
    }
    
    private var currentJob: kotlinx.coroutines.Job? = null
    
    /**
     * Connects to the agent by making an HTTP request to the configured endpoint.
     */
    override suspend fun run(input: RunAgentInput): Flow<BaseEvent> = channelFlow {
        currentJob = coroutineContext[kotlinx.coroutines.Job]
        
        try {
            httpClient.prepareRequest {
                method = HttpMethod.Post
                url(config.url)
                contentType(ContentType.Application.Json)
                accept(ContentType.Text.EventStream)
                setBody(input)
            }.execute { response ->
                if (!response.status.isSuccess()) {
                    throw HttpAgentException(
                        "HTTP request failed with status ${response.status}",
                        response.status
                    )
                }
                
                // Check if the response is SSE
                val contentType = response.contentType()
                if (contentType != null && contentType.match(ContentType.Text.EventStream)) {
                    // Handle SSE response using Ktor 3's improved SSE support
                    response.bodyAsChannel().let { channel ->
                        handleSseResponse(channel)
                    }
                } else {
                    // Handle regular JSON response (for compatibility)
                    handleJsonResponse(response)
                }
            }
        } catch (e: CancellationException) {
            logger.debug { "Agent connection cancelled" }
            throw e
        } catch (e: Exception) {
            logger.error(e) { "Error during HTTP request to agent" }
            send(
                RunErrorEvent(
                    message = e.message ?: "Unknown error",
                    code = "HTTP_ERROR"
                )
            )
            throw e
        } finally {
            currentJob = null
        }
    }
    
    /**
     * Handles Server-Sent Events response.
     */
    private suspend fun ProducerScope<BaseEvent>.handleSseResponse(channel: ByteReadChannel) {
        channel.readSse().collect { sseEvent ->
            sseEvent.data?.let { data ->
                try {
                    val event = parseEvent(data)
                    if (event != null) {
                        send(event)
                    }
                } catch (e: Exception) {
                    logger.error(e) { "Error parsing SSE event: $data" }
                }
            }
        }
    }
    
    /**
     * Handles regular JSON response (for compatibility).
     */
    private suspend fun ProducerScope<BaseEvent>.handleJsonResponse(response: HttpResponse) {
        val responseText = response.bodyAsText()
        try {
            // Try to parse as array of events
            val events = json.decodeFromString<List<JsonElement>>(responseText)
            events.forEach { jsonElement ->
                val event = parseEventFromJson(jsonElement)
                if (event != null) {
                    send(event)
                }
            }
        } catch (e: Exception) {
            // Try to parse as single event
            try {
                val event = parseEvent(responseText)
                if (event != null) {
                    send(event)
                }
            } catch (e2: Exception) {
                logger.error(e2) { "Error parsing JSON response: $responseText" }
                throw e2
            }
        }
    }
    
    /**
     * Parses an event from a string.
     */
    private fun parseEvent(data: String): BaseEvent? {
        return try {
            json.decodeFromString<BaseEvent>(data)
        } catch (e: Exception) {
            logger.error(e) { "Error parsing event: $data" }
            null
        }
    }
    
    /**
     * Parses an event from a JsonElement.
     */
    private fun parseEventFromJson(jsonElement: JsonElement): BaseEvent? {
        return try {
            json.decodeFromJsonElement<BaseEvent>(jsonElement)
        } catch (e: Exception) {
            logger.error(e) { "Error parsing event from JSON: $jsonElement" }
            null
        }
    }
    
    /**
     * Cancels the current HTTP request to the agent.
     */
    override fun abortRun() {
        super.abortRun()
        currentJob?.cancel("Client connection aborted")
    }
    
    /**
     * Creates a deep copy of the HttpAgent client instance.
     */
    override fun clone(): HttpAgent {
        return HttpAgent(
            HttpAgentConfig(
                url = config.url,
                headers = config.headers,
                agentId = generateAgentId(),
                description = description,
                threadId = threadId,
                initialMessages = messages.value.toList(),
                initialState = state.value
            )
        )
    }
}

/**
 * Extension function to read SSE from a ByteReadChannel.
 */
private fun ByteReadChannel.readSse(): Flow<SseEvent> = flow {
    val buffer = StringBuilder()
    
    while (!isClosedForRead) {
        val line = readUTF8Line() ?: break
        
        if (line.isEmpty()) {
            // Empty line indicates end of event
            if (buffer.isNotEmpty()) {
                val event = parseSseEvent(buffer.toString())
                if (event != null) {
                    emit(event)
                }
                buffer.clear()
            }
        } else {
            buffer.appendLine(line)
        }
    }
    
    // Handle any remaining data
    if (buffer.isNotEmpty()) {
        val event = parseSseEvent(buffer.toString())
        if (event != null) {
            emit(event)
        }
    }
}

/**
 * Data class representing a Server-Sent Event.
 */
private data class SseEvent(
    val data: String? = null,
    val event: String? = null,
    val id: String? = null,
    val retry: Long? = null
)

/**
 * Parses SSE event from text.
 */
private fun parseSseEvent(text: String): SseEvent? {
    var data: String? = null
    var event: String? = null
    var id: String? = null
    var retry: Long? = null
    
    text.lines().forEach { line ->
        when {
            line.startsWith("data:") -> {
                data = line.substring(5).trim()
            }
            line.startsWith("event:") -> {
                event = line.substring(6).trim()
            }
            line.startsWith("id:") -> {
                id = line.substring(3).trim()
            }
            line.startsWith("retry:") -> {
                retry = line.substring(6).trim().toLongOrNull()
            }
        }
    }
    
    return if (data != null || event != null || id != null || retry != null) {
        SseEvent(data, event, id, retry)
    } else {
        null
    }
}

/**
 * Exception thrown by HttpAgent for HTTP-related errors.
 */
class HttpAgentException(
    message: String,
    val statusCode: HttpStatusCode? = null,
    cause: Throwable? = null
) : Exception(message, cause)