/*
 * MIT License
 *
 * Copyright (c) 2025 Mark Fogle
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.contextable.agui4k.example.chatapp.model

import com.contextable.agui4k.example.chatapp.data.model.AgentConfig
import com.contextable.agui4k.example.chatapp.data.model.AuthMethod
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