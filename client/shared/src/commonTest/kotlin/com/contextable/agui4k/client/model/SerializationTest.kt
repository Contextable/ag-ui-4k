package com.contextable.agui4k.client.model

import com.contextable.agui4k.client.data.model.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlin.test.*

class SerializationTest {
    
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }
    
    @Test
    fun testAuthMethodSerialization() {
        val authMethods = listOf(
            AuthMethod.None(),
            AuthMethod.ApiKey("key", "X-API-Key"),
            AuthMethod.BearerToken("token"),
            AuthMethod.BasicAuth("user", "pass"),
            AuthMethod.Custom("oauth", mapOf("client_id" to "123"))
        )
        
        authMethods.forEach { original ->
            val jsonString = json.encodeToString<AuthMethod>(original)
            val decoded = json.decodeFromString<AuthMethod>(jsonString)
            assertEquals(original, decoded)
        }
    }
    
    @Test
    fun testAgentConfigSerialization() {
        val agent = AgentConfig(
            id = "test-1",
            name = "Test Agent",
            url = "https://test.com/agent",
            description = "A test agent",
            authMethod = AuthMethod.ApiKey("secret", "X-API-Key"),
            customHeaders = mapOf("X-Custom" to "value")
        )
        
        val jsonString = json.encodeToString(agent)
        val decoded = json.decodeFromString<AgentConfig>(jsonString)
        
        assertEquals(agent.id, decoded.id)
        assertEquals(agent.name, decoded.name)
        assertEquals(agent.url, decoded.url)
        assertEquals(agent.authMethod, decoded.authMethod)
        assertEquals(agent.customHeaders, decoded.customHeaders)
    }
}
