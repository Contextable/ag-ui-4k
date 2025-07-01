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