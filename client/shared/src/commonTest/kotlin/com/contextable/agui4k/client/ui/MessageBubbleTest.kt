package com.contextable.agui4k.client.ui

import androidx.compose.ui.test.*
import com.contextable.agui4k.client.ui.screens.chat.DisplayMessage
import com.contextable.agui4k.client.ui.screens.chat.MessageRole
import com.contextable.agui4k.client.ui.screens.chat.components.MessageBubble
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class MessageBubbleTest {
    
    @Test
    fun testUserMessageDisplay() = runComposeUiTest {
        val message = DisplayMessage(
            id = "1",
            role = MessageRole.USER,
            content = "Hello, AI!"
        )
        
        setContent {
            MessageBubble(message = message)
        }
        
        onNodeWithText("Hello, AI!").assertExists()
    }
    
    @Test
    fun testAssistantMessageDisplay() = runComposeUiTest {
        val message = DisplayMessage(
            id = "2",
            role = MessageRole.ASSISTANT,
            content = "Hello! How can I help you?"
        )
        
        setContent {
            MessageBubble(message = message)
        }
        
        onNodeWithText("Hello! How can I help you?").assertExists()
    }
    
    @Test
    fun testStreamingIndicator() = runComposeUiTest {
        val message = DisplayMessage(
            id = "3",
            role = MessageRole.ASSISTANT,
            content = "Thinking",
            isStreaming = true
        )
        
        setContent {
            MessageBubble(message = message)
        }
        
        onNodeWithText("Thinking").assertExists()
        // Should show progress indicator when streaming
        onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate))
            .assertExists()
    }
}
