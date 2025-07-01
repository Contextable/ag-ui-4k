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
package com.contextable.agui4k.sdk.tools

import com.contextable.agui4k.client.agent.HttpAgent
import com.contextable.agui4k.client.agent.RunAgentParameters
import com.contextable.agui4k.core.types.*
import com.contextable.agui4k.tools.ToolResponseHandler
import kotlinx.coroutines.flow.collect
import kotlinx.datetime.Clock
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Tool response handler that sends responses back through the HTTP agent
 */
class ClientToolResponseHandler(
    private val httpAgent: HttpAgent
) : ToolResponseHandler {

    override suspend fun sendToolResponse(
        toolMessage: ToolMessage,
        threadId: String?,
        runId: String?
    ) {
        logger.info { "Sending tool response for thread: $threadId, run: $runId" }

        // Create a minimal input with just the tool message
        val input = RunAgentInput(
            threadId = threadId ?: "tool_thread_${Clock.System.now().toEpochMilliseconds()}",
            runId = runId ?: "tool_run_${Clock.System.now().toEpochMilliseconds()}",
            messages = listOf(toolMessage)
        )

        // Send through HTTP agent
        try {
            httpAgent.runAgent(RunAgentParameters(
                runId = input.runId,
                tools = input.tools,
                context = input.context,
                forwardedProps = input.forwardedProps
            ))
            logger.debug { "Tool response sent successfully" }
        } catch (e: Exception) {
            logger.error(e) { "Failed to send tool response" }
            throw e
        }
    }
}