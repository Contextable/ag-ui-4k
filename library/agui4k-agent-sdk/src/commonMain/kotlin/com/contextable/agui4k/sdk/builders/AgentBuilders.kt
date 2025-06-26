// agui4k-agent-sdk/src/commonMain/kotlin/com/contextable/agui4k/sdk/builders/AgentBuilders.kt
package com.contextable.agui4k.sdk.builders

import com.contextable.agui4k.sdk.*
import com.contextable.agui4k.tools.ToolRegistry
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

/**
 * Create a simple stateless agent with bearer token auth
 */
fun agentWithBearer(url: String, token: String): AgUi4KAgent {
    return AgUi4KAgent(url) {
        bearerToken = token
    }
}

/**
 * Create a simple stateless agent with API key auth
 */
fun agentWithApiKey(
    url: String,
    apiKey: String,
    headerName: String = "X-API-Key"
): AgUi4KAgent {
    return AgUi4KAgent(url) {
        this.apiKey = apiKey
        this.apiKeyHeader = headerName
    }
}

/**
 * Create a stateless agent with tools
 */
fun agentWithTools(
    url: String,
    toolRegistry: ToolRegistry,
    configure: AgUi4kAgentConfig.() -> Unit = {}
): AgUi4KAgent {
    return AgUi4KAgent(url) {
        this.toolRegistry = toolRegistry
        configure()
    }
}

/**
 * Create a stateful chat agent
 */
fun chatAgent(
    url: String,
    systemPrompt: String,
    configure: StatefulAgUi4kAgentConfig.() -> Unit = {}
): StatefulAgUi4KAgent {
    return StatefulAgUi4KAgent(url) {
        this.systemPrompt = systemPrompt
        configure()
    }
}

/**
 * Create a stateful agent with initial state
 */
fun statefulAgent(
    url: String,
    initialState: JsonElement,
    configure: StatefulAgUi4kAgentConfig.() -> Unit = {}
): StatefulAgUi4KAgent {
    return StatefulAgUi4KAgent(url) {
        this.initialState = initialState
        configure()
    }
}

/**
 * Create a debug agent that logs all events
 */
fun debugAgent(
    url: String,
    configure: AgUi4kAgentConfig.() -> Unit = {}
): AgUi4KAgent {
    return AgUi4KAgent(url) {
        debug = true
        configure()
    }
}