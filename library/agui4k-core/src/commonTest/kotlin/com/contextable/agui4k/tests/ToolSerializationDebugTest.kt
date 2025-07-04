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

import com.contextable.agui4k.core.types.*
import kotlinx.serialization.json.*
import kotlinx.serialization.encodeToString
import kotlin.test.Test

class ToolSerializationDebugTest {
    
    @Test
    fun testUserConfirmationToolSerialization() {
        // Create the user_confirmation tool exactly as in theConfirmationToolExecutor
        val userConfirmationTool = Tool(
            name = "user_confirmation",
            description = "Request user confirmation for an action with specified importance level",
            parameters = buildJsonObject {
                put("type", "object")
                putJsonObject("properties") {
                    putJsonObject("message") {
                        put("type", "string")
                        put("description", "The confirmation message to display to the user")
                    }
                    putJsonObject("importance") {
                        put("type", "string")
                        put("enum", buildJsonArray {
                            add("critical")
                            add("high") 
                            add("medium")
                            add("low")
                        })
                        put("description", "The importance level of the confirmation")
                        put("default", "medium")
                    }
                    putJsonObject("details") {
                        put("type", "string")
                        put("description", "Optional additional details about the action requiring confirmation")
                    }
                }
                putJsonArray("required") {
                    add("message")
                }
            }
        )

        // Serialize just the tool
        val toolJson = AgUiJson.encodeToString(userConfirmationTool)
        println("\n=== Tool JSON ===")
        println(toolJson)
        
        // Create a minimal RunAgentInput
        val runInput = RunAgentInput(
            threadId = "thread_1750919849810",
            runId = "run_1750920834023",
            state = JsonObject(emptyMap()),
            messages = listOf(
                UserMessage(
                    id = "usr_1750920834023",
                    content = "delete user data"
                )
            ),
            tools = listOf(userConfirmationTool),
            context = emptyList(),
            forwardedProps = JsonObject(emptyMap())
        )
        
        // Serialize the full input
        val inputJson = AgUiJson.encodeToString(runInput)
        println("\n=== Full RunAgentInput JSON (minified) ===")
        println(inputJson)
        
        // Pretty print for readability
        val prettyJson = Json { 
            prettyPrint = true 
            serializersModule = AgUiJson.serializersModule
            ignoreUnknownKeys = true
            isLenient = true
            encodeDefaults = true
            explicitNulls = false
        }
        println("\n=== Pretty printed RunAgentInput ===")
        println(prettyJson.encodeToString(runInput))
        
        // Extract and show just the tools array
        val parsed = prettyJson.parseToJsonElement(inputJson).jsonObject
        val toolsArray = parsed["tools"]?.jsonArray
        println("\n=== Tools array only ===")
        println(prettyJson.encodeToString(toolsArray))
    }
}