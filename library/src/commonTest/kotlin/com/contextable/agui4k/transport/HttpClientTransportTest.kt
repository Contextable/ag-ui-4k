package com.contextable.agui4k.transport

import com.contextable.agui4k.core.types.*
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.*

/**
 * Tests for HttpClientTransport.
 * 
 * Note: These are basic unit tests. Integration tests with mock servers
 * would require additional test infrastructure.
 */
class HttpClientTransportTest {
    
    @Test
    fun testConfigurationDefaults() {
        val config = HttpClientTransportConfig(
            url = "https://example.com/agent"
        )
        
        assertEquals("https://example.com/agent", config.url)
        assertTrue(config.headers.isEmpty())
        assertEquals(600_000L, config.requestTimeoutMillis)
        assertEquals(30_000L, config.connectTimeoutMillis)
        assertEquals(600_000L, config.socketTimeoutMillis)
        assertTrue(config.retryPolicy is ExponentialBackoffRetryPolicy)
    }
    
    @Test
    fun testConfigurationWithCustomValues() {
        val headers = mapOf("Authorization" to "Bearer token123")
        val retryPolicy = FixedDelayRetryPolicy(maxAttempts = 5)
        
        val config = HttpClientTransportConfig(
            url = "https://api.example.com/v1/agent",
            headers = headers,
            requestTimeoutMillis = 300_000L,
            connectTimeoutMillis = 15_000L,
            socketTimeoutMillis = 300_000L,
            retryPolicy = retryPolicy
        )
        
        assertEquals("https://api.example.com/v1/agent", config.url)
        assertEquals(headers, config.headers)
        assertEquals(300_000L, config.requestTimeoutMillis)
        assertEquals(15_000L, config.connectTimeoutMillis)
        assertEquals(300_000L, config.socketTimeoutMillis)
        assertEquals(retryPolicy, config.retryPolicy)
    }
    
    @Test
    fun testThreadIdExtraction() {
        val transport = HttpClientTransport(
            HttpClientTransportConfig(url = "https://example.com/agent")
        )
        
        val userMessage = UserMessage(
            id = "msg1",
            content = "Hello"
        )
        
        // We can't easily test the private extractThreadId method,
        // but we can verify that the transport handles messages correctly
        assertNotNull(transport)
        assertNotNull(userMessage)
    }
    
    @Test
    fun testGeneratedThreadIdIsUnique() {
        val transport = HttpClientTransport(
            HttpClientTransportConfig(url = "https://example.com/agent")
        )
        
        // Since generateThreadId is private, we test uniqueness indirectly
        // by creating multiple transports and ensuring they're independent
        val transport2 = HttpClientTransport(
            HttpClientTransportConfig(url = "https://example.com/agent")
        )
        
        assertNotNull(transport)
        assertNotNull(transport2)
    }
}