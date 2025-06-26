// agui4k-agent-sdk/src/commonMain/kotlin/com/contextable/agui4k/sdk/AgUi4KAgent.kt
package com.contextable.agui4k.sdk

import com.contextable.agui4k.client.agent.*
import com.contextable.agui4k.client.state.defaultApplyEvents
import com.contextable.agui4k.client.verify.verifyEvents
import com.contextable.agui4k.core.types.*
import com.contextable.agui4k.tools.*
import com.contextable.agui4k.sdk.tools.ClientToolResponseHandler
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.flow.*
import kotlinx.datetime.Clock
import kotlinx.serialization.json.*
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Stateless AG-UI agent that processes each request independently.
 * Does not maintain conversation history or state between calls.
 */
open class AgUi4KAgent(
    protected val url: String,
    configure: AgUi4kAgentConfig.() -> Unit = {}
) {
    protected val config = AgUi4kAgentConfig().apply(configure)

    // Create HttpAgent which extends AbstractAgent
    protected val agent = HttpAgent(
        url = url,
        headers = config.buildHeaders(),
        httpClient = null,
        requestTimeout = config.requestTimeout,
        connectTimeout = config.connectTimeout,
        config = AgentConfig(
            agentId = null,
            description = "",
            threadId = null,
            initialMessages = emptyList(),
            initialState = JsonObject(emptyMap()),
            debug = config.debug
        )
    )

    protected val toolExecutionManager = config.toolRegistry?.let {
        ToolExecutionManager(it, ClientToolResponseHandler(agent))
    }

    /**
     * Run agent with explicit input and return observable event stream
     */
    open fun run(input: RunAgentInput): Flow<BaseEvent> {
        // Get the raw event stream from the agent
        val eventStream = agent.runAgentObservable(input)
        
        // If we have a tool execution manager, process events through it
        return if (toolExecutionManager != null) {
            toolExecutionManager.processEventStream(
                events = eventStream,
                threadId = input.threadId,
                runId = input.runId
            )
        } else {
            // No tools configured, just pass through the events
            eventStream
        }
    }

    /**
     * Simple message interface - creates fresh input each time
     */
    open fun sendMessage(
        message: String,
        threadId: String = generateThreadId(),
        state: JsonElement? = null,
        includeSystemPrompt: Boolean = true
    ): Flow<BaseEvent> {
        val messages = mutableListOf<Message>()

        if (includeSystemPrompt && config.systemPrompt != null) {
            messages.add(SystemMessage(
                id = generateId("sys"),
                content = config.systemPrompt!!
            ))
        }

        messages.add(UserMessage(
            id = generateId("usr"),
            content = message
        ))

        val input = RunAgentInput(
            threadId = threadId,
            runId = generateRunId(),
            messages = messages,
            state = state ?: JsonObject(emptyMap()),
            tools = config.toolRegistry?.getAllTools() ?: emptyList(),
            context = config.context,
            forwardedProps = config.forwardedProps
        )

        return run(input)
    }

    /**
     * Close the agent and release resources
     */
    open fun close() {
        agent.dispose()
    }

    protected fun generateThreadId(): String = "thread_${Clock.System.now().toEpochMilliseconds()}"
    protected fun generateRunId(): String = "run_${Clock.System.now().toEpochMilliseconds()}"
    protected fun generateId(prefix: String): String = "${prefix}_${Clock.System.now().toEpochMilliseconds()}"
}

/**
 * Configuration for AG-UI agents
 */
open class AgUi4kAgentConfig {
    var bearerToken: String? = null
    var apiKey: String? = null
    var apiKeyHeader: String = "X-API-Key"
    var headers: MutableMap<String, String> = mutableMapOf()
    var systemPrompt: String? = null
    var debug: Boolean = false
    var toolRegistry: ToolRegistry? = null
    // State handling is now done through defaultApplyEvents
    val context: MutableList<Context> = mutableListOf()
    var forwardedProps: JsonElement = JsonObject(emptyMap())
    var requestTimeout: Long = 600_000L // 10 minutes
    var connectTimeout: Long = 30_000L  // 30 seconds

    fun buildHeaders(): Map<String, String> = buildMap {
        bearerToken?.let { put("Authorization", "Bearer $it") }
        apiKey?.let { put(apiKeyHeader, it) }
        putAll(headers)
    }
}