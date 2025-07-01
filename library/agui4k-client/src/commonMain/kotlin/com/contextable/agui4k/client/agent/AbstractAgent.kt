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
package com.contextable.agui4k.client.agent

import com.contextable.agui4k.core.types.*
import com.contextable.agui4k.client.state.defaultApplyEvents
import com.contextable.agui4k.client.verify.verifyEvents
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.datetime.Clock
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Base class for all agents in the AG-UI protocol.
 * Provides the core agent functionality including state management and event processing.
 */
abstract class AbstractAgent(
    config: AgentConfig = AgentConfig()
) {
    var agentId: String? = config.agentId
    val description: String = config.description
    val threadId: String = config.threadId ?: generateId()
    
    // Agent state - consider using StateFlow for reactive updates in the future
    var messages: List<Message> = config.initialMessages
        protected set
    
    var state: State = config.initialState
        protected set
    
    val debug: Boolean = config.debug
    
    // Coroutine scope for agent lifecycle
    protected val agentScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    // Current run job for cancellation
    private var currentRunJob: Job? = null
    
    /**
     * Abstract method to be implemented by concrete agents.
     * Produces the event stream for the agent run.
     */
    protected abstract fun run(input: RunAgentInput): Flow<BaseEvent>
    
    /**
     * Main entry point to run the agent.
     * Consumes events internally for state management and returns when complete.
     * Matches TypeScript AbstractAgent.runAgent(): Promise<void>
     */
    suspend fun runAgent(parameters: RunAgentParameters? = null) {
        agentId = agentId ?: generateId()
        val input = prepareRunAgentInput(parameters)
        
        currentRunJob = agentScope.launch {
            try {
                run(input)
                    .verifyEvents(debug)
                    .let { events -> apply(input, events) }
                    .let { states -> processApplyEvents(input, states) }
                    .catch { error ->
                        logger.error(error) { "Agent execution failed" }
                        onError(error)
                    }
                    .onCompletion { cause ->
                        onFinalize()
                    }
                    .collect()
            } catch (e: CancellationException) {
                logger.debug { "Agent run cancelled" }
                throw e
            } catch (e: Exception) {
                logger.error(e) { "Unexpected error in agent run" }
                onError(e)
            }
        }
        
        currentRunJob?.join()
    }

    /**
     * Returns a Flow of events that can be observed/collected.
     * 
     * IMPORTANT: This method exists due to API confusion between TypeScript and Kotlin implementations.
     * 
     * In TypeScript:
     * - AbstractAgent.runAgent(): Promise<void> - consumes events internally, returns when complete
     * - Some usage examples show .subscribe() but this appears to be from a different/legacy API
     * - The protected run() method returns Observable<BaseEvent> but is not directly accessible
     * 
     * In Kotlin:
     * - runAgent(): suspend fun - matches TypeScript behavior (consumes events, returns Unit)
     * - runAgentObservable(): Flow<BaseEvent> - exposes event stream for observation/collection
     * 
     * Use this method when you need to observe individual events as they arrive:
     * ```
     * agent.runAgentObservable(input).collect { event ->
     *     when (event.eventType) {
     *         "text_message_content" -> println("Content: ${event.delta}")
     *         // Handle other events
     *     }
     * }
     * ```
     * 
     * Use runAgent() when you just want to execute the agent and wait for completion:
     * ```
     * agent.runAgent(parameters) // Suspends until complete
     * ```
     */
    fun runAgentObservable(input: RunAgentInput): Flow<BaseEvent> {
        agentId = agentId ?: generateId()
        
        return run(input)
            .verifyEvents(debug)
            .onEach { event ->
                // Run the full state management pipeline on each individual event
                // as a side effect, preserving the original event stream
                try {
                    flowOf(event)  // Create single-event flow
                        .let { events -> apply(input, events) }
                        .let { states -> processApplyEvents(input, states) }
                        .collect() // Consume the state updates
                } catch (e: Exception) {
                    logger.warn(e) { "Error in state management pipeline for event: ${event.eventType}" }
                    // Don't rethrow - state management errors shouldn't break the event stream
                }
            }
            .catch { error ->
                logger.error(error) { "Agent execution failed" }
                onError(error)
                throw error
            }
            .onCompletion { cause ->
                onFinalize()
            }
    }

    /**
     * Convenience method to observe agent events with parameters instead of full input.
     */
    fun runAgentObservable(parameters: RunAgentParameters? = null): Flow<BaseEvent> {
        val input = prepareRunAgentInput(parameters)
        return runAgentObservable(input)
    }
    
    /**
     * Cancels the current agent run.
     */
    open fun abortRun() {
        logger.debug { "Aborting agent run" }
        currentRunJob?.cancel("Agent run aborted")
    }
    
    /**
     * Applies events to update agent state.
     * Can be overridden for custom state management.
     */
    protected open fun apply(
        input: RunAgentInput,
        events: Flow<BaseEvent>
    ): Flow<AgentState> {
        return defaultApplyEvents(input, events)
    }
    
    /**
     * Processes state updates from the apply stage.
     */
    protected open fun processApplyEvents(
        input: RunAgentInput,
        states: Flow<AgentState>
    ): Flow<AgentState> {
        return states.onEach { agentState ->
            agentState.messages?.let { 
                messages = it
                if (debug) {
                    logger.debug { "Updated messages: ${it.size} messages" }
                }
            }
            agentState.state?.let { 
                state = it
                if (debug) {
                    logger.debug { "Updated state" }
                }
            }
        }
    }
    
    /**
     * Prepares the input for running the agent.
     */
    protected open fun prepareRunAgentInput(
        parameters: RunAgentParameters?
    ): RunAgentInput {
        return RunAgentInput(
            threadId = threadId,
            runId = parameters?.runId ?: generateId(),
            tools = parameters?.tools ?: emptyList(),
            context = parameters?.context ?: emptyList(),
            forwardedProps = parameters?.forwardedProps ?: JsonObject(emptyMap()),
            state = state,
            messages = messages.toList() // defensive copy
        )
    }
    
    /**
     * Called when an error occurs during agent execution.
     */
    protected open fun onError(error: Throwable) {
        // Default implementation logs the error
        logger.error(error) { "Agent execution failed" }
    }
    
    /**
     * Called when agent execution completes (success or failure).
     */
    protected open fun onFinalize() {
        // Default implementation does nothing
        logger.debug { "Agent execution finalized" }
    }
    
    /**
     * Creates a deep copy of this agent.
     * Concrete implementations should override this method.
     */
    open fun clone(): AbstractAgent {
        throw NotImplementedError("Clone must be implemented by concrete agent classes")
    }
    
    /**
     * Cleanup resources when agent is no longer needed.
     */
    open fun dispose() {
        logger.debug { "Disposing agent" }
        currentRunJob?.cancel()
        agentScope.cancel()
    }
    
    companion object {
        private fun generateId(): String = "id_${Clock.System.now().toEpochMilliseconds()}"
    }
}

/**
 * Configuration for creating an agent.
 */
open class AgentConfig(
    open val agentId: String? = null,
    open val description: String = "",
    open val threadId: String? = null,
    open val initialMessages: List<Message> = emptyList(),
    open val initialState: State = JsonObject(emptyMap()),
    open val debug: Boolean = false
)

/**
 * HTTP-specific agent configuration extending AgentConfig.
 * Includes URL and HTTP headers for HTTP-based agent implementations.
 */
class HttpAgentConfig(
    agentId: String? = null,
    description: String = "",
    threadId: String? = null,
    initialMessages: List<Message> = emptyList(),
    initialState: State = JsonObject(emptyMap()),
    debug: Boolean = false,
    val url: String,
    val headers: Map<String, String> = emptyMap(),
    val requestTimeout: Long = 600_000L, // 10 minutes
    val connectTimeout: Long = 30_000L   // 30 seconds
) : AgentConfig(agentId, description, threadId, initialMessages, initialState, debug)

/**
 * Parameters for running an agent.
 */
data class RunAgentParameters(
    val runId: String? = null,
    val tools: List<Tool>? = null,
    val context: List<Context>? = null,
    val forwardedProps: JsonElement? = null
)

/**
 * Represents the transformed agent state.
 */
data class AgentState(
    val messages: List<Message>? = null,
    val state: State? = null
)