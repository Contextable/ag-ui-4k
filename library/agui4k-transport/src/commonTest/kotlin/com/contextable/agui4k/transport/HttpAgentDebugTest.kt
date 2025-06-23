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
 * Minimal tests to debug specific HttpAgent issues
 */
class HttpAgentDebugTest {
    
    @Test
    fun testBasicRunTest() = runTest {
        // Just test that runTest works
        assertTrue(true)
    }
    
    @Test 
    fun testMockEngineCreation() {
        val mockEngine = MockEngine { request ->
            respondOk("test response")
        }
        assertNotNull(mockEngine)
    }
    
    @Test
    fun testMockEngineWithHttpClient() = runTest {
        val mockEngine = MockEngine { request ->
            respondOk("test response")
        }
        
        val httpClient = HttpClient(mockEngine) {
            install(SSE)
        }
        
        assertNotNull(httpClient)
        httpClient.close()
    }
    
    @Test
    fun testSimpleRunAgentInput() = runTest {
        val input = RunAgentInput(
            threadId = "test-thread",
            runId = "test-run",
            messages = listOf(
                UserMessage(id = "msg1", content = "Hello")
            )
        )
        
        assertNotNull(input)
        assertEquals("test-thread", input.threadId)
        assertEquals("test-run", input.runId)
    }
    
    @Test
    fun testHttpAgentWithMockEngine() = runTest {
        val mockEngine = MockEngine { request ->
            respondOk("event: test\ndata: {\"test\": \"data\"}\n\n")
        }
        
        val httpClient = HttpClient(mockEngine) {
            install(SSE)
        }
        
        val config = HttpAgent.HttpAgentConfig(
            url = "https://api.example.com/agent",
            httpClient = httpClient
        )
        
        val agent = HttpAgent(config)
        
        val input = RunAgentInput(
            threadId = "test-thread",
            runId = "test-run",
            messages = listOf(UserMessage(id = "msg1", content = "Test"))
        )
        
        // This might fail, but let's see where
        try {
            val events = agent.runAgent(input)
            assertNotNull(events)
        } catch (e: Exception) {
            // Expected for now, let's see what the exception is
            println("Exception: ${e.javaClass.simpleName}: ${e.message}")
        }
        
        agent.close()
    }
}