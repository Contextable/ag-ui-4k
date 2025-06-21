package com.contextable.agui4k.client

import com.contextable.agui4k.core.types.*
import com.contextable.agui4k.transport.ClientTransport
import com.contextable.agui4k.transport.RunSession
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class StatelessClientTest {
    
    @Test
    fun testStatelessClientSendMessage() = runTest {
        // Create a mock transport
        val mockTransport = MockClientTransport()
        
        // Create stateless client
        val client = StatelessClient(mockTransport)
        
        // Send a message
        val events = client.sendMessage("Hello, AI!").toList()
        
        // Verify the transport was called correctly
        assertEquals(1, mockTransport.startRunCalls.size)
        val (message, threadId) = mockTransport.startRunCalls.first()
        assertTrue(message is UserMessage)
        assertEquals("Hello, AI!", message.content)
        // StatelessClient doesn't provide threadId by default
        assertNull(threadId)
        
        // Verify events were returned
        assertEquals(3, events.size)
        assertTrue(events[0] is RunStartedEvent)
        assertTrue(events[1] is TextMessageStartEvent)
        assertTrue(events[2] is RunFinishedEvent)
    }
    
    @Test
    fun testStatelessClientWithSystemContext() = runTest {
        // Create a mock transport
        val mockTransport = MockClientTransport()
        
        // Create stateless client
        val client = StatelessClient(mockTransport)
        
        // Send a message with system context
        val events = client.sendMessage(
            content = "What's the weather?",
            systemContext = "You are a weather assistant.",
            threadId = "test-thread-123"
        ).toList()
        
        // Verify the transport was called correctly
        assertEquals(1, mockTransport.startRunCalls.size)
        val (message, threadId) = mockTransport.startRunCalls.first()
        assertTrue(message is UserMessage)
        assertEquals("What's the weather?", message.content)
        assertEquals("test-thread-123", threadId)
        
        // Verify events were returned
        assertTrue(events.isNotEmpty())
    }
}

/**
 * Mock implementation of ClientTransport for testing.
 */
class MockClientTransport : ClientTransport {
    val startRunCalls = mutableListOf<Pair<Message, String?>>()
    val startRunWithMessagesCalls = mutableListOf<Pair<List<Message>, String?>>()
    
    override suspend fun startRun(message: Message, threadId: String?, tools: List<Tool>?): RunSession {
        startRunCalls.add(message to threadId)
        return MockRunSession(threadId ?: "mock-thread-${System.currentTimeMillis()}")
    }
    
    override suspend fun startRunWithMessages(messages: List<Message>, threadId: String?, tools: List<Tool>?): RunSession {
        startRunWithMessagesCalls.add(messages to threadId)
        return MockRunSession(threadId ?: "mock-thread-${System.currentTimeMillis()}")
    }
}

/**
 * Mock implementation of RunSession for testing.
 */
class MockRunSession(private val threadId: String) : RunSession {
    
    override val isActive: StateFlow<Boolean> = MutableStateFlow(true)
    
    override val events: Flow<BaseEvent> = flow {
        // Emit some mock events
        emit(RunStartedEvent(
            threadId = threadId,
            runId = "run-${System.currentTimeMillis()}"
        ))
        
        emit(TextMessageStartEvent(
            messageId = "msg-${System.currentTimeMillis()}",
            role = "assistant"
        ))
        
        emit(RunFinishedEvent(
            threadId = threadId,
            runId = "run-${System.currentTimeMillis()}"
        ))
    }
    
    override suspend fun sendMessage(message: Message) {
        // Mock implementation - do nothing
    }
    
    override suspend fun close() {
        // Mock implementation - do nothing
    }
}