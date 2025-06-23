package com.contextable.agui4k.sample.client.viewmodel

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.contextable.agui4k.example.client.data.model.AgentConfig
import com.contextable.agui4k.example.client.data.model.AuthMethod
import com.contextable.agui4k.example.client.data.repository.AgentRepository
import com.contextable.agui4k.example.client.util.initializeAndroid
import com.contextable.agui4k.example.client.util.resetAndroidContext
import com.contextable.agui4k.core.types.*
import com.contextable.agui4k.example.client.ui.screens.chat.ChatViewModel
import com.contextable.agui4k.example.client.ui.screens.chat.MessageRole
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.*

/**
 * Android integration tests for ChatViewModel tool confirmation flow.
 * Tests the complete tool confirmation workflow on Android platform.
 * Runs on actual Android device/emulator where Android context is available.
 */
@RunWith(AndroidJUnit4::class)
class AndroidChatViewModelToolConfirmationTest {

    private lateinit var viewModel: ChatViewModel
    private lateinit var context: Context

    @Before
    fun setup() {
        // Reset any previous state
        resetAndroidContext()
        AgentRepository.resetInstance()

        // Initialize Android platform
        context = InstrumentationRegistry.getInstrumentation().targetContext
        initializeAndroid(context)

        // Create real ChatViewModel (will work now that Android is initialized)
        viewModel = ChatViewModel()

        // Set up a test agent
        val testAgent = AgentConfig(
            id = "test-agent",
            name = "Test Agent",
            url = "https://test.com/agent",
            authMethod = AuthMethod.None()
        )
    }

    @After
    fun tearDown() {
        resetAndroidContext()
        AgentRepository.resetInstance()
    }

    @Test
    fun testConfirmationDetectionOnAndroid() = runTest {
        // Test that user_confirmation tools are detected on Android
        val toolStartEvent = ToolCallStartEvent(
            toolCallId = "confirm-123",
            toolCallName = "user_confirmation"
        )

        viewModel.handleAgentEvent(toolStartEvent)

        // Verify that no ephemeral message is created for confirmation tools
        val state = viewModel.state.value
        val toolMessages = state.messages.filter { it.role == MessageRole.TOOL_CALL }
        assertTrue(toolMessages.isEmpty(), "Confirmation tools should not show ephemeral messages on Android")
    }

    @Test
    fun testConfirmationArgsBuildingOnAndroid() = runTest {
        // Test confirmation args parsing on Android platform
        viewModel.handleAgentEvent(ToolCallStartEvent("confirm-123", "user_confirmation"))

        // Send args in chunks (testing Android's JSON handling)
        val argsChunk1 = """{"action": "Delete Android file", "impact": """
        val argsChunk2 = """"critical", "details": {"path": "/android/data/file.db", """
        val argsChunk3 = """"size": "1MB"}, "timeout_seconds": 60}"""

        viewModel.handleAgentEvent(ToolCallArgsEvent("confirm-123", argsChunk1))
        viewModel.handleAgentEvent(ToolCallArgsEvent("confirm-123", argsChunk2))
        viewModel.handleAgentEvent(ToolCallArgsEvent("confirm-123", argsChunk3))

        // End the tool call to trigger parsing
        viewModel.handleAgentEvent(ToolCallEndEvent("confirm-123"))

        // Verify confirmation dialog works on Android
        val state = viewModel.state.value
        assertNotNull(state.pendingConfirmation)
        
        val confirmation = state.pendingConfirmation!!
        assertEquals("confirm-123", confirmation.toolCallId)
        assertEquals("Delete Android file", confirmation.action)
        assertEquals("critical", confirmation.impact)
        assertEquals(60, confirmation.timeout)
        
        // Verify Android-specific details
        assertEquals("/android/data/file.db", confirmation.details["path"])
        assertEquals("1MB", confirmation.details["size"])
    }

    @Test
    fun testConfirmationFlowOnAndroid() = runTest {
        // Set up confirmation dialog
        setupConfirmationDialog()

        // Verify dialog is present
        assertTrue(viewModel.state.value.pendingConfirmation != null)

        // Test confirmation on Android
        viewModel.confirmAction()

        // Verify dialog is cleared (Android platform behavior)
        val state = viewModel.state.value
        assertNull(state.pendingConfirmation, "Confirmation dialog should be cleared after confirmation on Android")
    }

    @Test
    fun testRejectionFlowOnAndroid() = runTest {
        // Set up confirmation dialog
        setupConfirmationDialog()

        // Verify dialog is present
        assertTrue(viewModel.state.value.pendingConfirmation != null)

        // Test rejection on Android
        viewModel.rejectAction()

        // Verify dialog is cleared (Android platform behavior)
        val state = viewModel.state.value
        assertNull(state.pendingConfirmation, "Confirmation dialog should be cleared after rejection on Android")
    }

    @Test
    fun testInvalidJsonHandlingOnAndroid() = runTest {
        // Test Android's JSON error handling
        viewModel.handleAgentEvent(ToolCallStartEvent("confirm-123", "user_confirmation"))
        
        // Use malformed JSON to test Android's parsing
        val invalidArgs = """{"action": "Test", malformed json on android}"""
        
        viewModel.handleAgentEvent(ToolCallArgsEvent("confirm-123", invalidArgs))
        viewModel.handleAgentEvent(ToolCallEndEvent("confirm-123"))

        // Verify Android handles JSON errors gracefully
        val state = viewModel.state.value
        assertNull(state.pendingConfirmation, "Invalid JSON should not create confirmation dialog on Android")
    }

    @Test
    fun testMultipleConfirmationsOnAndroid() = runTest {
        // Test handling multiple confirmations on Android
        
        // First confirmation
        viewModel.handleAgentEvent(ToolCallStartEvent("confirm-1", "user_confirmation"))
        viewModel.handleAgentEvent(ToolCallArgsEvent("confirm-1", """{"action": "First Android action"}"""))
        viewModel.handleAgentEvent(ToolCallEndEvent("confirm-1"))

        val state1 = viewModel.state.value
        assertNotNull(state1.pendingConfirmation)
        assertEquals("confirm-1", state1.pendingConfirmation!!.toolCallId)

        // Second confirmation (should replace first on Android)
        viewModel.handleAgentEvent(ToolCallStartEvent("confirm-2", "user_confirmation"))
        viewModel.handleAgentEvent(ToolCallArgsEvent("confirm-2", """{"action": "Second Android action"}"""))
        viewModel.handleAgentEvent(ToolCallEndEvent("confirm-2"))

        val state2 = viewModel.state.value
        assertNotNull(state2.pendingConfirmation)
        assertEquals("confirm-2", state2.pendingConfirmation!!.toolCallId)
        assertEquals("Second Android action", state2.pendingConfirmation!!.action)
    }

    @Test
    fun testAndroidSpecificConfirmationBehavior() = runTest {
        // Test any Android-specific confirmation behavior
        setupConfirmationDialog()
        
        val confirmation = viewModel.state.value.pendingConfirmation!!
        
        // Verify confirmation exists and has expected properties for Android
        assertEquals("Test Android action", confirmation.action)
        assertEquals("medium", confirmation.impact)
        assertEquals(30, confirmation.timeout)
        
        // Test that state persists correctly on Android
        val initialConfirmation = confirmation.copy()
        
        // Trigger some other events
        viewModel.handleAgentEvent(TextMessageStartEvent("msg-1", "assistant"))
        viewModel.handleAgentEvent(StepStartedEvent("android step"))
        
        // Verify confirmation state is unchanged
        val state = viewModel.state.value
        assertNotNull(state.pendingConfirmation)
        assertEquals(initialConfirmation.toolCallId, state.pendingConfirmation!!.toolCallId)
        assertEquals(initialConfirmation.action, state.pendingConfirmation!!.action)
    }

    /**
     * Helper method to set up a basic confirmation dialog for Android testing.
     */
    private fun setupConfirmationDialog() {
        viewModel.handleAgentEvent(ToolCallStartEvent("confirm-test", "user_confirmation"))
        
        val confirmationArgs = """
            {
                "action": "Test Android action",
                "impact": "medium",
                "details": {"platform": "android"},
                "timeout_seconds": 30
            }
        """.trimIndent()
        
        viewModel.handleAgentEvent(ToolCallArgsEvent("confirm-test", confirmationArgs))
        viewModel.handleAgentEvent(ToolCallEndEvent("confirm-test"))
    }
}