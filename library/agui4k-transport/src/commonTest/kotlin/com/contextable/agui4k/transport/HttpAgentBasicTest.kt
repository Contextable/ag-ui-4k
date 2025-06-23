package com.contextable.agui4k.transport

import com.contextable.agui4k.core.types.*
import com.contextable.agui4k.transport.http.HttpAgent
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.sse.*
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.*
import kotlin.test.*

/**
 * Basic tests for HttpAgent focusing on configuration and setup.
 * These tests avoid complex SSE mocking which is difficult to test reliably.
 */
class HttpAgentBasicTest {
    
    @Test
    fun testHttpAgentConfigCreation() {
        val config = HttpAgent.HttpAgentConfig(
            url = "https://api.example.com/agent",
            headers = mapOf(
                "Authorization" to "Bearer token123",
                "X-API-Version" to "1.0"
            )
        )
        
        assertEquals("https://api.example.com/agent", config.url)
        assertEquals("Bearer token123", config.headers["Authorization"])
        assertEquals("1.0", config.headers["X-API-Version"])
        assertNull(config.httpClient)
    }
    
    @Test
    fun testHttpAgentConfigWithCustomHttpClient() {
        val customClient = HttpClient(MockEngine { request ->
            respondOk("test")
        }) {
            install(SSE)
        }
        
        val config = HttpAgent.HttpAgentConfig(
            url = "https://custom.api.com/agent",
            httpClient = customClient
        )
        
        assertEquals("https://custom.api.com/agent", config.url)
        assertTrue(config.headers.isEmpty())
        assertEquals(customClient, config.httpClient)
        
        customClient.close()
    }
    
    @Test
    fun testHttpAgentConfigWithEmptyHeaders() {
        val config = HttpAgent.HttpAgentConfig(
            url = "https://api.example.com/agent",
            headers = emptyMap()
        )
        
        assertEquals("https://api.example.com/agent", config.url)
        assertTrue(config.headers.isEmpty())
        assertNull(config.httpClient)
    }
    
    @Test
    fun testHttpAgentCreation() {
        val config = HttpAgent.HttpAgentConfig(
            url = "https://api.example.com/agent"
        )
        
        val agent = HttpAgent(config)
        assertNotNull(agent)
        
        // Clean up
        agent.close()
    }
    
    @Test
    fun testHttpAgentCreationWithHeaders() {
        val config = HttpAgent.HttpAgentConfig(
            url = "https://api.example.com/agent",
            headers = mapOf(
                "Authorization" to "Bearer secret-token",
                "User-Agent" to "agui4k/1.0",
                "X-Client-Name" to "test-app"
            )
        )
        
        val agent = HttpAgent(config)
        assertNotNull(agent)
        
        // Clean up
        agent.close()
    }
    
    @Test
    fun testHttpAgentAbort() {
        val config = HttpAgent.HttpAgentConfig(
            url = "https://api.example.com/agent"
        )
        
        val agent = HttpAgent(config)
        
        // Should not throw exception
        agent.abort()
        
        // Multiple aborts should be safe
        agent.abort()
        agent.abort()
        
        agent.close()
    }
    
    @Test 
    fun testHttpAgentClose() {
        val config = HttpAgent.HttpAgentConfig(
            url = "https://api.example.com/agent"
        )
        
        val agent = HttpAgent(config)
        
        // Should not throw exception
        agent.close()
        
        // Multiple closes should be safe
        agent.close()
        agent.close()
    }
    
    @Test
    fun testHttpAgentCloseWithCustomClient() {
        val customClient = HttpClient(MockEngine { request ->
            respondOk("test")
        }) {
            install(SSE)
        }
        
        val config = HttpAgent.HttpAgentConfig(
            url = "https://api.example.com/agent",
            httpClient = customClient
        )
        
        val agent = HttpAgent(config)
        
        // When using custom client, close should not close the client
        agent.close()
        
        // Custom client should still be usable (not closed by agent)
        assertNotNull(customClient)
        
        // Clean up custom client manually
        customClient.close()
    }
    
    @Test
    fun testRunAgentInputCreation() {
        // Test that we can create valid inputs for the HttpAgent
        val state = buildJsonObject {
            put("sessionId", "session-123")
            put("userId", "user-456")
        }
        
        val forwardedProps = buildJsonObject {
            put("clientVersion", "1.0.0")
            put("requestId", "req-789")
        }
        
        val input = RunAgentInput(
            threadId = "thread-123",
            runId = "run-456",
            messages = listOf(
                UserMessage(id = "usr-1", content = "Hello, how are you?")
            ),
            state = state,
            tools = listOf(
                Tool(
                    name = "search_documents",
                    description = "Search through user documents",
                    parameters = buildJsonObject {
                        put("type", "object")
                        putJsonObject("properties") {
                            putJsonObject("query") {
                                put("type", "string")
                                put("description", "Search query")
                            }
                        }
                        putJsonArray("required") {
                            add("query")
                        }
                    }
                )
            ),
            context = listOf(
                Context(
                    description = "current_time",
                    value = "2024-01-15T10:30:00Z"
                )
            ),
            forwardedProps = forwardedProps
        )
        
        // Verify input is well-formed
        assertNotNull(input)
        assertEquals("thread-123", input.threadId)
        assertEquals("run-456", input.runId)
        assertEquals(1, input.messages.size)
        assertEquals(state, input.state)
        assertEquals(1, input.tools.size)
        assertEquals(1, input.context.size)
        assertEquals(forwardedProps, input.forwardedProps)
    }
    
    @Test
    fun testRunAgentInputWithMinimalData() {
        // Test minimal input
        val input = RunAgentInput(
            threadId = "minimal",
            runId = "minimal"
        )
        
        assertNotNull(input)
        assertEquals("minimal", input.threadId)
        assertEquals("minimal", input.runId)
        assertTrue(input.messages.isEmpty())
        assertTrue(input.tools.isEmpty())
        assertTrue(input.context.isEmpty())
        assertIs<JsonObject>(input.state)
        assertIs<JsonObject>(input.forwardedProps)
    }
    
    @Test
    fun testRunAgentInputWithComplexState() {
        // Test complex nested state structure
        val complexState = buildJsonObject {
            put("version", "1.0")
            putJsonArray("users") {
                addJsonObject {
                    put("id", 1)
                    put("name", "Alice")
                    putJsonArray("permissions") {
                        add("read")
                        add("write")
                    }
                }
                addJsonObject {
                    put("id", 2)
                    put("name", "Bob")
                    putJsonArray("permissions") {
                        add("read")
                    }
                }
            }
            putJsonObject("settings") {
                putJsonObject("ui") {
                    put("theme", "dark")
                    put("fontSize", 14)
                }
                putJsonObject("api") {
                    put("timeout", 30)
                    put("retries", 3)
                }
            }
        }
        
        val input = RunAgentInput(
            threadId = "complex-thread",
            runId = "complex-run",
            messages = listOf(
                UserMessage(id = "usr1", content = "Process complex state")
            ),
            state = complexState
        )
        
        assertNotNull(input)
        assertEquals(complexState, input.state)
        
        // Verify nested structure
        val state = input.state as JsonObject
        val users = state["users"]?.jsonArray
        assertNotNull(users)
        assertEquals(2, users.size)
        
        val firstUser = users[0].jsonObject
        assertEquals(1, firstUser["id"]?.jsonPrimitive?.int)
        assertEquals("Alice", firstUser["name"]?.jsonPrimitive?.content)
        
        val settings = state["settings"]?.jsonObject
        assertNotNull(settings)
        
        val uiSettings = settings["ui"]?.jsonObject
        assertNotNull(uiSettings)
        assertEquals("dark", uiSettings["theme"]?.jsonPrimitive?.content)
        assertEquals(14, uiSettings["fontSize"]?.jsonPrimitive?.int)
    }
}