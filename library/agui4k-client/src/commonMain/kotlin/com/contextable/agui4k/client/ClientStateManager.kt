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
package com.contextable.agui4k.client

import com.contextable.agui4k.core.types.Message

/**
 * Manages client-side state for conversations.
 * 
 * This interface defines how stateful clients can persist and retrieve
 * conversation history for different threads.
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
}

/**
 * Simple in-memory implementation of ClientStateManager.
 * 
 * This implementation stores conversation history in memory and is suitable for:
 * - Short-lived applications
 * - Testing scenarios
 * - Applications that don't require persistence across restarts
 */
class SimpleClientStateManager : ClientStateManager {
    
    private val threadMessages = mutableMapOf<String, MutableList<Message>>()
    
    override suspend fun addMessage(threadId: String, message: Message) {
        threadMessages.getOrPut(threadId) { mutableListOf() }.add(message)
    }
    
    override suspend fun getMessages(threadId: String): List<Message> {
        return threadMessages[threadId]?.toList() ?: emptyList()
    }
    
    override suspend fun clearMessages(threadId: String) {
        threadMessages[threadId]?.clear()
    }
    
    override suspend fun getAllThreadIds(): Set<String> {
        return threadMessages.keys.toSet()
    }
    
    override suspend fun removeThread(threadId: String) {
        threadMessages.remove(threadId)
    }
}