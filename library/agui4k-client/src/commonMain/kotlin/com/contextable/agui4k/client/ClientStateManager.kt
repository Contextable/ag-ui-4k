package com.contextable.agui4k.client

import com.contextable.agui4k.client.state.*
import com.contextable.agui4k.core.types.Message
import kotlinx.coroutines.flow.StateFlow

/**
 * Manages client-side state for conversations, threads, and runs.
 * 
 * This interface defines how stateful clients can persist and retrieve
 * conversation history, track active sessions, and manage thread state.
 */
interface ClientStateManager {
    
    /**
     * Stores a message in the conversation history for a thread.
     * 
     * @param threadId The thread ID
     * @param message The message to store
     */
    suspend fun addMessage(threadId: String, message: Message)
    
    /**
     * Retrieves the complete message history for a thread.
     * 
     * @param threadId The thread ID
     * @return List of messages in chronological order
     */
    suspend fun getMessages(threadId: String): List<Message>
    
    /**
     * Clears all messages for a thread.
     * 
     * @param threadId The thread ID
     */
    suspend fun clearMessages(threadId: String)
    
    /**
     * Gets the current thread state.
     * 
     * @param threadId The thread ID
     * @return The thread state, or null if not found
     */
    suspend fun getThreadState(threadId: String): Thread?
    
    /**
     * Updates the thread state.
     * 
     * @param threadId The thread ID
     * @param state The new thread state
     */
    suspend fun updateThreadState(threadId: String, state: Thread)
    
    /**
     * Gets the current run state for a thread.
     * 
     * @param threadId The thread ID
     * @return The run state, or null if no active run
     */
    suspend fun getCurrentRunState(threadId: String): Run?
    
    /**
     * Updates the run state for a thread.
     * 
     * @param threadId The thread ID
     * @param runState The new run state
     */
    suspend fun updateRunState(threadId: String, runState: Run)
    
    /**
     * Removes the run state for a thread (when run completes).
     * 
     * @param threadId The thread ID
     */
    suspend fun clearRunState(threadId: String)
    
    /**
     * Gets all known thread IDs.
     * 
     * @return Set of thread IDs
     */
    suspend fun getAllThreadIds(): Set<String>
    
    /**
     * Removes all data for a thread.
     * 
     * @param threadId The thread ID
     */
    suspend fun removeThread(threadId: String)
    
    /**
     * Gets a flow of the current thread state for reactive updates.
     * 
     * @param threadId The thread ID
     * @return StateFlow of the thread state
     */
    fun getThreadStateFlow(threadId: String): StateFlow<Thread?>
    
    /**
     * Gets a flow of the current run state for reactive updates.
     * 
     * @param threadId The thread ID
     * @return StateFlow of the run state
     */
    fun getRunStateFlow(threadId: String): StateFlow<Run?>
}

/**
 * In-memory implementation of ClientStateManager.
 * 
 * This implementation stores all state in memory and is suitable for:
 * - Short-lived applications
 * - Testing scenarios
 * - Applications that don't require persistence across restarts
 */
class InMemoryClientStateManager : ClientStateManager {
    
    private val threadMessages = mutableMapOf<String, MutableList<Message>>()
    private val threadStates = mutableMapOf<String, Thread>()
    private val runStates = mutableMapOf<String, Run>()
    
    // StateFlow management
    private val threadStateFlows = mutableMapOf<String, kotlinx.coroutines.flow.MutableStateFlow<Thread?>>()
    private val runStateFlows = mutableMapOf<String, kotlinx.coroutines.flow.MutableStateFlow<Run?>>()
    
    override suspend fun addMessage(threadId: String, message: Message) {
        threadMessages.getOrPut(threadId) { mutableListOf() }.add(message)
    }
    
    override suspend fun getMessages(threadId: String): List<Message> {
        return threadMessages[threadId]?.toList() ?: emptyList()
    }
    
    override suspend fun clearMessages(threadId: String) {
        threadMessages[threadId]?.clear()
    }
    
    override suspend fun getThreadState(threadId: String): Thread? {
        return threadStates[threadId]
    }
    
    override suspend fun updateThreadState(threadId: String, state: Thread) {
        threadStates[threadId] = state
        getOrCreateThreadStateFlow(threadId).value = state
    }
    
    override suspend fun getCurrentRunState(threadId: String): Run? {
        return runStates[threadId]
    }
    
    override suspend fun updateRunState(threadId: String, runState: Run) {
        runStates[threadId] = runState
        getOrCreateRunStateFlow(threadId).value = runState
    }
    
    override suspend fun clearRunState(threadId: String) {
        runStates.remove(threadId)
        getOrCreateRunStateFlow(threadId).value = null
    }
    
    override suspend fun getAllThreadIds(): Set<String> {
        return (threadMessages.keys + threadStates.keys + runStates.keys).toSet()
    }
    
    override suspend fun removeThread(threadId: String) {
        threadMessages.remove(threadId)
        threadStates.remove(threadId)
        runStates.remove(threadId)
        threadStateFlows.remove(threadId)
        runStateFlows.remove(threadId)
    }
    
    override fun getThreadStateFlow(threadId: String): StateFlow<Thread?> {
        return getOrCreateThreadStateFlow(threadId)
    }
    
    override fun getRunStateFlow(threadId: String): StateFlow<Run?> {
        return getOrCreateRunStateFlow(threadId)
    }
    
    private fun getOrCreateThreadStateFlow(threadId: String): kotlinx.coroutines.flow.MutableStateFlow<Thread?> {
        return threadStateFlows.getOrPut(threadId) {
            kotlinx.coroutines.flow.MutableStateFlow(threadStates[threadId])
        }
    }
    
    private fun getOrCreateRunStateFlow(threadId: String): kotlinx.coroutines.flow.MutableStateFlow<Run?> {
        return runStateFlows.getOrPut(threadId) {
            kotlinx.coroutines.flow.MutableStateFlow(runStates[threadId])
        }
    }
}