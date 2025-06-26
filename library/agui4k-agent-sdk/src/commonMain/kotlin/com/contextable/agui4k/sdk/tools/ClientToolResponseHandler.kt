// agui4k-agent-sdk/src/commonMain/kotlin/com/contextable/agui4k/sdk/tools/ClientToolResponseHandler.kt
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