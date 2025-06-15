package com.contextable.agui4k.client

import com.contextable.agui4k.client.data.model.AgentConfig
import com.contextable.agui4k.client.data.model.AuthMethod
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Simple test to verify the test setup is working correctly.
 * Run this with: ./gradlew :shared:test
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
    fun testJsonSerialization() {
        val json = Json { 
            ignoreUnknownKeys = true 
            isLenient = true
        }
        
        val authMethod = AuthMethod.ApiKey(
            key = "test-key",
            headerName = "X-API-Key"
        )
        
        // Serialize
        val jsonString = json.encodeToString<AuthMethod>(authMethod)
        
        // Deserialize
        val decoded = json.decodeFromString<AuthMethod>(jsonString)
        
        assertTrue(decoded is AuthMethod.ApiKey)
        assertEquals("test-key", (decoded as AuthMethod.ApiKey).key)
        assertEquals("X-API-Key", decoded.headerName)
    }
}