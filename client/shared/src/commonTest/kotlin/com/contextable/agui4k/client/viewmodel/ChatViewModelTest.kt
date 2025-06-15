package com.contextable.agui4k.client.viewmodel

import com.contextable.agui4k.client.ui.screens.chat.MessageRole
import com.contextable.agui4k.client.data.model.AgentConfig
import com.contextable.agui4k.client.data.model.AuthMethod
import com.contextable.agui4k.client.data.repository.AgentRepository
import com.contextable.agui4k.client.test.TestSettings
import com.contextable.agui4k.client.test.TestChatViewModel
import com.contextable.agui4k.core.types.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class ChatViewModelTest {

    private lateinit var testSettings: TestSettings
    private lateinit var agentRepository: AgentRepository
    private lateinit var viewModel: TestChatViewModel

    @BeforeTest
    fun setup() {
        testSettings = TestSettings()
        agentRepository = AgentRepository(testSettings)
        viewModel = TestChatViewModel()
    }

    @Test
    fun testInitialState() = runTest {
        // Create a test view model with a mock agent
        val testAgent = AgentConfig(
            id = "test-1",
            name = "Test Agent",
            url = "https://test.com/agent",
            authMethod = AuthMethod.None()
        )

        // Add agent to repository
        agentRepository.addAgent(testAgent)
        agentRepository.setActiveAgent(testAgent)

        // Wait for state updates
        delay(100)

        // Verify active agent is set
        val activeAgent = agentRepository.activeAgent.value
        assertNotNull(activeAgent)
        assertEquals("Test Agent", activeAgent.name)
    }

    @Test
    fun testAgentRepository() = runTest {
        val agent = AgentConfig(
            id = "test-1",
            name = "Test Agent",
            url = "https://test.com/agent",
            authMethod = AuthMethod.None()
        )

        // Test adding agent
        agentRepository.addAgent(agent)
        val agents = agentRepository.agents.value
        assertEquals(1, agents.size)
        assertEquals(agent, agents.first())

        // Test setting active agent
        agentRepository.setActiveAgent(agent)
        val activeAgent = agentRepository.activeAgent.value
        assertEquals(agent.id, activeAgent?.id)

        // Test session creation
        val session = agentRepository.currentSession.value
        assertNotNull(session)
        assertEquals(agent.id, session.agentId)
    }

    @Test
    fun testMessageFormatting() {
        val userMessage = com.contextable.agui4k.client.ui.screens.chat.DisplayMessage(
            id = "1",
            role = MessageRole.USER,
            content = "Hello, agent!"
        )

        assertEquals("1", userMessage.id)
        assertEquals(MessageRole.USER, userMessage.role)
        assertEquals("Hello, agent!", userMessage.content)
        assertFalse(userMessage.isStreaming)
    }

    @Test
    fun testStreamingMessage() {
        val streamingMessage = com.contextable.agui4k.client.ui.screens.chat.DisplayMessage(
            id = "2",
            role = MessageRole.ASSISTANT,
            content = "Thinking...",
            isStreaming = true
        )

        assertEquals("2", streamingMessage.id)
        assertEquals(MessageRole.ASSISTANT, streamingMessage.role)
        assertEquals("Thinking...", streamingMessage.content)
        assertTrue(streamingMessage.isStreaming)
    }
}