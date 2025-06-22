package com.contextable.agui4k.client

import com.contextable.agui4k.core.types.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class StatefulClientTest {
    
    @Test
    fun testStatefulClientConversation() = runTest {
        // Create a mock transport and state manager
        val mockTransport = MockClientTransport()
        val stateManager = InMemoryClientStateManager()
        
        // Create stateful client with single message strategy
        val client = StatefulClient(
            transport = mockTransport,
            stateManager = stateManager,
            config = StatefulClientConfig(contextStrategy = ContextStrategy.SINGLE_MESSAGE)
        )
        
        // Start a conversation
        val threadId = "test-thread-456"
        val events1 = client.continueConversation(
            content = "Hello!",
            threadId = threadId
        ).toList()
        
        // Verify state was tracked
        val messages = stateManager.getMessages(threadId)
        assertEquals(1, messages.size)
        assertTrue(messages[0] is UserMessage)
        assertEquals("Hello!", messages[0].content)
        
        // Continue the conversation
        val events2 = client.continueConversation(
            content = "How are you?",
            threadId = threadId
        ).toList()
        
        // Verify both messages are in history
        val allMessages = stateManager.getMessages(threadId)
        assertEquals(2, allMessages.size)
        assertEquals("Hello!", allMessages[0].content)
        assertEquals("How are you?", allMessages[1].content)
        
        // Verify transport was called with single messages
        assertEquals(2, mockTransport.startRunCalls.size)
    }
    
    @Test
    fun testStatefulClientFullHistory() = runTest {
        // Create a mock transport and state manager
        val mockTransport = MockClientTransport()
        val stateManager = InMemoryClientStateManager()
        
        // Create stateful client with full history strategy
        val client = StatefulClient(
            transport = mockTransport,
            stateManager = stateManager,
            config = StatefulClientConfig(contextStrategy = ContextStrategy.FULL_HISTORY)
        )
        
        val threadId = "test-thread-789"
        
        // Start with system context
        client.continueConversation(
            content = "What's your name?",
            threadId = threadId,
            systemContext = "You are a helpful assistant named Claude."
        ).toList()
        
        // Continue conversation
        client.continueConversation(
            content = "Tell me a joke.",
            threadId = threadId
        ).toList()
        
        // With FULL_HISTORY strategy, both calls should use startRun
        assertEquals(2, mockTransport.startRunCalls.size)
        
        // Check the second call which should have the full history
        val (sentMessages, sentThreadId, sentRunId) = mockTransport.startRunCalls[1]
        
        // Debug: print what we actually got
        println("Messages sent in second call: ${sentMessages.size}")
        sentMessages.forEachIndexed { index, msg ->
            println("  [$index] ${msg::class.simpleName}: ${msg.content}")
        }
        
        // The system message is added separately in the first call
        // So the second call should have 2 messages (the 2 user messages)
        assertEquals(2, sentMessages.size) // 2 user messages
        assertTrue(sentMessages[0] is UserMessage)
        assertTrue(sentMessages[1] is UserMessage)
        assertEquals(threadId, sentThreadId)
    }
    
    @Test
    fun testStatefulClientStateManagement() = runTest {
        // Create a mock transport and state manager
        val mockTransport = MockClientTransport()
        val stateManager = InMemoryClientStateManager()
        
        // Create stateful client
        val client = StatefulClient(mockTransport, stateManager)
        
        val threadId = "test-thread-state"
        
        // Get state flows
        val threadStateFlow = client.getThreadState(threadId)
        val runStateFlow = client.getRunState(threadId)
        
        // Initially no state
        assertNull(threadStateFlow.value)
        assertNull(runStateFlow.value)
        
        // Start a conversation
        client.continueConversation(
            content = "Test message",
            threadId = threadId
        ).toList()
        
        // After conversation, we should have messages
        val messages = client.getConversationHistory(threadId)
        assertEquals(1, messages.size)
        
        // Clear thread
        client.clearThread(threadId)
        
        // Verify thread was cleared
        val clearedMessages = client.getConversationHistory(threadId)
        assertEquals(0, clearedMessages.size)
    }
}