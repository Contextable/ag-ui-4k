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
open class StatefulAgUi4KAgent(
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
    userId = statefulConfig.userId
    context.addAll(statefulConfig.context)
    forwardedProps = statefulConfig.forwardedProps
    requestTimeout = statefulConfig.requestTimeout
    connectTimeout = statefulConfig.connectTimeout
}) {

    private val statefulConfig = StatefulAgUi4kAgentConfig().apply(configure)
    
    // Store conversation history per thread
    private val conversationHistory = mutableMapOf<String, MutableList<Message>>()
    private var currentState: JsonElement = statefulConfig.initialState

    /**
     * Chat interface - delegates to sendMessage with thread management
     *
     * @param message The user message to send
     * @param threadId The thread ID for conversation (defaults to "default")
     * @return Flow of events from the agent
     */
    fun chat(
        message: String,
        threadId: String = "default"
    ): Flow<BaseEvent> {
        return sendMessage(
            message = message,
            threadId = threadId,
            state = currentState,
            includeSystemPrompt = true
        )
    }
    
    /**
     * Override sendMessage to maintain conversation history
     */
    override fun sendMessage(
        message: String,
        threadId: String,
        state: JsonElement?,
        includeSystemPrompt: Boolean
    ): Flow<BaseEvent> {
        // Get or create conversation history for this thread
        val threadHistory = conversationHistory.getOrPut(threadId) { mutableListOf() }
        
        // Add system prompt if it's the first message and includeSystemPrompt is true
        if (threadHistory.isEmpty() && includeSystemPrompt && config.systemPrompt != null) {
            val systemMessage = SystemMessage(
                id = generateId("sys"),
                content = config.systemPrompt!!
            )
            threadHistory.add(systemMessage)
        }
        
        // Create and add the new user message
        val userMessage = UserMessage(
            id = config.userId ?: generateId("usr"),
            content = message
        )
        threadHistory.add(userMessage)
        
        // Build the complete message list including all history
        val messages = threadHistory.toMutableList()
        
        // Apply history length limit if configured
        if (statefulConfig.maxHistoryLength > 0 && threadHistory.size > statefulConfig.maxHistoryLength) {
            // Keep system message if present, then trim from the beginning
            val hasSystemMessage = threadHistory.firstOrNull() is SystemMessage
            val systemMessage = if (hasSystemMessage) threadHistory.first() else null
            
            val trimCount = threadHistory.size - statefulConfig.maxHistoryLength
            repeat(trimCount) {
                if (hasSystemMessage && threadHistory.size > 1) {
                    threadHistory.removeAt(1) // Keep system message at index 0
                } else if (!hasSystemMessage && threadHistory.isNotEmpty()) {
                    threadHistory.removeAt(0)
                }
            }
        }
        
        // Use the provided state or the current state
        val stateToUse = state ?: currentState
        
        // Create the input with full conversation history
        val input = RunAgentInput(
            threadId = threadId,
            runId = generateRunId(),
            messages = messages,
            state = stateToUse,
            tools = config.toolRegistry?.getAllTools() ?: emptyList(),
            context = config.context,
            forwardedProps = config.forwardedProps
        )
        
        // Collect events and extract assistant responses to add to history
        return run(input).onEach { event ->
            when (event) {
                is TextMessageStartEvent -> {
                    // Start collecting assistant message
                    val assistantMessage = AssistantMessage(
                        id = event.messageId,
                        content = "",
                        toolCalls = null
                    )
                    threadHistory.add(assistantMessage)
                }
                
                is TextMessageContentEvent -> {
                    // Update the last assistant message content
                    val lastMessage = threadHistory.lastOrNull()
                    if (lastMessage is AssistantMessage && lastMessage.id == event.messageId) {
                        val updatedContent = (lastMessage.content ?: "") + event.delta
                        threadHistory[threadHistory.lastIndex] = lastMessage.copy(content = updatedContent)
                    }
                }
                
                is StateSnapshotEvent -> {
                    // Update current state
                    currentState = event.snapshot
                }
                
                is StateDeltaEvent -> {
                    // Apply state delta (simplified - proper JSON patch implementation would be needed)
                    logger.debug { "State delta received - manual state update needed" }
                }
                
                else -> { /* Other events don't affect history */ }
            }
        }
    }
    
    /**
     * Clear conversation history for a specific thread
     */
    /**
     * Clear conversation history for a specific thread
     *
     * @param threadId The thread ID to clear history for, or null to clear all threads
     */
    fun clearHistory(threadId: String? = null) {
        if (threadId != null) {
            conversationHistory.remove(threadId)
        } else {
            conversationHistory.clear()
        }
    }
    
    /**
     * Get the current conversation history for a thread
     */
    /**
     * Get the current conversation history for a thread
     *
     * @param threadId The thread ID to get history for (defaults to "default")
     * @return List of messages in the conversation history
     */
    fun getHistory(threadId: String = "default"): List<Message> {
        return conversationHistory[threadId]?.toList() ?: emptyList()
    }

}

/**
 * Configuration for stateful agents
 */
class StatefulAgUi4kAgentConfig : AgUi4kAgentConfig() {
    /** Initial state for the agent */
    var initialState: JsonElement = JsonObject(emptyMap())
    
    /** Maximum conversation history length (0 = unlimited) */
    var maxHistoryLength: Int = 100
}