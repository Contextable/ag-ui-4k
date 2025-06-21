package com.contextable.agui4k.client

import com.contextable.agui4k.core.types.*
import com.contextable.agui4k.transport.ClientTransport
import com.contextable.agui4k.transport.RunSession
import com.contextable.agui4k.tools.ToolRegistry
import kotlinx.coroutines.flow.*
import kotlinx.datetime.Clock
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Configuration for how the StatefulClient sends conversation context.
 */
enum class ContextStrategy {
    /**
     * Send only the new message, relying on the agent to maintain conversation state.
     * Best for stateful agents that track conversation history server-side.
     */
    SINGLE_MESSAGE,
    
    /**
     * Send the full conversation history with each request.
     * Best for stateless agents that need complete context each time.
     */
    FULL_HISTORY
}

/**
 * Configuration for StatefulClient behavior.
 */
data class StatefulClientConfig(
    val contextStrategy: ContextStrategy = ContextStrategy.SINGLE_MESSAGE,
    val maxHistoryMessages: Int? = null // Limit history size for FULL_HISTORY mode
)

/**
 * A stateful client implementation that maintains conversation history and thread state.
 * 
 * This client:
 * - Maintains conversation history across multiple interactions
 * - Tracks thread and run state
 * - Provides reactive state updates through StateFlows
 * - Supports both single-message and full-history context strategies
 * - Suitable for persistent chat applications and complex multi-turn conversations
 */
class StatefulClient(
    transport: ClientTransport,
    private val stateManager: ClientStateManager = InMemoryClientStateManager(),
    private val config: StatefulClientConfig = StatefulClientConfig(),
    toolRegistry: ToolRegistry? = null
) : AbstractClient(transport, toolRegistry) {
    
    private val activeSessionsMap = mutableMapOf<String, RunSession>()
    
    /**
     * Starts a conversation in an existing thread or creates a new one.
     * The conversation history is maintained in the state manager.
     * 
     * @param content The message content to send
     * @param threadId Optional thread ID (if null, a new thread is created)
     * @param systemContext Optional system message for new threads
     * @param contextStrategy Override the default context strategy for this request
     * @return A flow of all events from the agent's response
     */
    suspend fun continueConversation(
        content: String,
        threadId: String? = null,
        systemContext: String? = null,
        contextStrategy: ContextStrategy? = null
    ): Flow<BaseEvent> {
        val finalThreadId = threadId ?: generateThreadId()
        val strategy = contextStrategy ?: config.contextStrategy
        
        logger.info { "Continuing conversation in thread $finalThreadId with strategy $strategy" }
        
        // For new threads, add system context if provided
        if (threadId == null && systemContext != null) {
            val systemMessage = createSystemMessage(systemContext)
            stateManager.addMessage(finalThreadId, systemMessage)
        }
        
        // Add the user message to history
        val userMessage = createUserMessage(content)
        stateManager.addMessage(finalThreadId, userMessage)
        
        // Prepare the message(s) to send based on strategy
        when (strategy) {
            ContextStrategy.SINGLE_MESSAGE -> {
                // Send only the new user message
                return startConversation(userMessage, finalThreadId)
            }
            ContextStrategy.FULL_HISTORY -> {
                // For full history, we need to send all messages
                // This requires a different approach - we'll start a new run with the full context
                return startConversationWithHistory(finalThreadId)
            }
        }
    }
    
    /**
     * Sends a tool response message in an existing conversation.
     * 
     * @param threadId The thread ID
     * @param toolCallId The ID of the tool call being responded to
     * @param content The tool response content
     * @param contextStrategy Override the default context strategy for this request
     * @return A flow of events from the agent's response
     */
    suspend fun sendToolResponse(
        threadId: String,
        toolCallId: String,
        content: String,
        contextStrategy: ContextStrategy? = null
    ): Flow<BaseEvent> {
        val strategy = contextStrategy ?: config.contextStrategy
        
        logger.info { "Sending tool response for call $toolCallId in thread $threadId with strategy $strategy" }
        
        val toolMessage = createToolMessage(toolCallId, content)
        stateManager.addMessage(threadId, toolMessage)
        
        when (strategy) {
            ContextStrategy.SINGLE_MESSAGE -> {
                // Get the active session for this thread
                val session = activeSessionsMap[threadId]
                    ?: throw IllegalStateException("No active session for thread $threadId")
                
                // Send the tool message through the existing session
                sendMessage(session, toolMessage)
                
                // Return the ongoing event stream (session should continue)
                return session.events
            }
            ContextStrategy.FULL_HISTORY -> {
                // For full history mode, we need to start a new request with complete context
                return startConversationWithHistory(threadId)
            }
        }
    }
    
    /**
     * Sends a request with full conversation history regardless of the default strategy.
     * 
     * @param content The message content to send
     * @param threadId The thread ID
     * @return A flow of events from the agent's response
     */
    suspend fun sendWithFullHistory(
        content: String,
        threadId: String
    ): Flow<BaseEvent> {
        return continueConversation(
            content = content,
            threadId = threadId,
            contextStrategy = ContextStrategy.FULL_HISTORY
        )
    }
    
    /**
     * Sends a request with only the new message regardless of the default strategy.
     * 
     * @param content The message content to send
     * @param threadId The thread ID
     * @return A flow of events from the agent's response
     */
    suspend fun sendSingleMessage(
        content: String,
        threadId: String
    ): Flow<BaseEvent> {
        return continueConversation(
            content = content,
            threadId = threadId,
            contextStrategy = ContextStrategy.SINGLE_MESSAGE
        )
    }
    
    /**
     * Gets the conversation history for a thread.
     * 
     * @param threadId The thread ID
     * @return List of messages in chronological order
     */
    suspend fun getConversationHistory(threadId: String): List<Message> {
        return stateManager.getMessages(threadId)
    }
    
    /**
     * Gets the current state of a thread.
     * 
     * @param threadId The thread ID
     * @return StateFlow of the thread state
     */
    fun getThreadState(threadId: String): StateFlow<Thread?> {
        return stateManager.getThreadStateFlow(threadId)
    }
    
    /**
     * Gets the current run state for a thread.
     * 
     * @param threadId The thread ID
     * @return StateFlow of the run state
     */
    fun getRunState(threadId: String): StateFlow<Run?> {
        return stateManager.getRunStateFlow(threadId)
    }
    
    /**
     * Clears the conversation history for a thread.
     * 
     * @param threadId The thread ID
     */
    suspend fun clearThread(threadId: String) {
        logger.info { "Clearing thread $threadId" }
        
        // Close any active session
        activeSessionsMap[threadId]?.close()
        activeSessionsMap.remove(threadId)
        
        // Clear state
        stateManager.removeThread(threadId)
    }
    
    /**
     * Gets all known thread IDs.
     * 
     * @return Set of thread IDs
     */
    suspend fun getAllThreads(): Set<String> {
        return stateManager.getAllThreadIds()
    }
    
    /**
     * Starts a conversation with full conversation history.
     * This is used for the FULL_HISTORY context strategy.
     */
    private suspend fun startConversationWithHistory(threadId: String): Flow<BaseEvent> {
        val history = stateManager.getMessages(threadId)
        
        // Apply history limit if configured
        val messagesToSend = config.maxHistoryMessages?.let { limit ->
            if (history.size > limit) {
                history.takeLast(limit)
            } else {
                history
            }
        } ?: history
        
        // Check if we have any messages
        if (messagesToSend.isEmpty()) {
            throw IllegalStateException("No messages in conversation history for thread $threadId")
        }
        
        logger.info { 
            "Starting conversation with ${messagesToSend.size} messages in history for thread $threadId" 
        }
        
        // Use the new method that sends all messages
        return startConversationWithMessages(messagesToSend, threadId)
    }
    
    /**
     * Override event processing to update state and maintain history.
     */
    override suspend fun processEvent(
        event: BaseEvent,
        session: RunSession
    ): BaseEvent? {
        // Extract thread ID from the event or session context
        val threadId = extractThreadIdFromEvent(event) ?: return event
        
        // Update state based on event type
        when (event) {
            is RunStartedEvent -> {
                val runState = Run(
                    runId = event.runId,
                    threadId = event.threadId,
                    status = RunStatus.STARTED,
                    startTime = kotlinx.datetime.Clock.System.now(),
                    messages = emptyList()
                )
                stateManager.updateRunState(threadId, runState)
                // Track the active session
                activeSessionsMap[threadId] = session
            }
            is RunFinishedEvent -> {
                val currentRun = stateManager.getCurrentRunState(threadId)
                if (currentRun != null) {
                    val finishedRun = currentRun.copy(
                        status = RunStatus.COMPLETED,
                        endTime = kotlinx.datetime.Clock.System.now()
                    )
                    stateManager.updateRunState(threadId, finishedRun)
                }
                stateManager.clearRunState(threadId)
                activeSessionsMap.remove(threadId)
            }
            is RunErrorEvent -> {
                val currentRun = stateManager.getCurrentRunState(threadId)
                if (currentRun != null) {
                    val errorRun = currentRun.copy(
                        status = RunStatus.ERROR,
                        endTime = kotlinx.datetime.Clock.System.now(),
                        error = RunError(
                            message = event.message,
                            code = event.code
                        )
                    )
                    stateManager.updateRunState(threadId, errorRun)
                }
                stateManager.clearRunState(threadId)
                activeSessionsMap.remove(threadId)
            }
            is TextMessageStartEvent -> {
                // Create an assistant message for text streaming
                val assistantMessage = AssistantMessage(
                    id = event.messageId,
                    content = "" // Will be filled by TextMessageContentEvent
                )
                stateManager.addMessage(threadId, assistantMessage)
            }
            is TextMessageContentEvent -> {
                // Update message content (in real implementation, you'd accumulate deltas)
                // For now, we'll just track that content is being streamed
            }
            is TextMessageEndEvent -> {
                // Mark message as complete
                // In a full implementation, you'd finalize the message content
            }
            else -> {
                // Other events don't affect persistent state
            }
        }
        
        return event
    }
    
    /**
     * Handle run completion for stateful interactions.
     */
    override suspend fun onRunFinished(
        session: RunSession,
        event: RunFinishedEvent
    ) {
        logger.info { "Stateful run finished" }
        
        // Clean up active session tracking
        val threadId = extractThreadIdFromEvent(event)
        if (threadId != null) {
            activeSessionsMap.remove(threadId)
        }
    }
    
    /**
     * Handle run errors for stateful interactions.
     */
    override suspend fun onRunError(
        session: RunSession,
        event: RunErrorEvent
    ) {
        logger.warn { "Stateful run error: ${event.message} (${event.code})" }
        
        // Clean up active session tracking
        val threadId = extractThreadIdFromEvent(event)
        if (threadId != null) {
            activeSessionsMap.remove(threadId)
        }
    }
    
    /**
     * Extracts thread ID from various event types.
     * This is a helper method since not all events directly contain thread IDs.
     */
    private fun extractThreadIdFromEvent(event: BaseEvent): String? {
        return when (event) {
            is RunStartedEvent -> event.threadId
            is RunFinishedEvent -> event.threadId
            is RunErrorEvent -> null // RunErrorEvent doesn't have thread context
            is TextMessageStartEvent -> null // Would need to be tracked separately
            is TextMessageContentEvent -> null
            is TextMessageEndEvent -> null
            is ToolCallStartEvent -> null
            is ToolCallArgsEvent -> null
            is ToolCallEndEvent -> null
            is StepStartedEvent -> null
            is StepFinishedEvent -> null
            is StateSnapshotEvent -> null
            is StateDeltaEvent -> null
            is MessagesSnapshotEvent -> null
            is RawEvent -> null
            is CustomEvent -> null
        }
    }
    
    private fun generateThreadId(): String = "thread_${Clock.System.now().toEpochMilliseconds()}"
    private fun generateMessageId(): String = "msg_${Clock.System.now().toEpochMilliseconds()}"
}