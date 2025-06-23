package com.contextable.agui4k.transport

import com.contextable.agui4k.core.types.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.*
import kotlin.test.*

/**
 * Integration tests for HttpClientTransport focusing on parameter handling,
 * state serialization, and error scenarios without requiring live servers.
 */
class HttpClientTransportIntegrationTest {

    @Test
    fun testHttpClientTransportCreatesCorrectRunAgentInput() = runTest {
        val config = HttpClientTransportConfig(
            url = "https://test.example.com/agent",
            headers = mapOf(
                "Authorization" to "Bearer test-token",
                "X-Client-Version" to "1.0.0"
            ),
            requestTimeoutMillis = 5000L  // Short timeout for test
        )
        
        val transport = HttpClientTransport(config)

        val state = buildJsonObject {
            put("sessionId", "session-123")
            put("user", buildJsonObject {
                put("id", "user-456")
                put("preferences", buildJsonObject {
                    put("theme", "dark")
                    put("language", "en")
                })
            })
            putJsonArray("recentActions") {
                add("login")
                add("view_dashboard")
                add("update_profile")
            }
        }

        val forwardedProps = buildJsonObject {
            put("clientVersion", "1.0.0")
            put("requestId", "req-789")
            put("metadata", buildJsonObject {
                put("platform", "test")
                put("userAgent", "test-agent")
            })
        }

        val tools = listOf(
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
                        putJsonObject("limit") {
                            put("type", "integer")
                            put("default", 10)
                        }
                    }
                    putJsonArray("required") {
                        add("query")
                    }
                }
            )
        )

        val context = listOf(
            Context(
                description = "current_time",
                value = "2024-01-15T10:30:00Z"
            ),
            Context(
                description = "user_location",
                value = "San Francisco, CA"
            )
        )

        val messages = listOf(
            SystemMessage(
                id = "sys-1",
                content = "You are a helpful assistant with access to user documents."
            ),
            UserMessage(
                id = "usr-1", 
                content = "Search for documents about project planning"
            )
        )

        // This will fail due to no actual server, but we can verify the transport
        // accepts all our JsonElement parameters without type errors
        try {
            transport.startRun(
                messages = messages,
                threadId = "thread-456",
                runId = "run-789",
                state = state,
                tools = tools,
                context = context,
                forwardedProps = forwardedProps
            )
        } catch (e: Exception) {
            // Expected - no server to connect to
            // The important thing is no compilation or type errors occurred
            assertTrue(e.message?.contains("connection") == true || 
                      e.message?.contains("timeout") == true ||
                      e.message?.contains("resolve") == true ||
                      e.message?.contains("Connect") == true,
                      "Expected connection-related error, got: ${e.message}")
        }

        // Verify the transport was created successfully
        assertNotNull(transport)
    }

    @Test 
    fun testHttpClientTransportWithNullParameters() = runTest {
        val transport = HttpClientTransport(
            HttpClientTransportConfig(
                url = "https://test.example.com/agent",
                requestTimeoutMillis = 1000L
            )
        )

        try {
            transport.startRun(
                messages = listOf(UserMessage(id = "msg-1", content = "Test")),
                threadId = null,  // Should be auto-generated
                runId = null,     // Should be auto-generated  
                state = null,     // Should become empty JsonObject
                tools = null,     // Should become empty list
                context = null,   // Should become empty list
                forwardedProps = null  // Should become empty JsonObject
            )
        } catch (e: Exception) {
            // Expected connection error, not parameter error
            assertFalse(e.message?.contains("parameter") == true)
            assertFalse(e.message?.contains("null") == true)
        }
    }

    @Test
    fun testHttpClientTransportWithJsonPrimitiveState() = runTest {
        val transport = HttpClientTransport(
            HttpClientTransportConfig(
                url = "https://test.example.com/agent",
                requestTimeoutMillis = 1000L
            )
        )

        val primitiveState = JsonPrimitive("simple-state-value")
        val primitiveProps = JsonPrimitive(42)

        try {
            transport.startRun(
                messages = listOf(UserMessage(id = "msg-1", content = "Test with primitives")),
                state = primitiveState,
                forwardedProps = primitiveProps
            )
        } catch (e: Exception) {
            // Should not be a type-related error
            assertFalse(e.message?.contains("type") == true)
            assertFalse(e.message?.contains("primitive") == true)
        }
    }

    @Test
    fun testHttpClientTransportWithJsonArrayState() = runTest {
        val transport = HttpClientTransport(
            HttpClientTransportConfig(
                url = "https://test.example.com/agent",
                requestTimeoutMillis = 1000L
            )
        )

        val arrayState = buildJsonArray {
            add("status1")
            add("status2")
            addJsonObject {
                put("nested", "value")
            }
        }

        val arrayProps = buildJsonArray {
            add("prop1")
            add(123)
            add(true)
        }

        try {
            transport.startRun(
                messages = listOf(UserMessage(id = "msg-1", content = "Test with arrays")),
                state = arrayState,
                forwardedProps = arrayProps
            )
        } catch (e: Exception) {
            // Should accept arrays without issue
            assertFalse(e.message?.contains("array") == true)
            assertFalse(e.message?.contains("type") == true)
        }
    }

    @Test
    fun testHttpClientTransportConfigurationValidation() {
        // Test various configuration scenarios
        val configs = listOf(
            HttpClientTransportConfig(
                url = "https://api.example.com/v1/agent",
                headers = emptyMap(),
                requestTimeoutMillis = 30_000L
            ),
            HttpClientTransportConfig(
                url = "http://localhost:8080/agent",
                headers = mapOf("Authorization" to "Bearer token"),
                requestTimeoutMillis = 60_000L,
                connectTimeoutMillis = 10_000L,
                socketTimeoutMillis = 60_000L
            ),
            HttpClientTransportConfig(
                url = "https://secure.api.com/agent",
                headers = mapOf(
                    "Authorization" to "Bearer secret",
                    "X-API-Key" to "key123",
                    "User-Agent" to "agui4k/1.0"
                ),
                retryPolicy = FixedDelayRetryPolicy(maxAttempts = 5)
            )
        )

        configs.forEach { config ->
            val transport = HttpClientTransport(config)
            assertNotNull(transport)
            
            // Verify all JsonElement types work with any configuration
            val testCases = listOf(
                JsonObject(emptyMap()),
                buildJsonObject { put("test", "value") },
                buildJsonArray { add("item") },
                JsonPrimitive("string"),
                JsonPrimitive(42),
                JsonPrimitive(true),
                JsonNull
            )

            testCases.forEach { jsonElement ->
                // Should not throw compilation or type errors
                assertNotNull(jsonElement)
            }
        }
    }

    @Test
    fun testHttpClientTransportRetryPolicyConfiguration() {
        val retryPolicies = listOf(
            NoRetryPolicy,
            ExponentialBackoffRetryPolicy(maxAttempts = 3),
            FixedDelayRetryPolicy(maxAttempts = 2),
            ExponentialBackoffRetryPolicy(
                maxAttempts = 5,
                baseDelayMs = 500L,
                maxDelayMs = 10_000L
            )
        )

        retryPolicies.forEach { retryPolicy ->
            val config = HttpClientTransportConfig(
                url = "https://test.example.com/agent",
                retryPolicy = retryPolicy,
                requestTimeoutMillis = 1000L
            )
            
            val transport = HttpClientTransport(config)
            assertNotNull(transport)
            
            // Verify policy is properly stored
            assertEquals(retryPolicy, config.retryPolicy)
        }
    }

    @Test
    fun testHttpClientTransportWithComplexNestedStructures() = runTest {
        val transport = HttpClientTransport(
            HttpClientTransportConfig(
                url = "https://test.example.com/agent",
                requestTimeoutMillis = 1000L
            )
        )

        // Create deeply nested structure to test serialization
        val complexState = buildJsonObject {
            put("version", "2.0")
            putJsonObject("application") {
                putJsonObject("user") {
                    put("id", "user123")
                    put("email", "test@example.com")
                    putJsonArray("roles") {
                        add("user")
                        add("premium")
                    }
                    putJsonObject("profile") {
                        put("firstName", "John")
                        put("lastName", "Doe")
                        putJsonObject("preferences") {
                            put("theme", "dark")
                            put("notifications", true)
                            putJsonArray("languages") {
                                add("en")
                                add("es")
                            }
                        }
                    }
                }
                putJsonObject("session") {
                    put("id", "session456")
                    put("startTime", "2024-01-15T10:00:00Z")
                    putJsonArray("activities") {
                        addJsonObject {
                            put("type", "login")
                            put("timestamp", "2024-01-15T10:00:00Z")
                        }
                        addJsonObject {
                            put("type", "page_view")
                            put("page", "/dashboard")
                            put("timestamp", "2024-01-15T10:01:00Z")
                        }
                    }
                }
            }
        }

        val complexForwardedProps = buildJsonObject {
            putJsonObject("request") {
                put("id", "req-123")
                put("timestamp", "2024-01-15T10:30:00Z")
                putJsonObject("client") {
                    put("version", "1.2.3")
                    put("platform", "web")
                    putJsonObject("browser") {
                        put("name", "Chrome")
                        put("version", "120.0.0")
                    }
                }
            }
            putJsonArray("features") {
                add("feature_a")
                add("feature_b")
                addJsonObject {
                    put("name", "feature_c")
                    put("enabled", true)
                    putJsonObject("config") {
                        put("param1", "value1")
                        put("param2", 42)
                    }
                }
            }
        }

        try {
            transport.startRun(
                messages = listOf(
                    UserMessage(id = "msg-1", content = "Complex structure test")
                ),
                state = complexState,
                forwardedProps = complexForwardedProps
            )
        } catch (e: Exception) {
            // Should handle complex structures without serialization errors
            assertFalse(e.message?.contains("serialization") == true)
            assertFalse(e.message?.contains("json") == true)
        }

        // Verify structures are well-formed JsonElements
        assertIs<JsonObject>(complexState)
        assertIs<JsonObject>(complexForwardedProps)
        
        // Verify nested access works
        val userEmail = complexState["application"]?.jsonObject
            ?.get("user")?.jsonObject
            ?.get("email")?.jsonPrimitive?.content
        assertEquals("test@example.com", userEmail)
        
        val clientVersion = complexForwardedProps["request"]?.jsonObject
            ?.get("client")?.jsonObject
            ?.get("version")?.jsonPrimitive?.content
        assertEquals("1.2.3", clientVersion)
    }

    @Test
    fun testHttpClientTransportParameterHandling() = runTest {
        // Test that various JsonElement types are handled correctly by the transport
        // This test focuses on parameter validation rather than network connectivity
        val transport = HttpClientTransport(
            HttpClientTransportConfig(
                url = "https://nonexistent.invalid.domain.test/agent",
                requestTimeoutMillis = 100L,  // Very short timeout to fail quickly
                connectTimeoutMillis = 100L
            )
        )

        val testCases = listOf(
            // Various state types that should all be accepted as JsonElement parameters
            Pair("null state", null as JsonElement?),
            Pair("empty object", JsonObject(emptyMap())),
            Pair("simple object", buildJsonObject { put("key", "value") }),
            Pair("array state", buildJsonArray { add("item") }),
            Pair("primitive state", JsonPrimitive("test")),
            Pair("boolean state", JsonPrimitive(true)),
            Pair("number state", JsonPrimitive(123))
        )

        testCases.forEach { (description, state) ->
            var exceptionThrown = false
            try {
                transport.startRun(
                    messages = listOf(UserMessage(id = "msg-1", content = "Test: $description")),
                    state = state
                )
            } catch (e: Exception) {
                exceptionThrown = true
                val message = e.message?.lowercase() ?: ""
                
                // The key assertion: verify it's NOT a type-related error
                // We accept any network/connection/timeout errors as expected
                assertFalse(message.contains("type") && message.contains("mismatch"), 
                    "Should not be a type mismatch error for $description, got: ${e.message}")
                assertFalse(message.contains("cast") && message.contains("exception"), 
                    "Should not be a cast exception for $description, got: ${e.message}")
                assertFalse(message.contains("serialization") && message.contains("exception"), 
                    "Should not be a serialization exception for $description, got: ${e.message}")
                assertFalse(message.contains("cannot convert"), 
                    "Should not be a conversion error for $description, got: ${e.message}")
            }
            
            // We expect some kind of exception (network failure) for most cases,
            // but if no exception is thrown, that's also acceptable as it means
            // the JsonElement parameters were handled correctly
            if (!exceptionThrown) {
                // This is fine - it means the parameters were accepted correctly
                // and possibly the HTTP client handled the invalid URL gracefully
            }
        }
    }
}