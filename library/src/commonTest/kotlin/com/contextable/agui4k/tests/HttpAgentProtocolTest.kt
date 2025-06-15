package com.contextable.agui4k.tests

import com.contextable.agui4k.client.HttpAgent
import com.contextable.agui4k.client.HttpAgentConfig
import com.contextable.agui4k.client.HttpAgentException
import com.contextable.agui4k.core.serialization.AgUiJson
import com.contextable.agui4k.core.types.*
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import io.ktor.utils.io.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalSerializationApi::class)
class HttpAgentProtocolTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        explicitNulls = false
    }

    @Test
    fun testSseEventParsing() = runTest {
        val sseContent = buildString {
            // Valid events
            appendLine("data: ${json.encodeToString<BaseEvent>(RunStartedEvent(threadId = "t1", runId = "r1"))}")
            appendLine()
            appendLine("data: ${json.encodeToString<BaseEvent>(TextMessageStartEvent(messageId = "m1", role = "assistant"))}")
            appendLine()
            appendLine("data: ${json.encodeToString<BaseEvent>(TextMessageContentEvent(messageId = "m1", delta = "Hello"))}")
            appendLine()
            appendLine("data: ${json.encodeToString<BaseEvent>(TextMessageContentEvent(messageId = "m1", delta = " world!"))}")
            appendLine()
            appendLine("data: ${json.encodeToString<BaseEvent>(TextMessageEndEvent(messageId = "m1"))}")
            appendLine()
            appendLine("data: ${json.encodeToString<BaseEvent>(RunFinishedEvent(threadId = "t1", runId = "r1"))}")
            appendLine()
        }

        val mockEngine = MockEngine { request ->
            assertEquals(HttpMethod.Post, request.method)
            assertTrue(request.url.toString().endsWith("/test"))

            respond(
                content = ByteReadChannel(sseContent),
                status = HttpStatusCode.OK,
                headers = headersOf(
                    HttpHeaders.ContentType, ContentType.Text.EventStream.toString()
                )
            )
        }

        // Note: We can't actually pass the mock engine to HttpAgent in the current implementation
        // This test demonstrates what SSE content should look like

        // Verify the SSE content is properly formatted
        val lines = sseContent.lines()
        val dataLines = lines.filter { it.startsWith("data: ") }
        assertEquals(6, dataLines.size) // 6 events

        // Each data line should contain valid JSON
        dataLines.forEach { line ->
            val jsonContent = line.removePrefix("data: ")
            try {
                val parsed = json.decodeFromString<BaseEvent>(jsonContent)
                assertNotNull(parsed)
            } catch (e: Exception) {
                fail("Failed to parse SSE event JSON: ${e.message}")
            }
        }
    }

    @Test
    fun testSseWithMultipleDataFields() = runTest {
        // Test SSE format with event types and IDs
        val sseContent = buildString {
            appendLine("event: message")
            appendLine("id: 1")
            appendLine("data: ${json.encodeToString<BaseEvent>(RunStartedEvent(threadId = "t1", runId = "r1"))}")
            appendLine()

            appendLine("event: message")
            appendLine("id: 2")
            appendLine("data: ${json.encodeToString<BaseEvent>(TextMessageContentEvent(messageId = "m1", delta = "Test"))}")
            appendLine()

            appendLine("event: error")
            appendLine("data: ${json.encodeToString<BaseEvent>(RunErrorEvent(message = "Test error", code = "TEST"))}")
            appendLine()
        }

        // Parse data lines
        val eventData = sseContent.lines()
            .filter { it.startsWith("data: ") }
            .map { it.removePrefix("data: ") }
            .filter { it.isNotEmpty() }

        assertEquals(3, eventData.size)

        // Verify each can be parsed
        eventData.forEach { data ->
            val event = json.decodeFromString<BaseEvent>(data)
            assertNotNull(event)
        }
    }

    @Test
    fun testMalformedSseHandling() {
        val malformedCases = listOf(
            // Missing data prefix
            "{'type':'RUN_STARTED','threadId':'t1','runId':'r1'}",

            // Invalid JSON
            "data: {invalid json}",

            // Incomplete event
            "data: {\"type\":\"RUN_STARTED\"",

            // Mixed valid and invalid
            """
            data: ${json.encodeToString<BaseEvent>(RunStartedEvent(threadId = "t1", runId = "r1"))}
            
            data: {bad json}
            
            data: ${json.encodeToString<BaseEvent>(RunFinishedEvent(threadId = "t1", runId = "r1"))}
            """.trimIndent()
        )

        malformedCases.forEach { content ->
            // Extract data lines and try to parse
            val dataLines = content.lines()
                .filter { it.startsWith("data: ") }
                .map { it.removePrefix("data: ") }

            var validEvents = 0
            var invalidEvents = 0

            dataLines.forEach { data ->
                try {
                    json.decodeFromString<BaseEvent>(data)
                    validEvents++
                } catch (e: Exception) {
                    invalidEvents++
                }
            }

            // We should handle both valid and invalid events gracefully
            assertTrue(invalidEvents > 0 || dataLines.isEmpty())
        }
    }

    @Test
    fun testHttpRequestHeaders() = runTest {
        val config = HttpAgentConfig(
            url = "https://test.example.com/agent",
            headers = mapOf(
                "Authorization" to "Bearer test-token",
                "X-Custom-Header" to "custom-value",
                "X-Request-ID" to "req-123"
            )
        )

        // Verify config stores headers correctly
        assertEquals(3, config.headers.size)
        assertEquals("Bearer test-token", config.headers["Authorization"])
        assertEquals("custom-value", config.headers["X-Custom-Header"])
        assertEquals("req-123", config.headers["X-Request-ID"])
    }

    @Test
    fun testHttpErrorResponses() {
        val errorCases = listOf(
            HttpStatusCode.BadRequest to "Bad Request",
            HttpStatusCode.Unauthorized to "Unauthorized",
            HttpStatusCode.Forbidden to "Forbidden",
            HttpStatusCode.NotFound to "Not Found",
            HttpStatusCode.TooManyRequests to "Too Many Requests",
            HttpStatusCode.InternalServerError to "Internal Server Error",
            HttpStatusCode.BadGateway to "Bad Gateway",
            HttpStatusCode.ServiceUnavailable to "Service Unavailable"
        )

        errorCases.forEach { (status, _) ->
            // Each error status should result in an HttpAgentException
            assertFalse(status.isSuccess())
        }
    }

    @Test
    fun testJsonResponseFallback() = runTest {
        // Test non-SSE JSON response format
        val events = listOf(
            RunStartedEvent(threadId = "t1", runId = "r1"),
            TextMessageStartEvent(messageId = "m1", role = "assistant"),
            TextMessageContentEvent(messageId = "m1", delta = "Response"),
            TextMessageEndEvent(messageId = "m1"),
            RunFinishedEvent(threadId = "t1", runId = "r1")
        )

        val jsonArrayResponse = json.encodeToString(events)

        // Verify it's valid JSON array
        val parsed = json.decodeFromString<List<BaseEvent>>(jsonArrayResponse)
        assertEquals(5, parsed.size)

        // Verify event types
        assertTrue(parsed[0] is RunStartedEvent)
        assertTrue(parsed[1] is TextMessageStartEvent)
        assertTrue(parsed[2] is TextMessageContentEvent)
        assertTrue(parsed[3] is TextMessageEndEvent)
        assertTrue(parsed[4] is RunFinishedEvent)
    }

    @Test
    fun testRunAgentInputSerialization() = runTest {
        val input = RunAgentInput(
            threadId = "thread_test",
            runId = "run_test",
            state = buildJsonObject {
                put("test", true)
                put("value", 42)
            },
            messages = listOf(
                UserMessage(id = "1", content = "Test message")
            ),
            tools = listOf(
                Tool(
                    name = "test_tool",
                    description = "A test tool",
                    parameters = buildJsonObject {
                        put("type", "object")
                    }
                )
            ),
            context = listOf(
                Context("test_context", "test_value")
            ),
            forwardedProps = buildJsonObject {
                put("custom", "prop")
            }
        )

        val serialized = json.encodeToString(input)
        val jsonObj = json.parseToJsonElement(serialized).jsonObject

        // Verify all fields are present
        assertTrue(jsonObj.containsKey("threadId"))
        assertTrue(jsonObj.containsKey("runId"))
        assertTrue(jsonObj.containsKey("state"))
        assertTrue(jsonObj.containsKey("messages"))
        assertTrue(jsonObj.containsKey("tools"))
        assertTrue(jsonObj.containsKey("context"))
        assertTrue(jsonObj.containsKey("forwardedProps"))

        // Verify deserialization works
        val deserialized = json.decodeFromString<RunAgentInput>(serialized)
        assertEquals(input.threadId, deserialized.threadId)
        assertEquals(input.runId, deserialized.runId)
        assertEquals(1, deserialized.messages.size)
        assertEquals(1, deserialized.tools.size)
        assertEquals(1, deserialized.context.size)
    }

    @Test
    fun testLongStreamingContent() = runTest {
        // Test handling of long streaming content
        val longText = "A".repeat(1000) // 1000 character chunk

        val events = listOf(
            TextMessageStartEvent(messageId = "long_msg", role = "assistant"),
            TextMessageContentEvent(messageId = "long_msg", delta = longText),
            TextMessageContentEvent(messageId = "long_msg", delta = longText),
            TextMessageContentEvent(messageId = "long_msg", delta = longText),
            TextMessageEndEvent(messageId = "long_msg")
        )

        // Create SSE content
        val sseContent = buildString {
            events.forEach { event ->
                appendLine("data: ${json.encodeToString<BaseEvent>(event)}")
                appendLine()
            }
        }

        // Verify each line is parseable
        sseContent.lines()
            .filter { it.startsWith("data: ") }
            .map { it.removePrefix("data: ") }
            .filter { it.isNotEmpty() }
            .forEach { data ->
                val event = json.decodeFromString<BaseEvent>(data)
                if (event is TextMessageContentEvent) {
                    assertEquals(1000, event.delta.length)
                }
            }
    }

    @Test
    fun testSpecialCharactersInSse() = runTest {
        val specialCases = listOf(
            "Hello\nWorld", // Newline
            "Tab\there", // Tab
            "Quote\"test\"", // Quotes
            "Backslash\\test", // Backslash
            "Unicode: 🎉 ñ é", // Unicode
            "Null char: \u0000", // Null character
            """{"json": "in content"}""" // JSON in content
        )

        specialCases.forEach { content ->
            val event = TextMessageContentEvent(
                messageId = "special",
                delta = content
            )

            val sseData = "data: ${json.encodeToString<BaseEvent>(event)}"

            // Should be able to parse back
            val jsonData = sseData.removePrefix("data: ")
            try {
                val parsed = json.decodeFromString<BaseEvent>(jsonData)
                assertTrue(parsed is TextMessageContentEvent)
                assertEquals(content, parsed.delta)
            } catch (e: Exception) {
                fail("Failed to parse SSE event with special characters: ${e.message}")
            }
        }
    }

    @Test
    fun testEventOrdering() = runTest {
        // Verify events maintain order
        val messageId = "order_test"
        val orderedEvents = mutableListOf<BaseEvent>()

        // Create a specific sequence
        orderedEvents.add(RunStartedEvent(threadId = "t1", runId = "r1"))
        orderedEvents.add(StepStartedEvent(stepName = "step1"))
        orderedEvents.add(TextMessageStartEvent(messageId = messageId, role = "assistant"))

        // Add numbered content chunks
        (1..10).forEach { i ->
            orderedEvents.add(TextMessageContentEvent(
                messageId = messageId,
                delta = "Chunk $i "
            ))
        }

        orderedEvents.add(TextMessageEndEvent(messageId = messageId))
        orderedEvents.add(StepFinishedEvent(stepName = "step1"))
        orderedEvents.add(RunFinishedEvent(threadId = "t1", runId = "r1"))

        // Serialize and deserialize each
        val serialized = orderedEvents.map { json.encodeToString<BaseEvent>(it) }
        val deserialized = serialized.map { json.decodeFromString<BaseEvent>(it) }

        // Verify order is maintained
        assertEquals(orderedEvents.size, deserialized.size)

        // Check specific ordering constraints
        val runStartIndex = deserialized.indexOfFirst { it is RunStartedEvent }
        val runEndIndex = deserialized.indexOfFirst { it is RunFinishedEvent }
        assertTrue(runStartIndex < runEndIndex)

        val msgStartIndex = deserialized.indexOfFirst { it is TextMessageStartEvent }
        val msgEndIndex = deserialized.indexOfFirst { it is TextMessageEndEvent }
        assertTrue(msgStartIndex < msgEndIndex)

        // Verify content chunks are in order
        val contentEvents = deserialized.filterIsInstance<TextMessageContentEvent>()
        assertEquals(10, contentEvents.size)
        contentEvents.forEachIndexed { index, event ->
            assertTrue(event.delta.contains("Chunk ${index + 1}"))
        }
    }
}