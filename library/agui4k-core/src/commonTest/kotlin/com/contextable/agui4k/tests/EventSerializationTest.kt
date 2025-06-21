package com.contextable.agui4k.tests

import com.contextable.agui4k.core.types.*
import com.contextable.agui4k.core.serialization.AgUiJson
import kotlinx.serialization.json.*
import kotlin.test.*

class EventSerializationTest {

    private val json = AgUiJson

    // ========== Lifecycle Events Tests ==========

    @Test
    fun testRunStartedEventSerialization() {
        val event = RunStartedEvent(
            threadId = "thread_123",
            runId = "run_456",
            timestamp = 1234567890L
        )

        val jsonString = json.encodeToString(BaseEvent.serializer(), event)
        val jsonObj = json.parseToJsonElement(jsonString).jsonObject

        // Verify the discriminator is present
        assertEquals("RUN_STARTED", jsonObj["type"]?.jsonPrimitive?.content)

        // Verify fields
        assertEquals("thread_123", jsonObj["threadId"]?.jsonPrimitive?.content)
        assertEquals("run_456", jsonObj["runId"]?.jsonPrimitive?.content)
        assertEquals(1234567890L, jsonObj["timestamp"]?.jsonPrimitive?.longOrNull)

        // Verify deserialization
        val decoded = json.decodeFromString<BaseEvent>(jsonString)
        assertTrue(decoded is RunStartedEvent)
        assertEquals(event, decoded)
    }

    @Test
    fun testRunFinishedEventSerialization() {
        val event = RunFinishedEvent(
            threadId = "thread_123",
            runId = "run_456"
        )

        val jsonString = json.encodeToString<BaseEvent>(event)
        val decoded = json.decodeFromString<BaseEvent>(jsonString)

        assertTrue(decoded is RunFinishedEvent)
        assertEquals(event.threadId, decoded.threadId)
        assertEquals(event.runId, decoded.runId)
    }

    @Test
    fun testRunErrorEventSerialization() {
        val event = RunErrorEvent(
            message = "Something went wrong",
            code = "ERR_001"
        )

        val jsonString = json.encodeToString<BaseEvent>(event)
        val jsonObj = json.parseToJsonElement(jsonString).jsonObject

        assertEquals("RUN_ERROR", jsonObj["type"]?.jsonPrimitive?.content)
        assertEquals("Something went wrong", jsonObj["message"]?.jsonPrimitive?.content)
        assertEquals("ERR_001", jsonObj["code"]?.jsonPrimitive?.content)

        val decoded = json.decodeFromString<BaseEvent>(jsonString)
        assertTrue(decoded is RunErrorEvent)
        assertEquals(event, decoded)
    }

    @Test
    fun testStepEventsSerialization() {
        val startEvent = StepStartedEvent(stepName = "data_processing")
        val finishEvent = StepFinishedEvent(stepName = "data_processing")

        // Test start event
        val startJson = json.encodeToString<BaseEvent>(startEvent)
        val decodedStart = json.decodeFromString<BaseEvent>(startJson)

        assertTrue(decodedStart is StepStartedEvent)
        assertEquals("data_processing", decodedStart.stepName)

        // Test finish event
        val finishJson = json.encodeToString<BaseEvent>(finishEvent)
        val decodedFinish = json.decodeFromString<BaseEvent>(finishJson)

        assertTrue(decodedFinish is StepFinishedEvent)
        assertEquals("data_processing", decodedFinish.stepName)
    }

    // ========== Text Message Events Tests ==========

    @Test
    fun testTextMessageStartEventSerialization() {
        val event = TextMessageStartEvent(
            messageId = "msg_789",
            role = "assistant"
        )

        val jsonString = json.encodeToString<BaseEvent>(event)
        val jsonObj = json.parseToJsonElement(jsonString).jsonObject

        assertEquals("TEXT_MESSAGE_START", jsonObj["type"]?.jsonPrimitive?.content)
        assertEquals("msg_789", jsonObj["messageId"]?.jsonPrimitive?.content)
        assertEquals("assistant", jsonObj["role"]?.jsonPrimitive?.content)

        val decoded = json.decodeFromString<BaseEvent>(jsonString)
        assertTrue(decoded is TextMessageStartEvent)
        assertEquals(event, decoded)
    }

    @Test
    fun testTextMessageContentEventSerialization() {
        val event = TextMessageContentEvent(
            messageId = "msg_789",
            delta = "Hello, world!"
        )

        val jsonString = json.encodeToString<BaseEvent>(event)
        val decoded = json.decodeFromString<BaseEvent>(jsonString)

        assertTrue(decoded is TextMessageContentEvent)
        assertEquals(event.messageId, decoded.messageId)
        assertEquals(event.delta, decoded.delta)
    }

    @Test
    fun testTextMessageContentEmptyDeltaValidation() {
        assertFailsWith<IllegalArgumentException> {
            TextMessageContentEvent(
                messageId = "msg_123",
                delta = ""
            )
        }
    }

    @Test
    fun testTextMessageEndEventSerialization() {
        val event = TextMessageEndEvent(messageId = "msg_789")

        val jsonString = json.encodeToString<BaseEvent>(event)
        val decoded = json.decodeFromString<BaseEvent>(jsonString)

        assertTrue(decoded is TextMessageEndEvent)
        assertEquals(event, decoded)
    }

    // ========== Tool Call Events Tests ==========

    @Test
    fun testToolCallStartEventSerialization() {
        val event = ToolCallStartEvent(
            toolCallId = "tool_123",
            toolCallName = "get_weather",
            parentMessageId = "msg_456"
        )

        val jsonString = json.encodeToString<BaseEvent>(event)
        val jsonObj = json.parseToJsonElement(jsonString).jsonObject

        assertEquals("TOOL_CALL_START", jsonObj["type"]?.jsonPrimitive?.content)
        assertEquals("tool_123", jsonObj["toolCallId"]?.jsonPrimitive?.content)
        assertEquals("get_weather", jsonObj["toolCallName"]?.jsonPrimitive?.content)
        assertEquals("msg_456", jsonObj["parentMessageId"]?.jsonPrimitive?.content)

        val decoded = json.decodeFromString<BaseEvent>(jsonString)
        assertTrue(decoded is ToolCallStartEvent)
        assertEquals(event, decoded)
    }

    @Test
    fun testToolCallArgsEventSerialization() {
        val event = ToolCallArgsEvent(
            toolCallId = "tool_123",
            delta = """{"location": "Paris"}"""
        )

        val jsonString = json.encodeToString<BaseEvent>(event)
        val decoded = json.decodeFromString<BaseEvent>(jsonString)

        assertTrue(decoded is ToolCallArgsEvent)
        assertEquals(event, decoded)
    }

    @Test
    fun testToolCallEndEventSerialization() {
        val event = ToolCallEndEvent(toolCallId = "tool_123")

        val jsonString = json.encodeToString<BaseEvent>(event)
        val decoded = json.decodeFromString<BaseEvent>(jsonString)

        assertTrue(decoded is ToolCallEndEvent)
        assertEquals(event, decoded)
    }

    // ========== State Management Events Tests ==========

    @Test
    fun testStateSnapshotEventSerialization() {
        val snapshot = buildJsonObject {
            put("user", "john_doe")
            put("preferences", buildJsonObject {
                put("theme", "dark")
                put("language", "en")
            })
        }

        val event = StateSnapshotEvent(snapshot = snapshot)

        val jsonString = json.encodeToString<BaseEvent>(event)
        val decoded = json.decodeFromString<BaseEvent>(jsonString)

        assertTrue(decoded is StateSnapshotEvent)
        assertEquals(event.snapshot, decoded.snapshot)
    }

    @Test
    fun testStateDeltaEventSerialization() {
        // Create patches as JsonArray (the format expected by JSON Patch)
        val patches = buildJsonArray {
            addJsonObject {
                put("op", "add")
                put("path", "/user/name")
                put("value", "John Doe")
            }
            addJsonObject {
                put("op", "replace")
                put("path", "/counter")
                put("value", 43)
            }
            addJsonObject {
                put("op", "remove")
                put("path", "/temp")
            }
            addJsonObject {
                put("op", "move")
                put("path", "/foo")
                put("from", "/bar")
            }
        }

        val event = StateDeltaEvent(delta = patches)

        val jsonString = json.encodeToString<BaseEvent>(event)
        val decoded = json.decodeFromString<BaseEvent>(jsonString)

        assertTrue(decoded is StateDeltaEvent)
        assertTrue(decoded.delta is JsonArray)
        assertEquals(4, decoded.delta.jsonArray.size)

        // Verify first patch
        val firstPatch = decoded.delta.jsonArray[0].jsonObject
        assertEquals("add", firstPatch["op"]?.jsonPrimitive?.content)
        assertEquals("/user/name", firstPatch["path"]?.jsonPrimitive?.content)
        assertEquals("John Doe", firstPatch["value"]?.jsonPrimitive?.content)
    }

    @Test
    fun testStateDeltaWithJsonNull() {
        val patches = buildJsonArray {
            addJsonObject {
                put("op", "add")
                put("path", "/nullable")
                put("value", JsonNull)
            }
            addJsonObject {
                put("op", "test")
                put("path", "/other")
                put("value", JsonNull)
            }
        }

        val event = StateDeltaEvent(delta = patches)
        val jsonString = json.encodeToString<BaseEvent>(event)
        val decoded = json.decodeFromString<BaseEvent>(jsonString)

        assertTrue(decoded is StateDeltaEvent)
        val patchArray = decoded.delta.jsonArray
        assertEquals(2, patchArray.size)

        patchArray.forEach { patch ->
            assertEquals(JsonNull, patch.jsonObject["value"])
        }
    }

    @Test
    fun testStateDeltaEmptyArray() {
        val event = StateDeltaEvent(delta = buildJsonArray { })

        val jsonString = json.encodeToString<BaseEvent>(event)
        val decoded = json.decodeFromString<BaseEvent>(jsonString)

        assertTrue(decoded is StateDeltaEvent)
        assertTrue(decoded.delta is JsonArray)
        assertEquals(0, decoded.delta.jsonArray.size)
    }

    @Test
    fun testMessagesSnapshotEventSerialization() {
        val messages = listOf(
            UserMessage(id = "msg_1", content = "Hello"),
            AssistantMessage(id = "msg_2", content = "Hi there!")
        )

        val event = MessagesSnapshotEvent(messages = messages)

        val jsonString = json.encodeToString<BaseEvent>(event)
        val decoded = json.decodeFromString<BaseEvent>(jsonString)

        assertTrue(decoded is MessagesSnapshotEvent)
        assertEquals(2, decoded.messages.size)
    }

    // ========== Special Events Tests ==========

    @Test
    fun testRawEventSerialization() {
        val rawData = buildJsonObject {
            put("customField", "customValue")
            put("nested", buildJsonObject {
                put("data", JsonArray(listOf(JsonPrimitive(1), JsonPrimitive(2))))
            })
        }

        val event = RawEvent(
            event = rawData,
            source = "external_system"
        )

        val jsonString = json.encodeToString<BaseEvent>(event)
        val decoded = json.decodeFromString<BaseEvent>(jsonString)

        assertTrue(decoded is RawEvent)
        assertEquals(event.event, decoded.event)
        assertEquals(event.source, decoded.source)
    }

    @Test
    fun testCustomEventSerialization() {
        val customValue = buildJsonObject {
            put("action", "user_clicked")
            put("element", "submit_button")
        }

        val event = CustomEvent(
            name = "ui_interaction",
            value = customValue
        )

        val jsonString = json.encodeToString<BaseEvent>(event)
        val decoded = json.decodeFromString<BaseEvent>(jsonString)

        assertTrue(decoded is CustomEvent)
        assertEquals(event.name, decoded.name)
        assertEquals(event.value, decoded.value)
    }

    // ========== Null Handling Tests ==========

    @Test
    fun testNullFieldsNotSerialized() {
        val event = RunErrorEvent(
            message = "Error",
            code = null,
            timestamp = null,
            rawEvent = null
        )

        val jsonString = json.encodeToString<BaseEvent>(event)
        val jsonObj = json.parseToJsonElement(jsonString).jsonObject

        // With explicitNulls = false, null fields should not be included
        assertFalse(jsonObj.containsKey("code"))
        assertFalse(jsonObj.containsKey("timestamp"))
        assertFalse(jsonObj.containsKey("rawEvent"))
    }

    @Test
    fun testOptionalFieldsWithValues() {
        val rawEvent = buildJsonObject {
            put("original", true)
        }

        val event = TextMessageStartEvent(
            messageId = "msg_123",
            role = "user",
            timestamp = 1234567890L,
            rawEvent = rawEvent
        )

        val jsonString = json.encodeToString<BaseEvent>(event)
        val jsonObj = json.parseToJsonElement(jsonString).jsonObject

        assertEquals(1234567890L, jsonObj["timestamp"]?.jsonPrimitive?.longOrNull)
        assertNotNull(jsonObj["rawEvent"])
    }

    // ========== Event List Serialization ==========

    @Test
    fun testEventListSerialization() {
        val events: List<BaseEvent> = listOf(
            RunStartedEvent(threadId = "t1", runId = "r1"),
            TextMessageStartEvent(messageId = "m1", role = "assistant"),
            TextMessageContentEvent(messageId = "m1", delta = "Hello"),
            TextMessageEndEvent(messageId = "m1"),
            RunFinishedEvent(threadId = "t1", runId = "r1")
        )

        val jsonString = json.encodeToString(events)
        val decoded: List<BaseEvent> = json.decodeFromString(jsonString)

        assertEquals(5, decoded.size)
        assertTrue(decoded[0] is RunStartedEvent)
        assertTrue(decoded[1] is TextMessageStartEvent)
        assertTrue(decoded[2] is TextMessageContentEvent)
        assertTrue(decoded[3] is TextMessageEndEvent)
        assertTrue(decoded[4] is RunFinishedEvent)
    }

    // ========== Protocol Compliance Tests ==========

    @Test
    fun testEventDiscriminatorFormat() {
        // Test that each event type produces the correct discriminator
        val testCases = mapOf(
            RunStartedEvent(threadId = "t", runId = "r") to "RUN_STARTED",
            RunFinishedEvent(threadId = "t", runId = "r") to "RUN_FINISHED",
            RunErrorEvent(message = "err") to "RUN_ERROR",
            StepStartedEvent(stepName = "s") to "STEP_STARTED",
            StepFinishedEvent(stepName = "s") to "STEP_FINISHED",
            TextMessageStartEvent(messageId = "m", role = "a") to "TEXT_MESSAGE_START",
            TextMessageContentEvent(messageId = "m", delta = "d") to "TEXT_MESSAGE_CONTENT",
            TextMessageEndEvent(messageId = "m") to "TEXT_MESSAGE_END",
            ToolCallStartEvent(toolCallId = "t", toolCallName = "n") to "TOOL_CALL_START",
            ToolCallArgsEvent(toolCallId = "t", delta = "{}") to "TOOL_CALL_ARGS",
            ToolCallEndEvent(toolCallId = "t") to "TOOL_CALL_END",
            StateSnapshotEvent(snapshot = JsonNull) to "STATE_SNAPSHOT",
            StateDeltaEvent(delta = buildJsonArray { }) to "STATE_DELTA",  // Changed to JsonArray
            MessagesSnapshotEvent(messages = emptyList()) to "MESSAGES_SNAPSHOT",
            RawEvent(event = JsonNull) to "RAW",
            CustomEvent(name = "n", value = JsonNull) to "CUSTOM"
        )

        testCases.forEach { (event, expectedType) ->
            val jsonString = json.encodeToString<BaseEvent>(event)
            val jsonObj = json.parseToJsonElement(jsonString).jsonObject
            assertEquals(
                expectedType,
                jsonObj["type"]?.jsonPrimitive?.content,
                "Event ${event::class.simpleName} should have discriminator $expectedType"
            )
        }
    }

    @Test
    fun testUnknownEventTypeHandling() {
        // Test that unknown event types are rejected
        val invalidJson = """{"type":"UNKNOWN_EVENT","data":"test"}"""

        assertFailsWith<Exception> {
            json.decodeFromString<BaseEvent>(invalidJson)
        }
    }

    @Test
    fun testForwardCompatibility() {
        // Test that extra fields are ignored
        val jsonWithExtra = """
            {
                "type": "RUN_STARTED",
                "threadId": "t1",
                "runId": "r1",
                "futureField": "ignored",
                "anotherField": 123
            }
        """.trimIndent()

        val decoded = json.decodeFromString<BaseEvent>(jsonWithExtra)
        assertTrue(decoded is RunStartedEvent)
        assertEquals("t1", decoded.threadId)
        assertEquals("r1", decoded.runId)
    }
}