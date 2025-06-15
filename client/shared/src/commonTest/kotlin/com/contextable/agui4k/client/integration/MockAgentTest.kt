package com.contextable.agui4k.client.integration

import com.contextable.agui4k.client.HttpAgent
import com.contextable.agui4k.client.HttpAgentConfig
import com.contextable.agui4k.core.types.*
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import io.ktor.utils.io.*
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.*

class MockAgentTest {
    
    @Test
    @Ignore("We can't mock the engine")
    fun testAgentConnection() = runTest {
        val mockEngine = MockEngine { request ->
            assertEquals("https://test.com/agent", request.url.toString())
            assertEquals(HttpMethod.Post, request.method)
            
            // Simulate SSE response
            respond(
                content = ByteReadChannel("""
                    data: {"type":"RUN_STARTED","threadId":"thread-1","runId":"run-1"}
                    
                    data: {"type":"TEXT_MESSAGE_START","messageId":"msg-1","role":"assistant"}
                    
                    data: {"type":"TEXT_MESSAGE_CONTENT","messageId":"msg-1","delta":"Hello!"}
                    
                    data: {"type":"TEXT_MESSAGE_END","messageId":"msg-1"}
                    
                    data: {"type":"RUN_FINISHED","threadId":"thread-1","runId":"run-1"}
                """.trimIndent()),
                status = HttpStatusCode.OK,
                headers = headersOf(
                    HttpHeaders.ContentType, ContentType.Text.EventStream.toString()
                )
            )
        }
        
        // Create agent with mock engine
        val agent = HttpAgent(
            HttpAgentConfig(
                url = "https://test.com/agent",
                headers = mapOf("Authorization" to "Bearer test-token")
            ),
            //engine = mockEngine
        )
        
        // Add a user message
        agent.addMessage(
            UserMessage(
                id = "user-1",
                content = "Hello"
            )
        )
        
        // Run agent and collect events
        val events = agent.runAgent().toList()
        
        // Verify events
        assertTrue(events.any { it is RunStartedEvent })
        assertTrue(events.any { it is TextMessageStartEvent })
        assertTrue(events.any { it is TextMessageContentEvent })
        assertTrue(events.any { it is TextMessageEndEvent })
        assertTrue(events.any { it is RunFinishedEvent })
        
        // Verify message content
        val contentEvent = events.filterIsInstance<TextMessageContentEvent>().first()
        assertEquals("Hello!", contentEvent.delta)
    }
}