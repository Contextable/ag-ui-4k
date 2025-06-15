package com.contextable.agui4k.tests

import com.contextable.agui4k.core.protocol.EventType
import com.contextable.agui4k.core.serialization.AgUiJson
import com.contextable.agui4k.core.types.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import kotlinx.datetime.Clock
import kotlin.test.*

@OptIn(ExperimentalSerializationApi::class)
class EventSerializationTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        explicitNulls = false
    }

    // Lifecycle Events Tests

    @Test
    fun testRunStartedEventSerialization() {
        val event = RunStartedEvent(
            threadId = "thread_123",
            runId = "run_456",
            timestamp = 1234567890L
        )

        val jsonString = json.encodeToString<BaseEvent>(event)
        val jsonObj = json.parseToJsonElement(jsonString).jsonObject

        // The discriminator shows up as "type" in JSON
        assertEquals("RUN_STARTED", jsonObj["type"]?.jsonPrimitive?.content)

        // But it's NOT the type property - verify other fields
        assertEquals("thread_123", jsonObj["threadId"]?.jsonPrimitive?.content)
        assertEquals("run_456", jsonObj["runId"]?.jsonPrimitive?.content)
        assertEquals(1234567890L, jsonObj["timestamp"]?.jsonPrimitive?.longOrNull)

        // Verify deserialization
        val decoded = json.decodeFromString<BaseEvent>(jsonString)
        assertTrue(decoded is RunStartedEvent)
        assertEquals(event, decoded)

        // Verify the type property is correctly set after deserialization
        assertEquals(EventType.RUN_STARTED, decoded.type)
    }

    @Test
    fun testTypeFieldNotSerialized() {
        val event = RunStartedEvent(
            threadId = "test",
            runId = "test"
        )

        // Serialize as concrete type to check raw output
        val concreteJson = Json {
            encodeDefaults = true
            explicitNulls = false
        }

        val jsonString = concreteJson.encodeToString(event)
        val jsonObj = concreteJson.parseToJsonElement(jsonString).jsonObject

        // The raw event should NOT have a "type" field when serialized as concrete type
        assertFalse(jsonObj.containsKey("type"), "type field should not be serialized")

        // But when serialized polymorphically, it should have discriminator
        val polyJsonString = json.encodeToString<BaseEvent>(event)
        val polyJsonObj = json.parseToJsonElement(polyJsonString).jsonObject

        // The discriminator "type" should be present
        assertEquals("RUN_STARTED", polyJsonObj["type"]?.jsonPrimitive?.content)
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
        assertEquals(event, decoded)
    }

    @Test
    fun testRunErrorEventSerialization() {
        val event = RunErrorEvent(
            message = "Something went wrong",
            code = "AGENT_ERROR"
        )

        val jsonString = json.encodeToString<BaseEvent>(event)
        val jsonObj = json.parseToJsonElement(jsonString).jsonObject

        assertEquals("RUN_ERROR", jsonObj["type"]?.jsonPrimitive?.content)
        assertEquals("Something went wrong", jsonObj["message"]?.jsonPrimitive?.content)
        assertEquals("AGENT_ERROR", jsonObj["code"]?.jsonPrimitive?.content)

        val decoded = json.decodeFromString<BaseEvent>(jsonString)
        assertTrue(decoded is RunErrorEvent)
        assertEquals(event, decoded)
    }

    @Test
    fun testStepEventseSerialization() {
        val startEvent = StepStartedEvent(stepName = "data_processing")
        val finishEvent = StepFinishedEvent(stepName = "data_processing")

        val startJson = json.encodeToString<BaseEvent>(startEvent)
        val finishJson = json.encodeToString<BaseEvent>(finishEvent)

        val decodedStart = json.decodeFromString<BaseEvent>(startJson)
        val decodedFinish = json.decodeFromString<BaseEvent>(finishJson)

        assertTrue(decodedStart is StepStartedEvent)
        assertTrue(decodedFinish is StepFinishedEvent)
        assertEquals(startEvent, decodedStart)
        assertEquals(finishEvent, decodedFinish)
    }

    // Text Message Events Tests

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
            delta = "Hello, how can I help you today?"
        )

        val jsonString = json.encodeToString<BaseEvent>(event)
        val decoded = json.decodeFromString<BaseEvent>(jsonString)

        assertTrue(decoded is TextMessageContentEvent)
        assertEquals(event, decoded)

        // Test with special characters
        val specialEvent = TextMessageContentEvent(
            messageId = "msg_special",
            delta = "Special chars: \"quotes\", \nnewlines, \ttabs, \\backslashes"
        )

        val specialJson = json.encodeToString<BaseEvent>(specialEvent)
        val decodedSpecial = json.decodeFromString<BaseEvent>(specialJson)

        assertTrue(decodedSpecial is TextMessageContentEvent)
        assertEquals(specialEvent.delta, decodedSpecial.delta)
    }

    @Test
    fun testTextMessageEndEventSerialization() {
        val event = TextMessageEndEvent(messageId = "msg_789")

        val jsonString = json.encodeToString<BaseEvent>(event)
        val decoded = json.decodeFromString<BaseEvent>(jsonString)

        assertTrue(decoded is TextMessageEndEvent)
        assertEquals(event, decoded)
    }

    // Tool Call Events Tests

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
            delta = """{"location": "Paris", "unit": "celsius"}"""
        )

        val jsonString = json.encodeToString<BaseEvent>(event)
        val decoded = json.decodeFromString<BaseEvent>(jsonString)

        assertTrue(decoded is ToolCallArgsEvent)
        assertEquals(event, decoded)

        // Test partial JSON args
        val partialEvent = ToolCallArgsEvent(
            toolCallId = "tool_123",
            delta = """{"loc"""
        )

        val partialJson = json.encodeToString<BaseEvent>(partialEvent)
        val decodedPartial = json.decodeFromString<BaseEvent>(partialJson)

        assertTrue(decodedPartial is ToolCallArgsEvent)
        assertEquals(partialEvent.delta, decodedPartial.delta)
    }

    @Test
    fun testToolCallEndEventSerialization() {
        val event = ToolCallEndEvent(toolCallId = "tool_123")

        val jsonString = json.encodeToString<BaseEvent>(event)
        val decoded = json.decodeFromString<BaseEvent>(jsonString)

        assertTrue(decoded is ToolCallEndEvent)
        assertEquals(event, decoded)
    }

    // State Management Events Tests

    @Test
    fun testStateSnapshotEventSerialization() {
        val stateData = buildJsonObject {
            put("user", "john_doe")
            put("preferences", buildJsonObject {
                put("theme", "dark")
                put("language", "en")
            })
            put("counter", 42)
        }

        val event = StateSnapshotEvent(snapshot = stateData)

        val jsonString = json.encodeToString<BaseEvent>(event)
        val decoded = json.decodeFromString<BaseEvent>(jsonString)

        assertTrue(decoded is StateSnapshotEvent)
        assertEquals(event.snapshot, decoded.snapshot)
    }

    @Test
    fun testStateDeltaEventSerialization() {
        val patches = listOf(
            JsonPatchOperation(
                op = "add",
                path = "/user/name",
                value = JsonPrimitive("John Doe")
            ),
            JsonPatchOperation(
                op = "replace",
                path = "/counter",
                value = JsonPrimitive(43)
            ),
            JsonPatchOperation(
                op = "remove",
                path = "/temp"
            ),
            JsonPatchOperation(
                op = "move",
                from = "/oldLocation",
                path = "/newLocation"
            )
        )

        val event = StateDeltaEvent(delta = patches)

        val jsonString = json.encodeToString<BaseEvent>(event)
        val decoded = json.decodeFromString<BaseEvent>(jsonString)

        assertTrue(decoded is StateDeltaEvent)
        assertEquals(event.delta.size, decoded.delta.size)

        // Verify each patch operation
        event.delta.zip(decoded.delta).forEach { (original, decoded) ->
            assertEquals(original.op, decoded.op)
            assertEquals(original.path, decoded.path)
            assertEquals(original.value, decoded.value)
            assertEquals(original.from, decoded.from)
        }
    }

    @Test
    fun testMessagesSnapshotEventSerialization() {
        val messages = listOf(
            UserMessage(id = "msg_1", content = "Hello"),
            AssistantMessage(
                id = "msg_2",
                content = "Hi there!",
                toolCalls = listOf(
                    ToolCall(
                        id = "call_1",
                        type = "function",
                        function = FunctionCall(
                            name = "get_info",
                            arguments = "{}"
                        )
                    )
                )
            ),
            ToolMessage(
                id = "msg_3",
                content = "Result data",
                toolCallId = "call_1"
            )
        )

        val event = MessagesSnapshotEvent(messages = messages)

        val jsonString = json.encodeToString<BaseEvent>(event)
        val decoded = json.decodeFromString<BaseEvent>(jsonString)

        assertTrue(decoded is MessagesSnapshotEvent)
        assertEquals(event.messages.size, decoded.messages.size)

        // Verify each message
        event.messages.zip(decoded.messages).forEach { (original, decoded) ->
            assertEquals(original.id, decoded.id)
            assertEquals(original.role, decoded.role)
            assertEquals(original.content, decoded.content)
        }
    }

    // Special Events Tests

    @Test
    fun testRawEventSerialization() {
        val rawData = buildJsonObject {
            put("customField", "customValue")
            put("nested", buildJsonObject {
                put("data", JsonArray(listOf(JsonPrimitive(1), JsonPrimitive(2), JsonPrimitive(3))))
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
            put("timestamp", 1234567890L)
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

    // Event Stream Tests

    @Test
    fun testEventStreamSerialization() {
        val events: List<BaseEvent> = listOf(
            RunStartedEvent(threadId = "t1", runId = "r1"),
            TextMessageStartEvent(messageId = "m1", role = "assistant"),
            TextMessageContentEvent(messageId = "m1", delta = "Processing"),
            ToolCallStartEvent(toolCallId = "tc1", toolCallName = "calculate"),
            ToolCallArgsEvent(toolCallId = "tc1", delta = """{"x": 5}"""),
            ToolCallEndEvent(toolCallId = "tc1"),
            TextMessageContentEvent(messageId = "m1", delta = " your request..."),
            TextMessageEndEvent(messageId = "m1"),
            RunFinishedEvent(threadId = "t1", runId = "r1")
        )

        // Serialize each event
        val serializedEvents = events.map { event ->
            json.encodeToString<BaseEvent>(event)
        }

        // Deserialize and verify
        serializedEvents.zip(events).forEach { (jsonString, originalEvent) ->
            val decoded = json.decodeFromString<BaseEvent>(jsonString)
            assertEquals(originalEvent::class, decoded::class)
            assertEquals(originalEvent, decoded)
        }
    }

    // Edge Cases and Error Tests

    @Test
    fun testEventWithNullOptionalFields() {
        val event = RunErrorEvent(
            message = "Error occurred",
            code = null,
            timestamp = null,
            rawEvent = null
        )

        val jsonString = json.encodeToString<BaseEvent>(event)
        val jsonObj = json.parseToJsonElement(jsonString).jsonObject

        // Verify nulls are not included (explicitNulls = false)
        assertFalse(jsonObj.containsKey("code"))
        assertFalse(jsonObj.containsKey("timestamp"))
        assertFalse(jsonObj.containsKey("rawEvent"))

        val decoded = json.decodeFromString<BaseEvent>(jsonString)
        assertTrue(decoded is RunErrorEvent)
        assertEquals(event, decoded)
    }

    @Test
    fun testEventWithRawEventField() {
        val originalEvent = buildJsonObject {
            put("type", "ORIGINAL_EVENT")
            put("data", "some data")
        }

        val event = TextMessageStartEvent(
            messageId = "msg_123",
            role = "assistant",
            rawEvent = originalEvent
        )

        val jsonString = json.encodeToString<BaseEvent>(event)
        val decoded = json.decodeFromString<BaseEvent>(jsonString)

        assertTrue(decoded is TextMessageStartEvent)
        assertEquals(event.rawEvent, decoded.rawEvent)
    }

    @Test
    fun testEventTypeEnumCoverage() {
        // Verify all EventType enum values have corresponding event classes
        val eventTypes = EventType.entries.toSet()
        val coveredTypes = mutableSetOf<EventType>()

        // Create one event of each type to ensure they can be serialized
        val testEvents = listOf(
            RunStartedEvent(threadId = "t", runId = "r"),
            RunFinishedEvent(threadId = "t", runId = "r"),
            RunErrorEvent(message = "error"),
            StepStartedEvent(stepName = "step"),
            StepFinishedEvent(stepName = "step"),
            TextMessageStartEvent(messageId = "m", role = "assistant"),
            TextMessageContentEvent(messageId = "m", delta = "text"),
            TextMessageEndEvent(messageId = "m"),
            ToolCallStartEvent(toolCallId = "tc", toolCallName = "tool"),
            ToolCallArgsEvent(toolCallId = "tc", delta = "{}"),
            ToolCallEndEvent(toolCallId = "tc"),
            StateSnapshotEvent(snapshot = JsonNull),
            StateDeltaEvent(delta = emptyList()),
            MessagesSnapshotEvent(messages = emptyList()),
            RawEvent(event = JsonNull),
            CustomEvent(name = "custom", value = JsonNull)
        )

        testEvents.forEach { event ->
            val jsonString = json.encodeToString<BaseEvent>(event)
            val decoded = json.decodeFromString<BaseEvent>(jsonString)
            assertEquals(event.type, decoded.type)
            coveredTypes.add(event.type)
        }

        // Verify all event types are covered
        assertEquals(eventTypes, coveredTypes)
    }
}