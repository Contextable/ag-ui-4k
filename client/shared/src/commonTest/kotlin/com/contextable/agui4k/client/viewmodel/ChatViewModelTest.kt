package com.contextable.agui4k.client.viewmodel

import com.contextable.agui4k.client.ui.screens.chat.ChatViewModel
import com.contextable.agui4k.client.ui.screens.chat.MessageRole
import com.contextable.agui4k.core.types.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class ChatViewModelTest {
    
    @Test
    fun testSendMessage() = runTest {
        val viewModel = ChatViewModel()
        
        // Initially no messages
        assertTrue(viewModel.state.value.messages.isEmpty())
        
        // Send a message (this would need a mock agent)
        viewModel.sendMessage("Hello, agent!")
        
        // Should have user message
        delay(100) // Give time for state update
        val messages = viewModel.state.value.messages
        assertTrue(messages.isNotEmpty())
        assertEquals(MessageRole.USER, messages.first().role)
        assertEquals("Hello, agent!", messages.first().content)
    }
    
    @Test
    fun testHandleTextMessageEvents() = runTest {
        val viewModel = ChatViewModel()
        
        // Simulate receiving text message events
        val messageId = "test-msg-1"
        
        // This would need access to the handleAgentEvent method
        // In real implementation, we'd make it internal or use a test interface
        
        // Test streaming message updates
        // Test message finalization
    }
}