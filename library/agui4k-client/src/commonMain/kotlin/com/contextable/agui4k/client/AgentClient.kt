// File: agui4k-client/src/commonMain/kotlin/com/contextable/agui4k/client/AgentClient.kt

package com.contextable.agui4k.client

import com.contextable.agui4k.client.state.*
import com.contextable.agui4k.core.types.*
import com.contextable.agui4k.transport.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.*

/**
 * The AG-UI Kotlin client.
 *
 * Simple example:
 * ```kotlin
 * val client = AgentClient("https://api.example.com/agent") {
 *     bearerToken = "..."
 * }
 *
 * client.chat("Hello!").collect { event ->
 *     when (event) {
 *         is TextMessageContentEvent -> print(event.delta)
 *     }
 * }
 * ```
 */
class AgentClient(
    url: String,
    configure: ClientConfig.() -> Unit = {}
) {
    private val config = ClientConfig(url).apply(configure)
    private val transport = HttpClientTransport(config.toTransportConfig())
    private val stateManager = StateManager(config.stateHandler)

    private val messageHistory = if (config.maintainHistory) {
        mutableMapOf<String, MutableList<Message>>()
    } else null

    val state: StateFlow<JsonElement> = stateManager.currentState

    fun chat(
        message: String,
        threadId: String = "default",
        includeHistory: Boolean = config.maintainHistory
    ): Flow<BaseEvent> {
        val messages = mutableListOf<Message>()

        if (messageHistory?.get(threadId).isNullOrEmpty() && config.systemPrompt != null) {
            messages.add(SystemMessage(
                id = generateId("msg"),
                content = config.systemPrompt
            ))
        }

        if (includeHistory) {
            messageHistory?.get(threadId)?.let { messages.addAll(it) }
        }

        val userMessage = UserMessage(
            id = generateId("msg"),
            content = message
        )
        messages.add(userMessage)

        messageHistory?.getOrPut(threadId) { mutableListOf() }?.add(userMessage)

        val input = RunAgentInput(
            threadId = threadId,
            runId = generateId("run"),
            messages = messages,
            state = stateManager.currentState.value,
            tools = config.tools,
            context = config.context
        )

        return flow {
            val session = transport.startRun(
                messages = input.messages,
                threadId = input.threadId,
                runId = input.runId,
                state = input.state,
                tools = input.tools,
                context = input.context
            )
            session.events.onEach { event ->
                stateManager.processEvent(event)
            }.collect { event ->
                emit(event)
            }
        }
    }

    fun run(input: RunAgentInput): Flow<BaseEvent> {
        return flow {
            val session = transport.startRun(
                messages = input.messages,
                threadId = input.threadId,
                runId = input.runId,
                state = input.state,
                tools = input.tools,
                context = input.context,
                forwardedProps = input.forwardedProps
            )
            session.events.onEach { event ->
                stateManager.processEvent(event)
            }.collect { event ->
                emit(event)
            }
        }
    }

    fun getStateValue(path: String): JsonElement? =
        stateManager.getValue(path)

    fun clearHistory(threadId: String = "default") {
        messageHistory?.remove(threadId)
    }

    private fun generateId(prefix: String) =
        "${prefix}_${kotlinx.datetime.Clock.System.now().toEpochMilliseconds()}"
}

data class ClientConfig(
    val url: String,
    var bearerToken: String? = null,
    var headers: MutableMap<String, String> = mutableMapOf(),
    var systemPrompt: String? = null,
    var maintainHistory: Boolean = false,
    var stateHandler: StateChangeHandler? = null,
    val tools: MutableList<Tool> = mutableListOf(),
    val context: MutableList<Context> = mutableListOf()
) {
    fun toTransportConfig() = HttpClientTransportConfig(
        url = url,
        headers = buildMap {
            bearerToken?.let { put("Authorization", "Bearer $it") }
            putAll(headers)
        }
    )
}