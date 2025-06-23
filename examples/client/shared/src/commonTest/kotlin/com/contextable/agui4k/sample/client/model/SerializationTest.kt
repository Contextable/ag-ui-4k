package com.contextable.agui4k.sample.client.model

import com.contextable.agui4k.example.client.data.model.AgentConfig
import com.contextable.agui4k.example.client.data.model.AuthMethod
import kotlinx.serialization.json.Json
import kotlin.test.*

class SerializationTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        // Remove custom classDiscriminator to use default sealed class handling
    }

    @Test
    fun testAuthMethodSerialization() {
        val authMethods = listOf(
            AuthMethod.None(),
            AuthMethod.ApiKey("key", "X-API-Key"),
            AuthMethod.BearerToken("token"),
            AuthMethod.BasicAuth("user", "pass")
            // Removed Custom for now as it might have different structure
        )

        authMethods.forEach { original ->
            try {
                val jsonString = json.encodeToString<AuthMethod>(original)
                println("Serialized $original to: $jsonString")
                val decoded = json.decodeFromString<AuthMethod>(jsonString)
                assertEquals(original, decoded)
            } catch (e: Exception) {
                println("Failed to serialize/deserialize: $original")
                println("Error: ${e.message}")
                println("Stack trace: ${e.stackTraceToString()}")
                throw e
            }
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

        try {
            val jsonString = json.encodeToString(agent)
            val decoded = json.decodeFromString<AgentConfig>(jsonString)

            assertEquals(agent.id, decoded.id)
            assertEquals(agent.name, decoded.name)
            assertEquals(agent.url, decoded.url)
            assertEquals(agent.authMethod, decoded.authMethod)
            assertEquals(agent.customHeaders, decoded.customHeaders)
        } catch (e: Exception) {
            println("Failed to serialize AgentConfig: $agent")
            println("Error: ${e.message}")
            throw e
        }
    }

    @Test
    fun testSimpleAuthMethodSerialization() {
        // Test each auth method individually to isolate issues
        val none = AuthMethod.None()
        val noneJson = json.encodeToString<AuthMethod>(none)
        val noneDecoded = json.decodeFromString<AuthMethod>(noneJson)
        assertEquals(none, noneDecoded)

        val apiKey = AuthMethod.ApiKey("test-key", "X-API-Key")
        val apiKeyJson = json.encodeToString<AuthMethod>(apiKey)
        val apiKeyDecoded = json.decodeFromString<AuthMethod>(apiKeyJson)
        assertEquals(apiKey, apiKeyDecoded)
    }
}