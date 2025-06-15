package com.contextable.agui4k.client

import com.contextable.agui4k.core.agent.AbstractAgent
import com.contextable.agui4k.core.agent.AgentConfig
import com.contextable.agui4k.core.serialization.AgUiJson
import com.contextable.agui4k.core.types.BaseEvent
import com.contextable.agui4k.core.types.Message
import com.contextable.agui4k.core.types.RunAgentInput
import com.contextable.agui4k.core.types.RunErrorEvent
import io.ktor.client.HttpClient
import io.ktor.client.statement.bodyAsText
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.DEFAULT
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.sse.SSE
import io.ktor.client.request.accept
import io.ktor.client.request.headers
import io.ktor.client.request.prepareRequest
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readUTF8Line
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import mu.KotlinLogging
import io.ktor.client.plugins.HttpRequestTimeoutException

private val logger = KotlinLogging.logger {}

private sealed class ParseResult<out T> {
    data class Success<T>(val value: T) : ParseResult<T>()
    data class Error(val exception: SerializationException) : ParseResult<Nothing>()
}


// Constants for magic numbers
private const val REQUEST_TIMEOUT_MILLIS = 600_000L // 10 minutes
private const val CONNECT_TIMEOUT_MILLIS = 30_000L // 30 seconds
private const val SOCKET_TIMEOUT_MILLIS = 600_000L // 10 minutes
private const val SSE_DATA_PREFIX_LENGTH = 5
private const val SSE_EVENT_PREFIX_LENGTH = 6
private const val SSE_ID_PREFIX_LENGTH = 3
private const val SSE_RETRY_PREFIX_LENGTH = 6

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

    private val json = AgUiJson

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
            requestTimeoutMillis = REQUEST_TIMEOUT_MILLIS
            connectTimeoutMillis = CONNECT_TIMEOUT_MILLIS
            socketTimeoutMillis = SOCKET_TIMEOUT_MILLIS
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
            executeHttpRequest(input)
        } catch (e: CancellationException) {
            logger.debug { "Agent connection cancelled" }
            throw e
        } catch (e: HttpAgentException) {
            handleHttpError(e, "HTTP_ERROR")
            throw e
        } catch (e: HttpRequestTimeoutException) {
            val agentException = HttpAgentException("Request timeout: ${e.message}", cause = e)
            handleHttpError(agentException, "TIMEOUT_ERROR")
            throw agentException
        } catch (e: SerializationException) {
            val agentException = HttpAgentException("Serialization error: ${e.message}", cause = e)
            handleHttpError(agentException, "SERIALIZATION_ERROR")
            throw agentException
        } finally {
            currentJob = null
        }
    }

    /**
     * Executes the HTTP request to the agent.
     */
    private suspend fun ProducerScope<BaseEvent>.executeHttpRequest(input: RunAgentInput) {
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

            handleResponse(response)
        }
    }
    /**
     * Handles the HTTP response based on content type.
     */
    private suspend fun ProducerScope<BaseEvent>.handleResponse(response: HttpResponse) {
        val contentType = response.contentType()
        if (contentType != null && contentType.match(ContentType.Text.EventStream)) {
            response.bodyAsChannel().let { channel ->
                handleSseResponse(channel)
            }
        } else {
            handleJsonResponse(response)
        }
    }

    /**
     * Handles HTTP errors by sending error events.
     */
    private suspend fun ProducerScope<BaseEvent>.handleHttpError(
        exception: HttpAgentException,
        errorCode: String
    ) {
        logger.error(exception) { "HTTP error: ${exception.message}" }
        send(
            RunErrorEvent(
                message = exception.message ?: "Unknown error",
                code = errorCode
            )
        )
    }

    /**
     * Handles Server-Sent Events response.
     */
    private suspend fun ProducerScope<BaseEvent>.handleSseResponse(channel: ByteReadChannel) {
        var parseErrors = 0
        var successfulEvents = 0

        channel.readSse().collect { sseEvent ->
            sseEvent.data?.let { data ->
                val event = parseEvent(data)
                if (event != null) {
                    send(event)
                    successfulEvents++
                } else {
                    parseErrors++
                }
            }
        }

        // If we had only errors and no successful events, throw an exception
        if (parseErrors > 0 && successfulEvents == 0) {
            throw HttpAgentException("Failed to parse any SSE events. Total errors: $parseErrors")
        }
    }

    /**
     * Handles regular JSON response (for compatibility).
     */
    private suspend fun ProducerScope<BaseEvent>.handleJsonResponse(response: HttpResponse) {
        val responseText = response.bodyAsText()
        var parseErrors = 0
        var successfulEvents = 0

        try {
            // Try to parse as array of events
            val events = json.decodeFromString<List<JsonElement>>(responseText)

            events.forEach { jsonElement ->
                val event = parseEventFromJson(jsonElement)
                if (event != null) {
                    send(event)
                    successfulEvents++
                } else {
                    parseErrors++
                }
            }

            // If we parsed some events but not all, log a warning
            if (parseErrors > 0 && successfulEvents > 0) {
                logger.warn { "Parsed $successfulEvents events successfully, failed to parse $parseErrors events" }
            } else if (parseErrors > 0 && successfulEvents == 0) {
                throw HttpAgentException("Failed to parse any events from array response")
            }
        } catch (e: SerializationException) {
            // Try to parse as single event
            val event = parseEvent(responseText)
            if (event != null) {
                send(event)
            } else {
                throw HttpAgentException("Failed to parse response as event or event array", cause = e)
            }
        }
    }

    /**
     * Parses an event from a string.
     */
    private fun parseEvent(data: String): BaseEvent? {
        return try {
            json.decodeFromString<BaseEvent>(data)
        } catch (e: SerializationException) {
            // Log the error with details but return null to allow stream to continue
            logger.error(e) { "Failed to parse event, skipping: $data" }
            null
        }
    }

    /**
     * Parses an event from a JsonElement.
     */
    private fun parseEventFromJson(jsonElement: JsonElement): BaseEvent? {
        return try {
            json.decodeFromJsonElement<BaseEvent>(jsonElement)
        } catch (e: SerializationException) {
            // Log the error with details but return null to allow stream to continue
            logger.error(e) { "Failed to parse JSON event, skipping: $jsonElement" }
            null
        }
    }

    /**
     * Cancels the current HTTP request to the agent.
     */
    override fun abortRun() {
        super.abortRun()
        currentJob?.cancel(CancellationException("Client connection aborted"))
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
                data = line.substring(SSE_DATA_PREFIX_LENGTH).trim()
            }
            line.startsWith("event:") -> {
                event = line.substring(SSE_EVENT_PREFIX_LENGTH).trim()
            }
            line.startsWith("id:") -> {
                id = line.substring(SSE_ID_PREFIX_LENGTH).trim()
            }
            line.startsWith("retry:") -> {
                retry = line.substring(SSE_RETRY_PREFIX_LENGTH).trim().toLongOrNull()
            }
        }
    }

    // Simplify complex condition
    val hasData = data != null
    val hasMetadata = event != null || id != null || retry != null

    return if (hasData || hasMetadata) {
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
