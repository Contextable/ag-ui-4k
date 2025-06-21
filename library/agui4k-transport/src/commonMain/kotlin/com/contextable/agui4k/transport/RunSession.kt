package com.contextable.agui4k.transport

import com.contextable.agui4k.core.types.BaseEvent
import com.contextable.agui4k.core.types.Message
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Represents an active run session with an AG-UI agent.
 * 
 * A session is created when a run starts and remains active until the run completes
 * or encounters an error. During the session, events can be received and additional
 * messages can be sent (e.g., tool results).
 */
interface RunSession {
    /**
     * Flow of events received from the agent during this run.
     * 
     * The flow will emit events in the order they are received and will complete
     * when the run finishes (either successfully or with an error).
     */
    val events: Flow<BaseEvent>
    
    /**
     * Send an additional message within the same run.
     * 
     * This is typically used to send tool results back to the agent after
     * receiving a tool call event.
     * 
     * @param message The message to send (usually a ToolMessage)
     * @throws IllegalStateException if the run is no longer active
     */
    suspend fun sendMessage(message: Message)
    
    /**
     * Indicates whether this run session is still active.
     * 
     * A session becomes inactive when:
     * - The run completes successfully (RunFinishedEvent)
     * - The run encounters an error (RunErrorEvent)
     * - The connection is lost
     * - The session is explicitly closed
     */
    val isActive: StateFlow<Boolean>
    
    /**
     * Close this session and release any resources.
     * 
     * After calling this method:
     * - No more events will be emitted
     * - [sendMessage] will throw an exception
     * - [isActive] will become false
     * 
     * This method is idempotent and can be called multiple times safely.
     */
    suspend fun close()
}