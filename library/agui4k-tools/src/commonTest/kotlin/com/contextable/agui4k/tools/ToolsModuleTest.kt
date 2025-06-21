package com.contextable.agui4k.tools

import com.contextable.agui4k.core.types.*
import com.contextable.agui4k.tools.builtin.*
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.*
import kotlin.test.*

/**
 * Tests for the Tools Module.
 */
class ToolsModuleTest {
    
    @Test
    fun testToolRegistry() = runTest {
        val registry = DefaultToolRegistry()
        
        // Test registration
        val echoTool = EchoToolExecutor()
        registry.registerTool(echoTool)
        
        assertTrue(registry.isToolRegistered("echo"))
        assertNotNull(registry.getToolExecutor("echo"))
        
        // Test tool execution
        val toolCall = ToolCall(
            id = "test-1",
            type = "function",
            function = FunctionCall(
                name = "echo",
                arguments = """{"message": "Hello, World!"}"""
            )
        )
        
        val context = ToolExecutionContext(toolCall)
        val result = registry.executeTool(context)
        
        assertTrue(result.success)
        assertNotNull(result.result)
    }
    
    @Test
    fun testEchoTool() = runTest {
        val executor = EchoToolExecutor()
        
        // Test valid execution
        val toolCall = ToolCall(
            id = "test-1",
            type = "function",
            function = FunctionCall(
                name = "echo",
                arguments = """{"message": "test message"}"""
            )
        )
        
        val context = ToolExecutionContext(toolCall)
        val result = executor.execute(context)
        
        assertTrue(result.success)
        assertEquals("Message echoed successfully", result.message)
    }
    
    @Test
    fun testCalculatorTool() = runTest {
        val executor = CalculatorToolExecutor()
        
        // Test addition
        val addCall = ToolCall(
            id = "test-1",
            type = "function",
            function = FunctionCall(
                name = "calculator",
                arguments = """{"operation": "add", "a": 5, "b": 3}"""
            )
        )
        
        val context = ToolExecutionContext(addCall)
        val result = executor.execute(context)
        
        assertTrue(result.success)
        assertNotNull(result.result)
        
        // Test division by zero
        val divideByZeroCall = ToolCall(
            id = "test-2",
            type = "function",
            function = FunctionCall(
                name = "calculator",
                arguments = """{"operation": "divide", "a": 5, "b": 0}"""
            )
        )
        
        val divideContext = ToolExecutionContext(divideByZeroCall)
        val divideResult = executor.execute(divideContext)
        
        assertFalse(divideResult.success)
        assertTrue(divideResult.message?.contains("Cannot divide by zero") == true)
    }
}

/**
 * Simple echo tool for testing.
 */
class EchoToolExecutor : AbstractToolExecutor(
    tool = Tool(
        name = "echo",
        description = "Echoes back the provided message",
        parameters = buildJsonObject {
            put("type", "object")
            putJsonObject("properties") {
                putJsonObject("message") {
                    put("type", "string")
                    put("description", "The message to echo")
                }
            }
            putJsonArray("required") {
                add("message")
            }
        }
    )
) {
    override suspend fun executeInternal(context: ToolExecutionContext): ToolExecutionResult {
        val args = Json.parseToJsonElement(context.toolCall.function.arguments).jsonObject
        val message = args["message"]?.jsonPrimitive?.content
            ?: return ToolExecutionResult.failure("Missing message parameter")
        
        val result = buildJsonObject {
            put("echo", message)
            put("timestamp", kotlinx.datetime.Clock.System.now().toString())
        }
        
        return ToolExecutionResult.success(result, "Message echoed successfully")
    }
    
    override fun validate(toolCall: ToolCall): ToolValidationResult {
        val args = try {
            Json.parseToJsonElement(toolCall.function.arguments).jsonObject
        } catch (e: Exception) {
            return ToolValidationResult.failure("Invalid JSON arguments")
        }
        
        val message = args["message"]?.jsonPrimitive?.content
        if (message.isNullOrBlank()) {
            return ToolValidationResult.failure("Message parameter is required and cannot be empty")
        }
        
        return ToolValidationResult.success()
    }
}

/**
 * Simple calculator tool for testing.
 */
class CalculatorToolExecutor : AbstractToolExecutor(
    tool = Tool(
        name = "calculator",
        description = "Performs basic arithmetic operations",
        parameters = buildJsonObject {
            put("type", "object")
            putJsonObject("properties") {
                putJsonObject("operation") {
                    put("type", "string")
                    put("enum", buildJsonArray {
                        add("add")
                        add("subtract")
                        add("multiply")
                        add("divide")
                    })
                }
                putJsonObject("a") {
                    put("type", "number")
                    put("description", "First number")
                }
                putJsonObject("b") {
                    put("type", "number")
                    put("description", "Second number")
                }
            }
            putJsonArray("required") {
                add("operation")
                add("a")
                add("b")
            }
        }
    )
) {
    override suspend fun executeInternal(context: ToolExecutionContext): ToolExecutionResult {
        val args = Json.parseToJsonElement(context.toolCall.function.arguments).jsonObject
        
        val operation = args["operation"]?.jsonPrimitive?.content
            ?: return ToolExecutionResult.failure("Missing operation parameter")
        
        val a = args["a"]?.jsonPrimitive?.doubleOrNull
            ?: return ToolExecutionResult.failure("Missing or invalid parameter 'a'")
        
        val b = args["b"]?.jsonPrimitive?.doubleOrNull
            ?: return ToolExecutionResult.failure("Missing or invalid parameter 'b'")
        
        val result = when (operation) {
            "add" -> a + b
            "subtract" -> a - b
            "multiply" -> a * b
            "divide" -> {
                if (b == 0.0) {
                    return ToolExecutionResult.failure("Cannot divide by zero")
                }
                a / b
            }
            else -> return ToolExecutionResult.failure("Invalid operation: $operation")
        }
        
        val resultJson = buildJsonObject {
            put("operation", operation)
            put("a", a)
            put("b", b)
            put("result", result)
        }
        
        return ToolExecutionResult.success(resultJson, "Calculation completed successfully")
    }
}