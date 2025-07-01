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
package com.contextable.agui4k.tests

import com.contextable.agui4k.core.types.AgUiJson
import com.contextable.agui4k.core.types.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class MessageSerializationTest {

    private val json = AgUiJson

    @Test
    fun testUserMessageSerialization() {
        val message = UserMessage(
            id = "msg_123",
            content = "Hello, world!"
        )

        val jsonString = json.encodeToString<Message>(message)

        // Verify no 'type' field is present
        assertFalse(jsonString.contains("\"type\""))

        // Verify the structure matches AG-UI protocol
        val decoded = json.decodeFromString<Message>(jsonString)
        assertEquals(message, decoded)
    }

    @Test
    fun testAssistantMessageWithToolCalls() {
        val message = AssistantMessage(
            id = "msg_456",
            content = "I'll help you with that",
            toolCalls = listOf(
                ToolCall(
                    id = "call_789",
                    function = FunctionCall(
                        name = "get_weather",
                        arguments = """{"location": "Paris"}"""
                    )
                )
            )
        )

        val jsonString = json.encodeToString<Message>(message)
        val decoded = json.decodeFromString<Message>(jsonString)

        assertEquals(message, decoded)
    }

    @Test
    fun testMessageListSerialization() {
        val messages: List<Message> = listOf(
            UserMessage(id = "1", content = "Hi"),
            AssistantMessage(id = "2", content = "Hello"),
            ToolMessage(id = "3", content = "Result", toolCallId = "call_1")
        )

        val jsonString = json.encodeToString(messages)
        val decoded: List<Message> = json.decodeFromString(jsonString)

        assertEquals(messages, decoded)
    }

    @Test
    fun testRunAgentInputSerialization() {
        val input = RunAgentInput(
            threadId = "thread_123",
            runId = "run_456",
            messages = listOf(
                UserMessage(id = "msg_1", content = "Test")
            ),
            tools = emptyList(),
            context = emptyList()
        )

        val jsonString = json.encodeToString(input)

        // Verify no 'type' field in the nested messages
        assertFalse(jsonString.contains("\"type\":\"user\""))

        val decoded = json.decodeFromString<RunAgentInput>(jsonString)
        assertEquals(input, decoded)
    }
}