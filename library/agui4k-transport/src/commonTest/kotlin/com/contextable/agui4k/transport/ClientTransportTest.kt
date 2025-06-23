package com.contextable.agui4k.transport

import com.contextable.agui4k.core.types.*
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.*
import kotlin.test.*

/**
 * Comprehensive tests for ClientTransport implementations.
 * Tests the contract and behavior of the transport layer.
 */
class ClientTransportTest {
    
    @Test
    fun testStartRunWithStateAsJsonObject() = runTest {
        val mockTransport = MockClientTransport()
        
        val state = buildJsonObject {
            put("user", "john_doe")
            put("preferences", buildJsonObject {
                put("theme", "dark")
                put("language", "en")
            })
            put("sessionCount", 5)
        }
        
        val messages = listOf(
            UserMessage(id = "msg1", content = "Hello")
        )
        
        mockTransport.startRun(
            messages = messages,
            state = state
        )
        
        // Verify state was captured correctly
        assertEquals(state, mockTransport.capturedState)
        assertIs<JsonObject>(mockTransport.capturedState)
        
        val capturedState = mockTransport.capturedState as JsonObject
        assertEquals("john_doe", capturedState["user"]?.jsonPrimitive?.content)
        assertEquals(5, capturedState["sessionCount"]?.jsonPrimitive?.int)
        assertEquals("dark", capturedState["preferences"]?.jsonObject?.get("theme")?.jsonPrimitive?.content)
    }
    
    @Test
    fun testStartRunWithStateAsJsonArray() = runTest {
        val mockTransport = MockClientTransport()
        
        val state = buildJsonArray {
            add("item1")
            add(42)
            add(true)
            addJsonObject {
                put("nested", "value")
            }
        }
        
        val messages = listOf(
            UserMessage(id = "msg1", content = "Process array")
        )
        
        mockTransport.startRun(
            messages = messages,
            state = state
        )
        
        // Verify state was captured correctly
        assertEquals(state, mockTransport.capturedState)
        assertIs<JsonArray>(mockTransport.capturedState)
        
        val capturedState = mockTransport.capturedState as JsonArray
        assertEquals(4, capturedState.size)
        assertEquals("item1", capturedState[0].jsonPrimitive.content)
        assertEquals(42, capturedState[1].jsonPrimitive.int)
        assertEquals(true, capturedState[2].jsonPrimitive.boolean)
        assertEquals("value", capturedState[3].jsonObject["nested"]?.jsonPrimitive?.content)
    }
    
    @Test
    fun testStartRunWithStateAsJsonPrimitive() = runTest {
        val mockTransport = MockClientTransport()
        
        val state = JsonPrimitive("simple-state-value")
        
        val messages = listOf(
            UserMessage(id = "msg1", content = "Test primitive")
        )
        
        mockTransport.startRun(
            messages = messages,
            state = state
        )
        
        // Verify state was captured correctly
        assertEquals(state, mockTransport.capturedState)
        assertIs<JsonPrimitive>(mockTransport.capturedState)
        assertEquals("simple-state-value", mockTransport.capturedState?.jsonPrimitive?.content)
    }
    
    @Test
    fun testStartRunWithNullState() = runTest {
        val mockTransport = MockClientTransport()
        
        val messages = listOf(
            UserMessage(id = "msg1", content = "No state")
        )
        
        mockTransport.startRun(
            messages = messages,
            state = null
        )
        
        // Verify null state is preserved
        assertNull(mockTransport.capturedState)
    }
    
    @Test
    fun testStartRunWithForwardedProps() = runTest {
        val mockTransport = MockClientTransport()
        
        val forwardedProps = buildJsonObject {
            put("requestId", "req-123")
            put("clientVersion", "1.0.0")
            put("features", buildJsonArray {
                add("feature1")
                add("feature2")
            })
            put("metadata", buildJsonObject {
                put("source", "mobile")
                put("timestamp", 1234567890)
            })
        }
        
        val messages = listOf(
            UserMessage(id = "msg1", content = "Test forwarded props")
        )
        
        mockTransport.startRun(
            messages = messages,
            forwardedProps = forwardedProps
        )
        
        // Verify forwardedProps was captured correctly
        assertEquals(forwardedProps, mockTransport.capturedForwardedProps)
        assertIs<JsonObject>(mockTransport.capturedForwardedProps)
        
        val captured = mockTransport.capturedForwardedProps as JsonObject
        assertEquals("req-123", captured["requestId"]?.jsonPrimitive?.content)
        assertEquals("1.0.0", captured["clientVersion"]?.jsonPrimitive?.content)
        
        val features = captured["features"]?.jsonArray
        assertNotNull(features)
        assertEquals(2, features.size)
        assertEquals("feature1", features[0].jsonPrimitive.content)
        
        val metadata = captured["metadata"]?.jsonObject
        assertNotNull(metadata)
        assertEquals("mobile", metadata["source"]?.jsonPrimitive?.content)
        assertEquals(1234567890, metadata["timestamp"]?.jsonPrimitive?.long)
    }
    
    @Test
    fun testStartRunWithAllParameters() = runTest {
        val mockTransport = MockClientTransport()
        
        val state = buildJsonObject {
            put("activeUser", "test-user")
        }
        
        val forwardedProps = buildJsonObject {
            put("sessionId", "session-456")
        }
        
        val messages = listOf(
            SystemMessage(id = "sys1", content = "You are a helpful assistant"),
            UserMessage(id = "usr1", content = "Hello")
        )
        
        val tools = listOf(
            Tool(
                name = "get_weather",
                description = "Get weather information",
                parameters = buildJsonObject {
                    put("type", "object")
                    putJsonObject("properties") {
                        putJsonObject("location") {
                            put("type", "string")
                        }
                    }
                }
            )
        )
        
        val context = listOf(
            Context(
                description = "current_time",
                value = "2024-01-01T12:00:00Z"
            ),
            Context(
                description = "user_preferences", 
                value = """{"theme": "dark"}"""
            )
        )
        
        mockTransport.startRun(
            messages = messages,
            threadId = "thread-123",
            runId = "run-789",
            state = state,
            tools = tools,
            context = context,
            forwardedProps = forwardedProps
        )
        
        // Verify all parameters were captured
        assertEquals(2, mockTransport.capturedMessages?.size)
        assertEquals("thread-123", mockTransport.capturedThreadId)
        assertEquals("run-789", mockTransport.capturedRunId)
        assertEquals(state, mockTransport.capturedState)
        assertEquals(1, mockTransport.capturedTools?.size)
        assertEquals("get_weather", mockTransport.capturedTools?.first()?.name)
        assertEquals(2, mockTransport.capturedContext?.size)
        assertEquals("current_time", mockTransport.capturedContext?.first()?.description)
        assertEquals(forwardedProps, mockTransport.capturedForwardedProps)
        assertEquals(1, mockTransport.startRunCallCount)
    }
    
    @Test
    fun testStartRunMultipleCalls() = runTest {
        val mockTransport = MockClientTransport()
        
        // First call
        mockTransport.startRun(
            messages = listOf(UserMessage(id = "msg1", content = "First")),
            state = JsonPrimitive("state1")
        )
        
        assertEquals(1, mockTransport.startRunCallCount)
        assertEquals("state1", mockTransport.capturedState?.jsonPrimitive?.content)
        
        // Second call
        mockTransport.startRun(
            messages = listOf(UserMessage(id = "msg2", content = "Second")),
            state = JsonPrimitive("state2")
        )
        
        assertEquals(2, mockTransport.startRunCallCount)
        assertEquals("state2", mockTransport.capturedState?.jsonPrimitive?.content)
        
        // Reset and verify
        mockTransport.reset()
        assertEquals(0, mockTransport.startRunCallCount)
        assertNull(mockTransport.capturedState)
    }
    
    @Test
    fun testRunSessionWithEvents() = runTest {
        val events = listOf(
            RunStartedEvent(threadId = "thread-1", runId = "run-1"),
            TextMessageStartEvent(messageId = "msg-1"),
            TextMessageContentEvent(delta = "Hello", messageId = "msg-1"),
            TextMessageContentEvent(delta = " world!", messageId = "msg-1"),
            TextMessageEndEvent(messageId = "msg-1"),
            RunFinishedEvent(threadId = "thread-1", runId = "run-1")
        )
        
        val mockTransport = MockClientTransport(responseEvents = events)
        
        val session = mockTransport.startRun(
            messages = listOf(UserMessage(id = "usr1", content = "Test"))
        )
        
        // Collect all events
        val collectedEvents = session.events.toList()
        
        assertEquals(6, collectedEvents.size)
        assertIs<RunStartedEvent>(collectedEvents[0])
        assertIs<TextMessageStartEvent>(collectedEvents[1])
        assertIs<TextMessageContentEvent>(collectedEvents[2])
        assertIs<TextMessageContentEvent>(collectedEvents[3])
        assertIs<TextMessageEndEvent>(collectedEvents[4])
        assertIs<RunFinishedEvent>(collectedEvents[5])
        
        // Session should be inactive after RunFinishedEvent
        assertFalse(session.isActive.value)
    }
    
    @Test
    fun testRunSessionSendMessage() = runTest {
        val mockTransport = MockClientTransport()
        val session = mockTransport.startRun(
            messages = listOf(UserMessage(id = "usr1", content = "Start"))
        ) as MockRunSession
        
        // Send a tool message
        val toolMessage = ToolMessage(
            id = "tool1",
            toolCallId = "call1",
            content = "Tool result"
        )
        
        session.sendMessage(toolMessage)
        
        // Verify message was captured
        assertEquals(1, session.capturedSentMessages.size)
        assertEquals(toolMessage, session.capturedSentMessages.first())
    }
    
    @Test
    fun testRunSessionClose() = runTest {
        val mockTransport = MockClientTransport()
        val session = mockTransport.startRun(
            messages = listOf(UserMessage(id = "usr1", content = "Test"))
        )
        
        assertTrue(session.isActive.value)
        
        session.close()
        
        assertFalse(session.isActive.value)
        
        // Sending message after close should throw
        assertFailsWith<RunSessionClosedException> {
            session.sendMessage(UserMessage(id = "usr2", content = "After close"))
        }
    }
    
    @Test
    fun testTransportError() = runTest {
        val mockTransport = MockClientTransport(
            shouldError = true,
            errorMessage = "Connection failed"
        )
        
        assertFailsWith<TransportConnectionException> {
            mockTransport.startRun(
                messages = listOf(UserMessage(id = "usr1", content = "Test"))
            )
        }
    }
    
    @Test
    fun testComplexStateStructure() = runTest {
        val mockTransport = MockClientTransport()
        
        // Create a complex nested state structure
        val complexState = buildJsonObject {
            put("version", "1.0")
            putJsonArray("users") {
                addJsonObject {
                    put("id", 1)
                    put("name", "Alice")
                    putJsonArray("permissions") {
                        add("read")
                        add("write")
                    }
                }
                addJsonObject {
                    put("id", 2)
                    put("name", "Bob")
                    putJsonArray("permissions") {
                        add("read")
                    }
                }
            }
            putJsonObject("settings") {
                putJsonObject("ui") {
                    put("theme", "dark")
                    put("fontSize", 14)
                }
                putJsonObject("api") {
                    put("timeout", 30)
                    put("retries", 3)
                }
            }
            putJsonArray("history") {
                addJsonObject {
                    put("action", "login")
                    put("timestamp", 1234567890)
                }
                addJsonObject {
                    put("action", "update_profile")
                    put("timestamp", 1234567900)
                }
            }
        }
        
        mockTransport.startRun(
            messages = listOf(UserMessage(id = "usr1", content = "Process complex state")),
            state = complexState
        )
        
        // Verify the complex state was captured correctly
        assertEquals(complexState, mockTransport.capturedState)
        
        val captured = mockTransport.capturedState as JsonObject
        
        // Verify nested structure
        val users = captured["users"]?.jsonArray
        assertNotNull(users)
        assertEquals(2, users.size)
        
        val firstUser = users[0].jsonObject
        assertEquals(1, firstUser["id"]?.jsonPrimitive?.int)
        assertEquals("Alice", firstUser["name"]?.jsonPrimitive?.content)
        
        val permissions = firstUser["permissions"]?.jsonArray
        assertNotNull(permissions)
        assertEquals(2, permissions.size)
        assertEquals("read", permissions[0].jsonPrimitive.content)
        assertEquals("write", permissions[1].jsonPrimitive.content)
        
        val settings = captured["settings"]?.jsonObject
        assertNotNull(settings)
        
        val uiSettings = settings["ui"]?.jsonObject
        assertNotNull(uiSettings)
        assertEquals("dark", uiSettings["theme"]?.jsonPrimitive?.content)
        assertEquals(14, uiSettings["fontSize"]?.jsonPrimitive?.int)
    }
}