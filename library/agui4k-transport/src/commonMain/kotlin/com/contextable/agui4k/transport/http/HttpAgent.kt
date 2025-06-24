// File: agui4k-transport/src/commonMain/kotlin/com/contextable/agui4k/transport/http/HttpAgent.kt

package com.contextable.agui4k.transport.http

import com.contextable.agui4k.core.types.*
import io.ktor.client.*
import io.ktor.client.plugins.sse.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * HTTP Agent for AG-UI protocol communication.
 *
 * This class provides a familiar interface for developers coming from the
 * Python and TypeScript AG-UI SDKs. It handles HTTP requests with SSE streaming.
 */
class HttpAgent(
    private val config: HttpAgentConfig
) {
    private val client: HttpClient
    private var abortController: CompletableJob? = null

    init {
        client = config.httpClient ?: HttpClient {
            install(SSE)
        }
    }

    data class HttpAgentConfig(
        val url: String,
        val headers: Map<String, String> = emptyMap(),
        val httpClient: HttpClient? = null
    )

    fun runAgent(input: RunAgentInput): Flow<BaseEvent> = channelFlow {
        abortController = Job()

        try {
            client.sse(
                urlString = config.url,
                request = {
                    method = HttpMethod.Post
                    config.headers.forEach { (key, value) ->
                        header(key, value)
                    }
                    contentType(ContentType.Application.Json)
                    accept(ContentType.Text.EventStream)
                    setBody(input)
                }
            ) {
                incoming.collect { sseEvent ->
                    ensureActive()
                    sseEvent.data?.let { data ->
                        try {
                            val event = AgUiJson.decodeFromString<BaseEvent>(data)
                            logger.debug { "Received event: ${event.eventType}" }
                            send(event)
                        } catch (e: Exception) {
                            logger.error(e) { "Failed to parse event data: $data" }
                        }
                    }
                }
            }
        } catch (e: CancellationException) {
            logger.debug { "Agent run aborted" }
            throw e
        } catch (e: Exception) {
            logger.error(e) { "Agent run failed: ${e.message}" }
            throw e
        } finally {
            abortController = null
        }
    }.flowOn(Dispatchers.IO + (abortController ?: Job()))

    fun abort() {
        logger.debug { "Aborting agent run" }
        abortController?.cancel("Agent run aborted by user")
        abortController = null
    }

    fun close() {
        abort()
        if (config.httpClient == null) {
            client.close()
        }
    }
}