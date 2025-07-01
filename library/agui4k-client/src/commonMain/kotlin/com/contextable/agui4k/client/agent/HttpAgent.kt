/*
 * MIT License
 *
 * Copyright (c) 2025 Mark Fogle
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
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
    private val config: HttpAgentConfig,
    private val httpClient: HttpClient? = null
) : AbstractAgent(config) {
    
    private val client: HttpClient
    private val sseParser = SseParser()
    
    init {
        client = httpClient ?: createPlatformHttpClient(config.requestTimeout, config.connectTimeout)
    }
    
    /**
     * Implementation of abstract run method using HTTP/SSE transport.
     */
    override fun run(input: RunAgentInput): Flow<BaseEvent> = channelFlow {
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
            config = HttpAgentConfig(
                agentId = this@HttpAgent.agentId,
                description = this@HttpAgent.description,
                threadId = this@HttpAgent.threadId,
                initialMessages = this@HttpAgent.messages.toList(),
                initialState = this@HttpAgent.state,
                debug = this@HttpAgent.debug,
                url = config.url,
                headers = config.headers,
                requestTimeout = config.requestTimeout,
                connectTimeout = config.connectTimeout
            ),
            httpClient = httpClient
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