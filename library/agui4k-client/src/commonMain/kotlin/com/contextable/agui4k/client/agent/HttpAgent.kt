// agui4k-client/src/commonMain/kotlin/com/contextable/agui4k/client/agent/HttpAgent.kt
package com.contextable.agui4k.client.agent

import com.contextable.agui4k.client.sse.SseParser
import com.contextable.agui4k.core.types.*
import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.sse.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * HTTP-based agent implementation using Ktor client.
 * Extends AbstractAgent to provide HTTP/SSE transport.
 */
class HttpAgent(
    private val url: String,
    private val headers: Map<String, String> = emptyMap(),
    private val httpClient: HttpClient? = null,
    private val requestTimeout: Long = 600_000L, // 10 minutes
    private val connectTimeout: Long = 30_000L,   // 30 seconds
    config: AgentConfig = AgentConfig()
) : AbstractAgent(config) {
    
    private val client: HttpClient
    private val sseParser = SseParser()
    
    init {
        client = httpClient ?: createPlatformHttpClient(requestTimeout, connectTimeout)
    }
    
    /**
     * Implementation of abstract run method using HTTP/SSE transport.
     */
    override fun run(input: RunAgentInput): Flow<BaseEvent> = channelFlow {
        try {
            client.sse(
                urlString = url,
                request = {
                    method = HttpMethod.Post
                    this@HttpAgent.headers.forEach { (key, value) ->
                        header(key, value)
                    }
                    contentType(ContentType.Application.Json)
                    accept(ContentType.Text.EventStream)
                    setBody(input)
                }
            ) {
                // Convert SSE events to string flow
                val stringFlow = incoming.mapNotNull { sseEvent ->
                    logger.debug { "Raw SSE event: ${sseEvent}" }
                    sseEvent.data?.also { data ->
                        logger.debug { "SSE data: $data" }
                    }
                }
                
                // Parse SSE stream
                sseParser.parseFlow(stringFlow)
                    .collect { event ->
                        logger.debug { "Parsed event: ${event.eventType}" }
                        send(event)
                    }
            }
        } catch (e: CancellationException) {
            logger.debug { "Agent run cancelled" }
            throw e
        } catch (e: Exception) {
            logger.error(e) { "Agent run failed: ${e.message}" }
            
            // Emit error event
            send(RunErrorEvent(
                message = e.message ?: "Unknown error",
                code = when (e) {
                    is HttpRequestTimeoutException -> "TIMEOUT_ERROR"
                    else -> "TRANSPORT_ERROR"
                }
            ))
        }
    }
    
    /**
     * Creates a clone of this agent with the same configuration.
     */
    override fun clone(): AbstractAgent {
        return HttpAgent(
            url = url,
            headers = headers,
            httpClient = httpClient,
            requestTimeout = requestTimeout,
            connectTimeout = connectTimeout,
            config = AgentConfig(
                agentId = this@HttpAgent.agentId,
                description = this@HttpAgent.description,
                threadId = this@HttpAgent.threadId,
                initialMessages = this@HttpAgent.messages.toList(),
                initialState = this@HttpAgent.state,
                debug = this@HttpAgent.debug
            )
        )
    }
    
    /**
     * Cleanup HTTP client resources only when explicitly closed, not after each run.
     */
    override fun onFinalize() {
        super.onFinalize()
        // Don't close the client here - it should be reusable for multiple runs
    }
    
    /**
     * Override dispose to properly cleanup HTTP client resources.
     */
    override fun dispose() {
        // Close the HTTP client if we created it
        if (httpClient == null) {
            client.close()
        }
        super.dispose()
    }
}