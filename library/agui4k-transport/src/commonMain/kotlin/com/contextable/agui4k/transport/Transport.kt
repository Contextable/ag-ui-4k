package com.contextable.agui4k.transport

import com.contextable.agui4k.core.types.Context
import com.contextable.agui4k.core.types.Message
import com.contextable.agui4k.core.types.Tool

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
     * Start a new run with one or more messages.
     * 
     * @param messages The messages to send to the agent (in chronological order)
     * @param threadId Optional thread ID for conversation continuity. If null, a new thread ID will be generated.
     * @param runId Optional run ID. If provided, used for client-side state correlation (StatefulClient). If null, agent generates it (StatelessClient).
     * @param state Optional client-side state to send to the agent.
     * @param tools Optional list of tools available to the agent. If null, no tools are provided.
     * @param context Optional list of context values to provide to the agent.
     * @param forwardedProps Optional properties to forward to the agent.
     * @return A [RunSession] for managing the run lifecycle and receiving events
     */
    suspend fun startRun(
        messages: List<Message>, 
        threadId: String? = null, 
        runId: String? = null, 
        state: Any? = null,
        tools: List<Tool>? = null,
        context: List<Context>? = null,
        forwardedProps: Any? = null
    ): RunSession
    
}