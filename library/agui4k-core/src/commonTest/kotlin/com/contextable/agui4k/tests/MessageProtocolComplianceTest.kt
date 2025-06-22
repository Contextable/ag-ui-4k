package com.contextable.agui4k.tests

import com.contextable.agui4k.core.types.AgUiJson
import com.contextable.agui4k.core.types.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.*
import kotlin.test.*

@OptIn(ExperimentalSerializationApi::class)
class MessageProtocolComplianceTest {

    private val json = AgUiJson

    // Test that messages follow AG-UI protocol format

    @Test
    fun testUserMessageProtocolCompliance() {
        val message = UserMessage(
            id = "msg_user_123",
            content = "What's the weather like?"
        )

        val jsonString = json.encodeToString<Message>(message)
        val jsonObj = json.parseToJsonElement(jsonString).jsonObject

        // AG-UI protocol compliance checks
        assertEquals("msg_user_123", jsonObj["id"]?.jsonPrimitive?.content)
        assertEquals("user", jsonObj["role"]?.jsonPrimitive?.content)
        assertEquals("What's the weather like?", jsonObj["content"]?.jsonPrimitive?.content)

        // Ensure no 'type' field (AG-UI uses 'role' only)
        assertFalse(jsonObj.containsKey("type"))

        // Verify optional fields
        assertFalse(jsonObj.containsKey("name")) // null name should not be included

        val decoded = json.decodeFromString<Message>(jsonString)
        assertTrue(decoded is UserMessage)
        assertEquals(message, decoded)
    }

    @Test
    fun testUserMessageWithName() {
        val message = UserMessage(
            id = "msg_user_456",
            content = "Hello!",
            name = "John Doe"
        )

        val jsonString = json.encodeToString<Message>(message)
        val jsonObj = json.parseToJsonElement(jsonString).jsonObject

        assertEquals("John Doe", jsonObj["name"]?.jsonPrimitive?.content)

        val decoded = json.decodeFromString<Message>(jsonString)
        assertTrue(decoded is UserMessage)
        assertEquals("John Doe", decoded.name)
    }

    @Test
    fun testAssistantMessageProtocolCompliance() {
        val message = AssistantMessage(
            id = "msg_asst_789",
            content = "I can help you with that."
        )

        val jsonString = json.encodeToString<Message>(message)
        val jsonObj = json.parseToJsonElement(jsonString).jsonObject

        assertEquals("msg_asst_789", jsonObj["id"]?.jsonPrimitive?.content)
        assertEquals("assistant", jsonObj["role"]?.jsonPrimitive?.content)
        assertEquals("I can help you with that.", jsonObj["content"]?.jsonPrimitive?.content)

        // No toolCalls field when null
        assertFalse(jsonObj.containsKey("toolCalls"))

        val decoded = json.decodeFromString<Message>(jsonString)
        assertTrue(decoded is AssistantMessage)
        assertEquals(message, decoded)
    }

    @Test
    fun testAssistantMessageWithToolCalls() {
        val toolCalls = listOf(
            ToolCall(
                id = "call_abc123",
                function = FunctionCall(
                    name = "get_weather",
                    arguments = """{"location": "New York", "unit": "fahrenheit"}"""
                )
            ),
            ToolCall(
                id = "call_def456",
                function = FunctionCall(
                    name = "get_time",
                    arguments = """{"timezone": "EST"}"""
                )
            )
        )

        val message = AssistantMessage(
            id = "msg_asst_tools",
            content = "Let me check that for you.",
            toolCalls = toolCalls
        )

        val jsonString = json.encodeToString<Message>(message)
        val jsonObj = json.parseToJsonElement(jsonString).jsonObject

        // Verify tool calls structure
        val toolCallsArray = jsonObj["toolCalls"]?.jsonArray
        assertNotNull(toolCallsArray)
        assertEquals(2, toolCallsArray.size)

        // Check first tool call
        val firstCall = toolCallsArray[0].jsonObject
        assertEquals("call_abc123", firstCall["id"]?.jsonPrimitive?.content)
        assertEquals("function", firstCall["type"]?.jsonPrimitive?.content)

        val functionObj = firstCall["function"]?.jsonObject
        assertNotNull(functionObj)
        assertEquals("get_weather", functionObj["name"]?.jsonPrimitive?.content)
        assertEquals(
            """{"location": "New York", "unit": "fahrenheit"}""",
            functionObj["arguments"]?.jsonPrimitive?.content
        )

        val decoded = json.decodeFromString<Message>(jsonString)
        assertTrue(decoded is AssistantMessage)
        assertEquals(message.toolCalls?.size, decoded.toolCalls?.size)
    }

    @Test
    fun testAssistantMessageWithNullContent() {
        // Assistant messages can have null content when using tools
        val message = AssistantMessage(
            id = "msg_asst_null",
            content = null,
            toolCalls = listOf(
                ToolCall(
                    id = "call_123",
                    function = FunctionCall(name = "action", arguments = "{}")
                )
            )
        )

        val jsonString = json.encodeToString<Message>(message)
        val jsonObj = json.parseToJsonElement(jsonString).jsonObject

        // content field should not be present when null
        assertFalse(jsonObj.containsKey("content"))
        assertTrue(jsonObj.containsKey("toolCalls"))

        val decoded = json.decodeFromString<Message>(jsonString)
        assertTrue(decoded is AssistantMessage)
        assertNull(decoded.content)
    }

    @Test
    fun testSystemMessageProtocolCompliance() {
        val message = SystemMessage(
            id = "msg_sys_001",
            content = "You are a helpful assistant."
        )

        val jsonString = json.encodeToString<Message>(message)
        val jsonObj = json.parseToJsonElement(jsonString).jsonObject

        assertEquals("msg_sys_001", jsonObj["id"]?.jsonPrimitive?.content)
        assertEquals("system", jsonObj["role"]?.jsonPrimitive?.content)
        assertEquals("You are a helpful assistant.", jsonObj["content"]?.jsonPrimitive?.content)

        val decoded = json.decodeFromString<Message>(jsonString)
        assertTrue(decoded is SystemMessage)
        assertEquals(message, decoded)
    }

    @Test
    fun testToolMessageProtocolCompliance() {
        val message = ToolMessage(
            id = "msg_tool_result",
            content = """{"temperature": 72, "condition": "sunny"}""",
            toolCallId = "call_abc123"
        )

        val jsonString = json.encodeToString<Message>(message)
        val jsonObj = json.parseToJsonElement(jsonString).jsonObject

        assertEquals("msg_tool_result", jsonObj["id"]?.jsonPrimitive?.content)
        assertEquals("tool", jsonObj["role"]?.jsonPrimitive?.content)
        assertEquals("""{"temperature": 72, "condition": "sunny"}""", jsonObj["content"]?.jsonPrimitive?.content)
        assertEquals("call_abc123", jsonObj["toolCallId"]?.jsonPrimitive?.content)

        val decoded = json.decodeFromString<Message>(jsonString)
        assertTrue(decoded is ToolMessage)
        assertEquals(message, decoded)
    }

    @Test
    fun testDeveloperMessageProtocolCompliance() {
        val message = DeveloperMessage(
            id = "msg_dev_debug",
            content = "Debug: Processing started",
            name = "debugger"
        )

        val jsonString = json.encodeToString<Message>(message)
        val jsonObj = json.parseToJsonElement(jsonString).jsonObject

        assertEquals("msg_dev_debug", jsonObj["id"]?.jsonPrimitive?.content)
        assertEquals("developer", jsonObj["role"]?.jsonPrimitive?.content)
        assertEquals("Debug: Processing started", jsonObj["content"]?.jsonPrimitive?.content)
        assertEquals("debugger", jsonObj["name"]?.jsonPrimitive?.content)

        val decoded = json.decodeFromString<Message>(jsonString)
        assertTrue(decoded is DeveloperMessage)
        assertEquals(message, decoded)
    }

    @Test
    fun testMessageListPolymorphicSerialization() {
        val messages: List<Message> = listOf(
            SystemMessage(id = "1", content = "System initialized"),
            UserMessage(id = "2", content = "Hello"),
            AssistantMessage(
                id = "3",
                content = "Hi! I'll help you.",
                toolCalls = listOf(
                    ToolCall(
                        id = "tc1",
                        function = FunctionCall("greet", "{}")
                    )
                )
            ),
            ToolMessage(id = "4", content = "Greeting sent", toolCallId = "tc1"),
            DeveloperMessage(id = "5", content = "Log entry")
        )

        val jsonString = json.encodeToString(messages)
        val jsonArray = json.parseToJsonElement(jsonString).jsonArray

        assertEquals(5, jsonArray.size)

        // Verify each message maintains correct role
        assertEquals("system", jsonArray[0].jsonObject["role"]?.jsonPrimitive?.content)
        assertEquals("user", jsonArray[1].jsonObject["role"]?.jsonPrimitive?.content)
        assertEquals("assistant", jsonArray[2].jsonObject["role"]?.jsonPrimitive?.content)
        assertEquals("tool", jsonArray[3].jsonObject["role"]?.jsonPrimitive?.content)
        assertEquals("developer", jsonArray[4].jsonObject["role"]?.jsonPrimitive?.content)

        val decoded: List<Message> = json.decodeFromString(jsonString)
        assertEquals(messages.size, decoded.size)

        // Verify type preservation
        assertTrue(decoded[0] is SystemMessage)
        assertTrue(decoded[1] is UserMessage)
        assertTrue(decoded[2] is AssistantMessage)
        assertTrue(decoded[3] is ToolMessage)
        assertTrue(decoded[4] is DeveloperMessage)
    }

    @Test
    fun testMessageContentWithSpecialCharacters() {
        val specialContent = """
            Special characters test:
            - Quotes: "double" and 'single'
            - Newlines: 
            Line 1
            Line 2
            - Tabs:	Tab1	Tab2
            - Backslashes: \path\to\file
            - Unicode: 🚀 ñ © ™
            - JSON in content: {"key": "value"}
        """.trimIndent()

        val message = UserMessage(
            id = "msg_special",
            content = specialContent
        )

        val jsonString = json.encodeToString<Message>(message)
        val decoded = json.decodeFromString<Message>(jsonString)

        assertTrue(decoded is UserMessage)
        assertEquals(specialContent, decoded.content)
    }

    @Test
    fun testToolCallArgumentsSerialization() {
        // Test various argument formats
        val testCases = listOf(
            "{}",
            """{"simple": "value"}""",
            """{"nested": {"key": "value"}}""",
            """{"array": [1, 2, 3]}""",
            """{"mixed": {"str": "text", "num": 42, "bool": true, "null": null}}""",
            """{"escaped": "line1\nline2\ttab"}"""
        )

        testCases.forEach { args ->
            val toolCall = ToolCall(
                id = "test_call",
                function = FunctionCall(
                    name = "test_function",
                    arguments = args
                )
            )

            val message = AssistantMessage(
                id = "test_msg",
                content = null,
                toolCalls = listOf(toolCall)
            )

            val jsonString = json.encodeToString<Message>(message)
            try {
                val decoded = json.decodeFromString<Message>(jsonString)
                assertTrue(decoded is AssistantMessage)
                assertEquals(args, decoded.toolCalls?.first()?.function?.arguments)
            } catch (e: Exception) {
                fail("Failed to serialize/deserialize tool call with arguments: $args - ${e.message}")
            }
        }
    }

    @Test
    fun testMessageIdFormats() {
        // Test various ID formats that might be used
        val idFormats = listOf(
            "simple_id",
            "msg_123456789",
            "00000000-0000-0000-0000-000000000000", // UUID
            "msg_2024_01_15_12_30_45_123",
            "a1b2c3d4e5f6",
            "MESSAGE#USER#12345"
        )

        idFormats.forEach { id ->
            val message = UserMessage(id = id, content = "Test")
            val jsonString = json.encodeToString<Message>(message)
            val decoded = json.decodeFromString<Message>(jsonString)

            assertEquals(id, decoded.id)
        }
    }

    @Test
    fun testRoleEnumCoverage() {
        // Ensure all Role enum values can be used in messages
        val roles = mapOf(
            Role.USER to UserMessage(id = "1", content = "test"),
            Role.ASSISTANT to AssistantMessage(id = "2", content = "test"),
            Role.SYSTEM to SystemMessage(id = "3", content = "test"),
            Role.TOOL to ToolMessage(id = "4", content = "test", toolCallId = "tc"),
            Role.DEVELOPER to DeveloperMessage(id = "5", content = "test")
        )

        roles.forEach { (expectedRole, message) ->
            assertEquals(expectedRole, message.role)

            val jsonString = json.encodeToString<Message>(message)
            val decoded = json.decodeFromString<Message>(jsonString)

            assertEquals(expectedRole, decoded.role)
        }
    }

    @Test
    fun testEmptyContentHandling() {
        // Test messages with empty content (different from null)
        val emptyContentMessage = UserMessage(
            id = "empty_content",
            content = ""
        )

        val jsonString = json.encodeToString<Message>(emptyContentMessage)
        val jsonObj = json.parseToJsonElement(jsonString).jsonObject

        // Empty string should be preserved
        assertEquals("", jsonObj["content"]?.jsonPrimitive?.content)

        val decoded = json.decodeFromString<Message>(jsonString)
        assertTrue(decoded is UserMessage)
        assertEquals("", decoded.content)
    }
}