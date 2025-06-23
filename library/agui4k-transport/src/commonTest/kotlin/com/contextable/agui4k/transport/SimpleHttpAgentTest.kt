package com.contextable.agui4k.transport

import com.contextable.agui4k.transport.http.HttpAgent
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.sse.*
import kotlin.test.Test
import kotlin.test.assertNotNull

/**
 * Simple test to debug HttpAgent issues
 */
class SimpleHttpAgentTest {
    
    @Test
    fun testCreateMockEngine() {
        val mockEngine = MockEngine { request ->
            respondOk("test")
        }
        assertNotNull(mockEngine)
    }
    
    @Test
    fun testCreateHttpClientWithMockEngine() {
        val mockEngine = MockEngine { request ->
            respondOk("test")
        }
        
        val httpClient = HttpClient(mockEngine) {
            install(SSE)
        }
        assertNotNull(httpClient)
        httpClient.close()
    }
    
    @Test
    fun testHttpAgentConfigCreation() {
        val config = HttpAgent.HttpAgentConfig(
            url = "https://api.example.com/agent"
        )
        assertNotNull(config)
    }
    
    @Test
    fun testHttpAgentCreationWithoutCustomClient() {
        val config = HttpAgent.HttpAgentConfig(
            url = "https://api.example.com/agent"
        )
        
        val agent = HttpAgent(config)
        assertNotNull(agent)
        agent.close()
    }
}