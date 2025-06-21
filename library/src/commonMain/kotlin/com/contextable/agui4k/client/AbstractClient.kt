package com.contextable.agui4k.client

import com.contextable.agui4k.core.types.*
import com.contextable.agui4k.transport.ClientTransport
import com.contextable.agui4k.transport.RunSession
import com.contextable.agui4k.tools.ToolRegistry
import kotlinx.coroutines.flow.*
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Abstract base class for all client implementations.
 * 
 * Provides common functionality for interacting with AI agents including:
 * - Message sending and receiving
 * - Event stream processing
 * - State management integration
 * - Error handling patterns
 */
abstract class AbstractClient(
    protected val transport: ClientTransport,
    protected val toolRegistry: ToolRegistry? = null
) {
    /**
     * Starts a new conversation with the agent.
     * 
     * @param message The initial message to send
     * @param threadId Optional thread ID to use (if null, a new thread is created)
     * @return A flow of events from the agent
     */
    suspend fun startConversation(
        message: Message,
        threadId: String? = null
    ): Flow<BaseEvent> {
        logger.info { "Starting conversation with message: ${message.content}" }
        
        val tools = toolRegistry?.getAllTools()
        val session = transport.startRun(message, threadId, tools)
        return processEventStream(session)
    }
    
    /**
     * Starts a new conversation with multiple messages (full history).
     * 
     * @param messages The messages to send (in chronological order)
     * @param threadId Optional thread ID to use (if null, a new thread is created)
     * @return A flow of events from the agent
     */
    suspend fun startConversationWithMessages(
        messages: List<Message>,
        threadId: String? = null
    ): Flow<BaseEvent> {
        logger.info { "Starting conversation with ${messages.size} messages" }
        
        val tools = toolRegistry?.getAllTools()
        val session = transport.startRunWithMessages(messages, threadId, tools)
        return processEventStream(session)
    }
    
    /**
     * Continues an existing conversation by sending a message within a session.
     * 
     * @param session The active run session
     * @param message The message to send
     */
    suspend fun sendMessage(session: RunSession, message: Message) {
        logger.info { "Sending message in existing session: ${message.content}" }
        session.sendMessage(message)
    }
    
    /**
     * Processes the event stream from a run session, applying client-specific logic.
     * 
     * @param session The run session to process events from
     * @return A flow of processed events
     */
    protected open suspend fun processEventStream(session: RunSession): Flow<BaseEvent> = flow {
        try {
            session.events.collect { event ->
                logger.debug { "Received event: ${event::class.simpleName}" }
                
                // Apply client-specific event processing
                val processedEvent = processEvent(event, session)
                if (processedEvent != null) {
                    emit(processedEvent)
                }
                
                // Handle session lifecycle
                when (event) {
                    is RunFinishedEvent -> {
                        logger.info { "Run finished" }
                        onRunFinished(session, event)
                    }
                    is RunErrorEvent -> {
                        logger.error { "Run error: ${event.message}" }
                        onRunError(session, event)
                    }
                    else -> {}
                }
            }
        } finally {
            session.close()
        }
    }
    
    /**
     * Processes a single event. Subclasses can override this to implement
     * custom event processing logic (filtering, transformation, state updates, etc.).
     * 
     * @param event The event to process
     * @param session The session that produced the event
     * @return The processed event, or null to filter it out
     */
    protected open suspend fun processEvent(
        event: BaseEvent,
        session: RunSession
    ): BaseEvent? = event
    
    /**
     * Called when a run finishes successfully.
     * Subclasses can override this to implement cleanup or state updates.
     * 
     * @param session The session that finished
     * @param event The run finished event
     */
    protected open suspend fun onRunFinished(
        session: RunSession,
        event: RunFinishedEvent
    ) {
        // Default implementation does nothing
    }
    
    /**
     * Called when a run encounters an error.
     * Subclasses can override this to implement error handling or recovery.
     * 
     * @param session The session that encountered an error
     * @param event The run error event
     */
    protected open suspend fun onRunError(
        session: RunSession,
        event: RunErrorEvent
    ) {
        // Default implementation does nothing
    }
    
    /**
     * Creates a system message with the given content.
     * System messages are used to establish context, provide instructions,
     * or configure the agent's behavior.
     * 
     * @param content The system message content
     * @return A system message
     */
    protected fun createSystemMessage(content: String): Message {
        return SystemMessage(
            id = generateMessageId(),
            content = content
        )
    }
    
    /**
     * Creates a user message with the given content.
     * 
     * @param content The message content
     * @return A user message
     */
    protected fun createUserMessage(content: String): Message {
        return UserMessage(
            id = generateMessageId(),
            content = content
        )
    }
    
    /**
     * Creates a tool message with the given tool call ID and content.
     * 
     * @param toolCallId The ID of the tool call this message responds to
     * @param content The tool response content
     * @return A tool message
     */
    protected fun createToolMessage(toolCallId: String, content: String): Message {
        return ToolMessage(
            id = generateMessageId(),
            content = content,
            toolCallId = toolCallId
        )
    }
    
    private fun generateMessageId(): String = "msg_${System.currentTimeMillis()}"
}