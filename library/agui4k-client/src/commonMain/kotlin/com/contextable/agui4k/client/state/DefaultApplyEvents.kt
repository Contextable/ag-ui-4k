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
package com.contextable.agui4k.client.state

import com.contextable.agui4k.client.agent.AgentState
import com.contextable.agui4k.core.types.*
import com.reidsync.kxjsonpatch.JsonPatch
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.*
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Configuration for predictive state updates during tool execution.
 * 
 * This class defines how to update the agent state based on incoming tool arguments
 * before the tool execution is complete. This allows for optimistic UI updates
 * and improved user experience.
 * 
 * @param state_key The JSON pointer path in the state to update
 * @param tool The name of the tool whose arguments should trigger state updates
 * @param tool_argument Optional specific argument name to extract from tool arguments.
 *                      If null, the entire arguments object is used.
 */
data class PredictStateValue(
    val state_key: String,
    val tool: String,
    val tool_argument: String? = null
)

/**
 * Default implementation of event application logic with comprehensive event handling.
 * 
 * This function transforms a stream of AG-UI protocol events into a stream of agent states.
 * It handles all standard event types and maintains consistency between messages and state.
 * 
 * Key features:
 * - Handles all AG-UI protocol events (text messages, tool calls, state changes)
 * - Applies JSON Patch operations for state deltas
 * - Supports predictive state updates during tool execution
 * - Maintains message history and tool call tracking
 * - Provides error handling and recovery for state operations
 * - Integrates with custom state change handlers
 * 
 * Event Processing:
 * - Text message events: Build and update assistant messages incrementally
 * - Tool call events: Track tool calls and their arguments as they stream in
 * - State events: Apply snapshots and deltas using RFC 6902 JSON Patch
 * - Custom events: Handle special events like predictive state configuration
 * 
 * @param input The initial agent input containing messages, state, and configuration
 * @param events Stream of events from the agent to process
 * @param stateHandler Optional handler for state change notifications and error handling
 * @return Flow of agent states as events are processed
 * 
 * @see AgentState
 * @see BaseEvent
 * @see StateChangeHandler
 */
fun defaultApplyEvents(
    input: RunAgentInput,
    events: Flow<BaseEvent>,
    stateHandler: StateChangeHandler? = null
): Flow<AgentState> {
    // Mutable state copies
    val messages = input.messages.toMutableList()
    var state = input.state
    var predictState: List<PredictStateValue>? = null
    
    return events.transform { event ->
        when (event) {
            is TextMessageStartEvent -> {
                messages.add(
                    AssistantMessage(
                        id = event.messageId,
                        content = ""
                    )
                )
                emit(AgentState(messages = messages.toList()))
            }
            
            is TextMessageContentEvent -> {
                val lastMessage = messages.lastOrNull() as? AssistantMessage
                if (lastMessage != null && lastMessage.id == event.messageId) {
                    messages[messages.lastIndex] = lastMessage.copy(
                        content = (lastMessage.content ?: "") + event.delta
                    )
                    emit(AgentState(messages = messages.toList()))
                }
            }
            
            is TextMessageEndEvent -> {
                // No state update needed
            }
            
            is ToolCallStartEvent -> {
                val targetMessage = when {
                    event.parentMessageId != null && 
                    messages.lastOrNull()?.id == event.parentMessageId -> {
                        messages.last() as? AssistantMessage
                    }
                    else -> null
                }
                
                if (targetMessage != null) {
                    val updatedCalls = (targetMessage.toolCalls ?: emptyList()) + ToolCall(
                        id = event.toolCallId,
                        function = FunctionCall(
                            name = event.toolCallName,
                            arguments = ""
                        )
                    )
                    messages[messages.lastIndex] = targetMessage.copy(toolCalls = updatedCalls)
                } else {
                    messages.add(
                        AssistantMessage(
                            id = event.parentMessageId ?: event.toolCallId,
                            content = null,
                            toolCalls = listOf(
                                ToolCall(
                                    id = event.toolCallId,
                                    function = FunctionCall(
                                        name = event.toolCallName,
                                        arguments = ""
                                    )
                                )
                            )
                        )
                    )
                }
                emit(AgentState(messages = messages.toList()))
            }
            
            is ToolCallArgsEvent -> {
                val lastMessage = messages.lastOrNull() as? AssistantMessage
                val toolCalls = lastMessage?.toolCalls?.toMutableList()
                val lastToolCall = toolCalls?.lastOrNull()
                
                if (lastToolCall != null && lastToolCall.id == event.toolCallId) {
                    val updatedCall = lastToolCall.copy(
                        function = lastToolCall.function.copy(
                            arguments = lastToolCall.function.arguments + event.delta
                        )
                    )
                    toolCalls[toolCalls.lastIndex] = updatedCall
                    messages[messages.lastIndex] = lastMessage.copy(toolCalls = toolCalls)
                    
                    // Handle predictive state updates
                    var stateUpdated = false
                    predictState?.find { it.tool == updatedCall.function.name }?.let { config ->
                        try {
                            val newState = updatePredictiveState(
                                state,
                                updatedCall.function.arguments,
                                config
                            )
                            if (newState != null) {
                                state = newState
                                stateUpdated = true
                            }
                        } catch (e: Exception) {
                            logger.debug { "Failed to update predictive state: ${e.message}" }
                        }
                    }
                    
                    if (stateUpdated) {
                        emit(AgentState(messages = messages.toList(), state = state))
                    } else {
                        emit(AgentState(messages = messages.toList()))
                    }
                } else {
                    emit(AgentState(messages = messages.toList()))
                }
            }
            
            is ToolCallEndEvent -> {
                // No state update needed
            }
            
            is StateSnapshotEvent -> {
                state = event.snapshot
                stateHandler?.onStateSnapshot(state)
                emit(AgentState(state = state))
            }
            
            is StateDeltaEvent -> {
                try {
                    // Use JsonPatch library for proper patch application
                    state = JsonPatch.apply(event.delta, state)
                    stateHandler?.onStateDelta(event.delta)
                    emit(AgentState(state = state))
                } catch (e: Exception) {
                    logger.error(e) { "Failed to apply state delta" }
                    stateHandler?.onStateError(e, event.delta)
                }
            }
            
            is MessagesSnapshotEvent -> {
                messages.clear()
                messages.addAll(event.messages)
                emit(AgentState(messages = messages.toList()))
            }
            
            is CustomEvent -> {
                if (event.name == "PredictState") {
                    predictState = parsePredictState(event.value)
                }
            }
            
            is StepFinishedEvent -> {
                // Reset predictive state after step is finished
                predictState = null
            }
            
            else -> {
                // Other events don't affect state
            }
        }
    }
}

/**
 * Parses predictive state configuration from a JSON element.
 */
private fun parsePredictState(value: JsonElement): List<PredictStateValue>? {
    return try {
        value.jsonArray.map { element ->
            val obj = element.jsonObject
            PredictStateValue(
                state_key = obj["state_key"]!!.jsonPrimitive.content,
                tool = obj["tool"]!!.jsonPrimitive.content,
                tool_argument = obj["tool_argument"]?.jsonPrimitive?.content
            )
        }
    } catch (e: Exception) {
        logger.debug { "Failed to parse predictive state: ${e.message}" }
        null
    }
}

/**
 * Updates state based on tool arguments and predictive state configuration.
 */
private fun updatePredictiveState(
    currentState: State,
    toolArgs: String,
    config: PredictStateValue
): State? {
    return try {
        // Try to parse the accumulated arguments
        val parsedArgs = Json.parseToJsonElement(toolArgs).jsonObject
        
        val newValue = if (config.tool_argument != null) {
            // Extract specific argument
            parsedArgs[config.tool_argument]
        } else {
            // Use entire arguments object
            parsedArgs
        }
        
        if (newValue != null) {
            // Create updated state
            val stateObj = currentState.jsonObject.toMutableMap()
            stateObj[config.state_key] = newValue
            JsonObject(stateObj)
        } else {
            null
        }
    } catch (e: Exception) {
        // Arguments not yet valid JSON, ignore
        null
    }
}