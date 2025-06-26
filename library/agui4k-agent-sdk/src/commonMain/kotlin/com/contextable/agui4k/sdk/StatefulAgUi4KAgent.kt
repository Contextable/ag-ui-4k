// agui4k-agent-sdk/src/commonMain/kotlin/com/contextable/agui4k/sdk/StatefulAgUi4KAgent.kt
package com.contextable.agui4k.sdk

import com.contextable.agui4k.client.agent.AgentState
import com.contextable.agui4k.client.state.PredictStateValue
import com.contextable.agui4k.core.types.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.*
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Stateful AG-UI agent that maintains conversation history and state.
 * Includes predictive state updates via PredictStateValue.
 */
class StatefulAgUi4KAgent(
    url: String,
    configure: StatefulAgUi4kAgentConfig.() -> Unit = {}
) : AgUi4KAgent(url, { 
    val statefulConfig = StatefulAgUi4kAgentConfig().apply(configure)
    // Copy properties from stateful config to base config
    bearerToken = statefulConfig.bearerToken
    apiKey = statefulConfig.apiKey
    apiKeyHeader = statefulConfig.apiKeyHeader
    headers = statefulConfig.headers
    systemPrompt = statefulConfig.systemPrompt
    debug = statefulConfig.debug
    toolRegistry = statefulConfig.toolRegistry
    context.addAll(statefulConfig.context)
    forwardedProps = statefulConfig.forwardedProps
    requestTimeout = statefulConfig.requestTimeout
    connectTimeout = statefulConfig.connectTimeout
}) {

    private val statefulConfig = StatefulAgUi4kAgentConfig().apply(configure)

    /**
     * Chat interface - delegates to sendMessage with thread management
     */
    fun chat(
        message: String,
        threadId: String = "default"
    ): Flow<BaseEvent> {
        return sendMessage(
            message = message,
            threadId = threadId,
            state = statefulConfig.initialState,
            includeSystemPrompt = true
        )
    }

}

/**
 * Configuration for stateful agents
 */
class StatefulAgUi4kAgentConfig : AgUi4kAgentConfig() {
    var initialState: JsonElement = JsonObject(emptyMap())
    var maxHistoryLength: Int = 100  // 0 = unlimited
}