package com.contextable.agui4k.tests

import com.contextable.agui4k.core.agent.AbstractAgent
import com.contextable.agui4k.core.agent.AgentConfig
import com.contextable.agui4k.core.types.*
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import io.ktor.utils.io.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalSerializationApi::class)
class AgentTest {
    
    /**
     * Test creating a client with configuration.
     */
    @Test
    fun testClientCreation() {
        val config = AgentConfig(
            agentId = "test-client",
            description = "Test client",
            threadId = "test-thread"
        )
        
        val client = TestClient(config)
        
        assertEquals("test-client", client.agentId)
        assertEquals("Test client", client.description)
        assertEquals("test-thread", client.threadId)
    }
    
    /**
     * Test adding messages to a client.
     */
    @Test
    fun testAddMessage() = runTest {
        val client = TestClient()
        
        val message = UserMessage(
            id = "msg_1",
            content = "Hello, agent!"
        )
        
        client.addMessage(message)
        
        val messages = client.messages.value
        assertEquals(1, messages.size)
        assertEquals(message, messages.first())
    }
    
    /**
     * Test connecting to an agent and receiving events.
     */
    @Test
    fun testRunAgent() = runTest {
        val client = TestClient()
        
        val events = client.runAgent().toList()
        
        assertTrue(events.isNotEmpty())
        assertTrue(events.first() is RunStartedEvent)
        assertTrue(events.last() is RunFinishedEvent)
        
        // Check for text message events
        val textEvents = events.filterIsInstance<TextMessageContentEvent>()
        assertTrue(textEvents.isNotEmpty())
    }
    
    /**
     * Test client with tools.
     */
    @Test
    fun testClientWithTools() = runTest {
        val client = TestClient()
        
        val tool = Tool(
            name = "testTool",
            description = "A test tool",
            parameters = Json.parseToJsonElement("""
                {
                    "type": "object",
                    "properties": {
                        "param": {
                            "type": "string"
                        }
                    }
                }
            """)
        )
        
        val parameters = RunAgentParameters(
            tools = listOf(tool)
        )
        
        val events = client.runAgent(parameters).toList()
        
        // Verify tool was passed to the agent
        val toolCallEvents = events.filterIsInstance<ToolCallStartEvent>()
        if (toolCallEvents.isNotEmpty()) {
            assertEquals("testTool", toolCallEvents.first().toolCallName)
        }
    }
    
    /**
     * Test client cloning.
     */
    @Test
    fun testClientClone() {
        val originalClient = TestClient(
            AgentConfig(
                agentId = "original",
                description = "Original client",
                threadId = "thread-1"
            )
        )
        
        originalClient.addMessage(
            UserMessage(
                id = "msg_1",
                content = "Test message"
            )
        )
        
        val clonedClient = originalClient.clone() as TestClient
        
        // Verify the clone has different ID but same data
        assertNotNull(clonedClient.agentId)
        assertTrue(clonedClient.agentId != originalClient.agentId)
        assertEquals(originalClient.description, clonedClient.description)
        assertEquals(originalClient.threadId, clonedClient.threadId)
        assertEquals(originalClient.messages.value, clonedClient.messages.value)
    }
}

/**
 * Simple test client implementation for unit testing.
 */
@OptIn(ExperimentalCoroutinesApi::class, ExperimentalSerializationApi::class)
class TestClient(config: AgentConfig = AgentConfig()) : AbstractAgent(config) {

    override fun clone(): TestClient {
        return TestClient(
            AgentConfig(
                agentId = generateAgentId(),
                description = description,
                threadId = threadId,
                initialMessages = messages.value.toList(),
                initialState = state.value
            )
        )
    }

    override suspend fun run(input: RunAgentInput): Flow<BaseEvent> = flow {
        // Emit run started
        emit(RunStartedEvent(
            threadId = input.threadId,
            runId = input.runId
        ))
        
        // Simulate some agent response
        val messageId = "test_msg_${System.currentTimeMillis()}"
        
        emit(TextMessageStartEvent(
            messageId = messageId,
            role = "assistant"
        ))
        
        emit(TextMessageContentEvent(
            messageId = messageId,
            delta = "This is a test response from the agent"
        ))
        
        emit(TextMessageEndEvent(
            messageId = messageId
        ))
        
        // If tools are provided, simulate tool usage
        if (input.tools.isNotEmpty()) {
            val tool = input.tools.first()
            val toolCallId = "tool_${System.currentTimeMillis()}"
            
            emit(ToolCallStartEvent(
                toolCallId = toolCallId,
                toolCallName = tool.name
            ))
            
            emit(ToolCallArgsEvent(
                toolCallId = toolCallId,
                delta = """{"param": "test"}"""
            ))
            
            emit(ToolCallEndEvent(
                toolCallId = toolCallId
            ))
        }
        
        // Emit run finished
        emit(RunFinishedEvent(
            threadId = input.threadId,
            runId = input.runId
        ))
    }
}
