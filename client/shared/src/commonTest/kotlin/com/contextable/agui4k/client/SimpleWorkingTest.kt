package com.contextable.agui4k.client

import com.contextable.agui4k.client.data.model.AgentConfig
import com.contextable.agui4k.client.data.model.AuthMethod
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertNotNull

/**
 * Simple test to verify the test setup is working correctly.
 */
class SimpleWorkingTest {

    @Test
    fun testBasicAssertion() {
        assertEquals(4, 2 + 2)
        assertTrue(true)
    }

    @Test
    fun testAgentConfigCreation() {
        val agent = AgentConfig(
            id = "test-123",
            name = "Test Agent",
            url = "https://example.com/agent",
            description = "A test agent",
            authMethod = AuthMethod.None()
        )

        assertEquals("test-123", agent.id)
        assertEquals("Test Agent", agent.name)
        assertEquals("https://example.com/agent", agent.url)
        assertTrue(agent.authMethod is AuthMethod.None)
    }

    @Test
    fun testAuthMethodTypes() {
        val none = AuthMethod.None()
        val apiKey = AuthMethod.ApiKey("test-key", "X-API-Key")
        val bearer = AuthMethod.BearerToken("test-token")
        val basic = AuthMethod.BasicAuth("user", "pass")

        assertTrue(none is AuthMethod.None)
        assertTrue(apiKey is AuthMethod.ApiKey)
        assertTrue(bearer is AuthMethod.BearerToken)
        assertTrue(basic is AuthMethod.BasicAuth)

        assertEquals("test-key", apiKey.key)
        assertEquals("X-API-Key", apiKey.headerName)
        assertEquals("test-token", bearer.token)
        assertEquals("user", basic.username)
        assertEquals("pass", basic.password)
    }
}