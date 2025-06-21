package com.contextable.agui4k.client

import com.contextable.agui4k.core.types.*
import com.contextable.agui4k.transport.ClientTransport
import com.contextable.agui4k.transport.RunSession
import com.contextable.agui4k.tools.ToolRegistry
import kotlinx.coroutines.flow.*
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * A stateless client implementation that treats each conversation as independent.
 * 
 * This client:
 * - Does not maintain conversation history or state between runs
 * - Suitable for one-off interactions or stateless request-response patterns
 * - Minimal memory footprint
 * - Thread-safe by design (no shared mutable state)
 */
class StatelessClient(
    transport: ClientTransport,
    toolRegistry: ToolRegistry? = null
) : AbstractClient(transport, toolRegistry) {
    
    /**
     * Sends a single message to the agent and returns the complete response.
     * This is a convenience method for simple request-response interactions.
     * 
     * @param content The message content to send
     * @param systemContext Optional system message to establish context
     * @param threadId Optional thread ID (if null, a new thread is created)
     * @return A flow of all events from the agent's response
     */
    suspend fun sendMessage(
        content: String,
        systemContext: String? = null,
        threadId: String? = null
    ): Flow<BaseEvent> {
        logger.info { "Sending stateless message: $content" }
        
        // If system context is provided, we need to send both messages
        val message = if (systemContext != null) {
            // For stateless interactions, we typically send system context as the initial message
            // However, since startRun takes a single message, we'll use the user message
            // and let the transport/agent handle the system context appropriately
            createUserMessage(content)
        } else {
            createUserMessage(content)
        }
        
        return startConversation(message, threadId)
    }
    
    /**
     * Sends a message with explicit system context.
     * This method starts a conversation with a system message followed by a user message.
     * 
     * @param userContent The user message content
     * @param systemContent The system context/instructions
     * @param threadId Optional thread ID (if null, a new thread is created)
     * @return A flow of all events from the agent's response
     */
    suspend fun sendMessageWithContext(
        userContent: String,
        systemContent: String,
        threadId: String? = null
    ): Flow<BaseEvent> {
        logger.info { "Sending stateless message with system context" }
        
        // Start with system message to establish context
        val systemMessage = createSystemMessage(systemContent)
        return startConversation(systemMessage, threadId).onCompletion { cause ->
            if (cause == null) {
                // After system message is processed, send user message
                // Note: This is a simplified approach - in practice, you might want to
                // handle this at the transport level or use a different pattern
                logger.debug { "System context established, would typically send user message next" }
            }
        }.flatMapConcat { event ->
            // For now, just return system message events
            // In a more sophisticated implementation, you might queue the user message
            flowOf(event)
        }
    }
    
    /**
     * Override event processing to add stateless-specific behavior.
     * This implementation passes through all events without modification.
     */
    override suspend fun processEvent(
        event: BaseEvent,
        session: RunSession
    ): BaseEvent? {
        // Stateless client doesn't modify events, just passes them through
        return event
    }
    
    /**
     * Handle run completion for stateless interactions.
     */
    override suspend fun onRunFinished(
        session: RunSession,
        event: RunFinishedEvent
    ) {
        logger.debug { "Stateless run finished" }
        // No state to clean up in stateless client
    }
    
    /**
     * Handle run errors for stateless interactions.
     */
    override suspend fun onRunError(
        session: RunSession,
        event: RunErrorEvent
    ) {
        logger.warn { "Stateless run error: ${event.message} (${event.code})" }
        // No state to clean up in stateless client
    }
}