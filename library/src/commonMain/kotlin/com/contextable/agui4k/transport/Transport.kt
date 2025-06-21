package com.contextable.agui4k.transport

import com.contextable.agui4k.core.types.Message

/**
 * Client-side transport interface for communicating with AG-UI agents.
 * 
 * This interface provides the client perspective of AG-UI communication,
 * where the client initiates runs by sending messages and receives streaming
 * events in response.
 * 
 * Implementations handle the underlying communication protocol (HTTP/SSE, etc.)
 * and provide a consistent interface for starting runs and managing sessions.
 */
interface ClientTransport {
    /**
     * Start a new run with an initial message.
     * 
     * @param message The initial message to send to the agent
     * @param threadId Optional thread ID for conversation continuity. If null, a new thread ID will be generated.
     * @return A [RunSession] for managing the run lifecycle and receiving events
     */
    suspend fun startRun(message: Message, threadId: String? = null): RunSession
    
    /**
     * Start a new run with multiple messages (full conversation history).
     * This is useful for stateless agents that need the complete context.
     * 
     * @param messages The messages to send (in chronological order)
     * @param threadId Optional thread ID for conversation continuity. If null, a new thread ID will be generated.
     * @return A [RunSession] for managing the run lifecycle and receiving events
     */
    suspend fun startRunWithMessages(messages: List<Message>, threadId: String? = null): RunSession {
        // Default implementation: just send the last message
        // Concrete implementations should override this to send all messages
        return startRun(messages.lastOrNull() ?: throw IllegalArgumentException("No messages provided"), threadId)
    }
}