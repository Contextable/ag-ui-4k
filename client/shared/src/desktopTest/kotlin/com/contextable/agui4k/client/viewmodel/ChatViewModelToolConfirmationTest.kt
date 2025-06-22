package com.contextable.agui4k.sample.client.viewmodel

import com.contextable.agui4k.sample.client.ui.screens.chat.*
import com.contextable.agui4k.sample.client.data.model.AgentConfig
import com.contextable.agui4k.sample.client.data.model.AuthMethod
import com.contextable.agui4k.sample.client.data.repository.AgentRepository
import com.contextable.agui4k.sample.client.test.TestSettings
import com.contextable.agui4k.core.types.*
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlin.test.*

/**
 * Comprehensive tests for tool confirmation flow in ChatViewModel.
 * Tests the complete workflow from tool call detection through user response.
 */
class ChatViewModelToolConfirmationTest {

    private lateinit var testSettings: TestSettings
    private lateinit var agentRepository: AgentRepository
    private lateinit var viewModel: ChatViewModel

    @BeforeTest
    fun setup() {
        // Reset singleton instances
        AgentRepository.resetInstance()

        testSettings = TestSettings()
        agentRepository = AgentRepository.getInstance(testSettings)
        viewModel = ChatViewModel()

        // Set up a test agent
        val testAgent = AgentConfig(
            id = "test-agent",
            name = "Test Agent",
            url = "https://test.com/agent",
            authMethod = AuthMethod.None()
        )
    }

    @AfterTest
    fun tearDown() {
        AgentRepository.resetInstance()
    }

    @Test
    fun testUserConfirmationToolDetection() = runTest {
        // Start a user_confirmation tool call
        val toolStartEvent = ToolCallStartEvent(
            toolCallId = "confirm-123",
            toolCallName = "user_confirmation"
        )

        viewModel.handleAgentEvent(toolStartEvent)

        // Verify that no ephemeral message is created for confirmation tools
        val state = viewModel.state.value
        val toolMessages = state.messages.filter { it.role == MessageRole.TOOL_CALL }
        assertTrue(toolMessages.isEmpty(), "Confirmation tools should not show ephemeral messages")
    }

    @Test
    fun testConfirmationArgsBuilding() = runTest {
        // Start confirmation tool
        viewModel.handleAgentEvent(ToolCallStartEvent("confirm-123", "user_confirmation"))

        // Send args in multiple chunks to test accumulation
        val argsChunk1 = """{"action": "Delete file", "impact": """
        val argsChunk2 = """"high", "details": {"file": "important.txt", """
        val argsChunk3 = """"reason": "cleanup"}, "timeout_seconds": 30}"""

        viewModel.handleAgentEvent(ToolCallArgsEvent("confirm-123", argsChunk1))
        viewModel.handleAgentEvent(ToolCallArgsEvent("confirm-123", argsChunk2))
        viewModel.handleAgentEvent(ToolCallArgsEvent("confirm-123", argsChunk3))

        // End the tool call to trigger parsing
        viewModel.handleAgentEvent(ToolCallEndEvent("confirm-123"))

        // Verify confirmation dialog is shown
        val state = viewModel.state.value
        assertNotNull(state.pendingConfirmation)
        
        val confirmation = state.pendingConfirmation!!
        assertEquals("confirm-123", confirmation.toolCallId)
        assertEquals("Delete file", confirmation.action)
        assertEquals("high", confirmation.impact)
        assertEquals(30, confirmation.timeout)
        
        // Verify details are parsed correctly
        assertEquals("important.txt", confirmation.details["file"])
        assertEquals("cleanup", confirmation.details["reason"])
    }

    @Test
    fun testConfirmationWithMinimalArgs() = runTest {
        // Test with only required fields
        viewModel.handleAgentEvent(ToolCallStartEvent("confirm-123", "user_confirmation"))
        
        val minimalArgs = """{"action": "Simple action"}"""
        
        viewModel.handleAgentEvent(ToolCallArgsEvent("confirm-123", minimalArgs))
        viewModel.handleAgentEvent(ToolCallEndEvent("confirm-123"))

        // Verify confirmation dialog with defaults
        val state = viewModel.state.value
        assertNotNull(state.pendingConfirmation)
        
        val confirmation = state.pendingConfirmation!!
        assertEquals("Simple action", confirmation.action)
        assertEquals("medium", confirmation.impact) // Should default to medium
        assertEquals(30, confirmation.timeout) // Should default to 30
        assertTrue(confirmation.details.isEmpty())
    }

    @Test
    fun testConfirmationWithInvalidJson() = runTest {
        // Test error handling for malformed JSON
        viewModel.handleAgentEvent(ToolCallStartEvent("confirm-123", "user_confirmation"))
        
        // Use truly malformed JSON
        val invalidArgs = """{"action": "Test", "invalid": json, missing quotes}"""
        
        viewModel.handleAgentEvent(ToolCallArgsEvent("confirm-123", invalidArgs))
        viewModel.handleAgentEvent(ToolCallEndEvent("confirm-123"))

        // Verify no confirmation dialog is shown (the exception should be caught)
        val state = viewModel.state.value
        assertNull(state.pendingConfirmation, "Invalid JSON should not create confirmation dialog")
    }

    @Test
    fun testConfirmActionFlow() = runTest {
        // Set up confirmation dialog
        setupConfirmationDialog()

        // Verify dialog is present
        assertTrue(viewModel.state.value.pendingConfirmation != null)

        // Confirm the action
        viewModel.confirmAction()

        // Verify dialog is cleared
        val state = viewModel.state.value
        assertNull(state.pendingConfirmation, "Confirmation dialog should be cleared after confirmation")

        // Note: Testing the actual tool response message would require mocking
        // the StatefulClient, which is beyond the scope of this unit test
    }

    @Test
    fun testRejectActionFlow() = runTest {
        // Set up confirmation dialog
        setupConfirmationDialog()

        // Verify dialog is present
        assertTrue(viewModel.state.value.pendingConfirmation != null)

        // Reject the action
        viewModel.rejectAction()

        // Verify dialog is cleared
        val state = viewModel.state.value
        assertNull(state.pendingConfirmation, "Confirmation dialog should be cleared after rejection")
    }

    @Test
    fun testMultipleConfirmationToolCalls() = runTest {
        // Start first confirmation
        viewModel.handleAgentEvent(ToolCallStartEvent("confirm-1", "user_confirmation"))
        viewModel.handleAgentEvent(ToolCallArgsEvent("confirm-1", """{"action": "First action"}"""))
        viewModel.handleAgentEvent(ToolCallEndEvent("confirm-1"))

        // Verify first confirmation is shown
        val state1 = viewModel.state.value
        assertNotNull(state1.pendingConfirmation)
        assertEquals("confirm-1", state1.pendingConfirmation!!.toolCallId)

        // Start second confirmation (should replace first)
        viewModel.handleAgentEvent(ToolCallStartEvent("confirm-2", "user_confirmation"))
        viewModel.handleAgentEvent(ToolCallArgsEvent("confirm-2", """{"action": "Second action"}"""))
        viewModel.handleAgentEvent(ToolCallEndEvent("confirm-2"))

        // Verify second confirmation replaces first
        val state2 = viewModel.state.value
        assertNotNull(state2.pendingConfirmation)
        assertEquals("confirm-2", state2.pendingConfirmation!!.toolCallId)
        assertEquals("Second action", state2.pendingConfirmation!!.action)
    }

    @Test
    fun testConfirmationTimeoutHandling() = runTest {
        // Test with custom timeout
        viewModel.handleAgentEvent(ToolCallStartEvent("confirm-123", "user_confirmation"))
        
        val argsWithTimeout = """{"action": "Quick action", "timeout_seconds": 5}"""
        
        viewModel.handleAgentEvent(ToolCallArgsEvent("confirm-123", argsWithTimeout))
        viewModel.handleAgentEvent(ToolCallEndEvent("confirm-123"))

        // Verify custom timeout is set
        val state = viewModel.state.value
        assertNotNull(state.pendingConfirmation)
        assertEquals(5, state.pendingConfirmation!!.timeout)
    }

    @Test
    fun testConfirmationImpactLevels() = runTest {
        val impactLevels = listOf("low", "medium", "high", "critical")
        
        for ((index, impact) in impactLevels.withIndex()) {
            val toolCallId = "confirm-$index"
            
            viewModel.handleAgentEvent(ToolCallStartEvent(toolCallId, "user_confirmation"))
            
            val args = """{"action": "Test action", "impact": "$impact"}"""
            
            viewModel.handleAgentEvent(ToolCallArgsEvent(toolCallId, args))
            viewModel.handleAgentEvent(ToolCallEndEvent(toolCallId))

            // Verify impact level is correctly parsed
            val state = viewModel.state.value
            assertNotNull(state.pendingConfirmation)
            assertEquals(impact, state.pendingConfirmation!!.impact)
            
            // Clear for next test
            viewModel.confirmAction()
        }
    }

    @Test
    fun testConfirmationDetailsHandling() = runTest {
        // Test with complex details object
        viewModel.handleAgentEvent(ToolCallStartEvent("confirm-123", "user_confirmation"))
        
        val argsWithDetails = """
            {
                "action": "Database operation",
                "impact": "high",
                "details": {
                    "database": "production",
                    "table": "users",
                    "operation": "truncate",
                    "affected_rows": "1000"
                }
            }
        """.trimIndent()
        
        viewModel.handleAgentEvent(ToolCallArgsEvent("confirm-123", argsWithDetails))
        viewModel.handleAgentEvent(ToolCallEndEvent("confirm-123"))

        // Verify details are correctly parsed
        val state = viewModel.state.value
        assertNotNull(state.pendingConfirmation)
        
        val details = state.pendingConfirmation!!.details
        assertEquals("production", details["database"])
        assertEquals("users", details["table"])
        assertEquals("truncate", details["operation"])
        assertEquals("1000", details["affected_rows"])
    }

    @Test
    fun testNonConfirmationToolsIgnored() = runTest {
        // Test that regular tools don't trigger confirmation dialog
        viewModel.handleAgentEvent(ToolCallStartEvent("tool-123", "file_read"))
        
        val regularArgs = """{"path": "/some/file.txt"}"""
        
        viewModel.handleAgentEvent(ToolCallArgsEvent("tool-123", regularArgs))
        viewModel.handleAgentEvent(ToolCallEndEvent("tool-123"))

        // Verify no confirmation dialog is shown
        val state = viewModel.state.value
        assertNull(state.pendingConfirmation, "Regular tools should not show confirmation dialog")
    }

    @Test
    fun testConfirmationStateConsistency() = runTest {
        // Test that confirmation state is consistent across multiple operations
        
        // Set up initial confirmation
        setupConfirmationDialog()
        val initialConfirmation = viewModel.state.value.pendingConfirmation!!
        
        // Simulate some other events that shouldn't affect confirmation state
        viewModel.handleAgentEvent(TextMessageStartEvent("msg-1", "assistant"))
        viewModel.handleAgentEvent(TextMessageContentEvent("msg-1", "Some text"))
        viewModel.handleAgentEvent(StepStartedEvent("processing"))
        
        // Verify confirmation is still there and unchanged
        val state = viewModel.state.value
        assertNotNull(state.pendingConfirmation)
        assertEquals(initialConfirmation.toolCallId, state.pendingConfirmation!!.toolCallId)
        assertEquals(initialConfirmation.action, state.pendingConfirmation!!.action)
        assertEquals(initialConfirmation.impact, state.pendingConfirmation!!.impact)
    }

    @Test
    fun testConfirmationDialogEdgeCases() = runTest {
        // Test with empty action (should still work)
        viewModel.handleAgentEvent(ToolCallStartEvent("confirm-1", "user_confirmation"))
        viewModel.handleAgentEvent(ToolCallArgsEvent("confirm-1", """{"action": ""}"""))
        viewModel.handleAgentEvent(ToolCallEndEvent("confirm-1"))

        val state1 = viewModel.state.value
        assertNotNull(state1.pendingConfirmation)
        assertEquals("", state1.pendingConfirmation!!.action)

        // Clear and test with missing action
        viewModel.confirmAction()
        
        viewModel.handleAgentEvent(ToolCallStartEvent("confirm-2", "user_confirmation"))
        viewModel.handleAgentEvent(ToolCallArgsEvent("confirm-2", """{"impact": "high"}"""))
        viewModel.handleAgentEvent(ToolCallEndEvent("confirm-2"))

        val state2 = viewModel.state.value
        assertNotNull(state2.pendingConfirmation)
        assertEquals("Unknown action", state2.pendingConfirmation!!.action)
    }

    /**
     * Helper method to set up a basic confirmation dialog for testing.
     */
    private fun setupConfirmationDialog() {
        viewModel.handleAgentEvent(ToolCallStartEvent("confirm-test", "user_confirmation"))
        
        val confirmationArgs = """
            {
                "action": "Test action",
                "impact": "medium",
                "details": {"test": "value"},
                "timeout_seconds": 30
            }
        """.trimIndent()
        
        viewModel.handleAgentEvent(ToolCallArgsEvent("confirm-test", confirmationArgs))
        viewModel.handleAgentEvent(ToolCallEndEvent("confirm-test"))
    }
}