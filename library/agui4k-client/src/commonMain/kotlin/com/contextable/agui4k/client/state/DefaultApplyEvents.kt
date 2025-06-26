// agui4k-client/src/commonMain/kotlin/com/contextable/agui4k/client/state/DefaultApplyEvents.kt
package com.contextable.agui4k.client.state

import com.contextable.agui4k.client.agent.AgentState
import com.contextable.agui4k.core.types.*
import com.reidsync.kxjsonpatch.JsonPatch
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.*
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Data class for predictive state configuration.
 */
data class PredictStateValue(
    val state_key: String,
    val tool: String,
    val tool_argument: String? = null
)

/**
 * Default implementation of event application logic with JSON Patch support.
 * Updates agent state based on received events.
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