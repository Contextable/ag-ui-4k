// agui4k-agent-sdk/src/commonTest/kotlin/com/contextable/agui4k/sdk/AgUi4KAgentConfigTest.kt
package com.contextable.agui4k.sdk

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AgUi4KAgentConfigTest {

    @Test
    fun testDefaultConfiguration() {
        val config = AgUi4kAgentConfig()
        
        assertEquals("X-API-Key", config.apiKeyHeader)
        assertEquals(false, config.debug)
        assertEquals(600_000L, config.requestTimeout)
        assertEquals(30_000L, config.connectTimeout)
        assertNotNull(config.headers)
        assertTrue(config.headers.isEmpty())
        assertTrue(config.context.isEmpty())
    }

    @Test
    fun testBuildHeadersWithBearerToken() {
        val config = AgUi4kAgentConfig().apply {
            bearerToken = "test-token"
        }
        
        val headers = config.buildHeaders()
        assertEquals("Bearer test-token", headers["Authorization"])
    }

    @Test
    fun testBuildHeadersWithApiKey() {
        val config = AgUi4kAgentConfig().apply {
            apiKey = "test-api-key"
            apiKeyHeader = "X-Custom-Key"
        }
        
        val headers = config.buildHeaders()
        assertEquals("test-api-key", headers["X-Custom-Key"])
    }

    @Test
    fun testBuildHeadersWithCustomHeaders() {
        val config = AgUi4kAgentConfig().apply {
            headers["Custom-Header"] = "custom-value"
        }
        
        val headers = config.buildHeaders()
        assertEquals("custom-value", headers["Custom-Header"])
    }
}