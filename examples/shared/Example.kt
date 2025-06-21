package com.contextable.agui4k.examples

import com.contextable.agui4k.client.HttpAgent
import com.contextable.agui4k.client.HttpAgentConfig
import com.contextable.agui4k.core.agent.AgentConfig
import com.contextable.agui4k.core.types.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.*

/**
 * Example demonstrating how to use the ag-ui-4k client library.
 */
@OptIn(ExperimentalCoroutinesApi::class, ExperimentalSerializationApi::class)
suspend fun main() {
    // Create an HTTP client to connect to an AG-UI agent
    val client = HttpAgent(
        HttpAgentConfig(
            url = "https://your-agent-endpoint.com/agent",
            headers = mapOf(
                "Authorization" to "Bearer your-api-key"
            )
        )
    )
    
    // Add initial messages to send to the agent
    client.addMessage(
        UserMessage(
            id = "msg_1",
            content = "Hello! Can you help me with a task?"
        )
    )
    
    // Define tools that the agent can use
    val tools = listOf(
        Tool(
            name = "confirmAction",
            description = "Ask the user to confirm an action before proceeding",
            parameters = buildJsonObject {
                put("type", "object")
                putJsonObject("properties") {
                    putJsonObject("action") {
                        put("type", "string")
                        put("description", "The action that needs confirmation")
                    }
                    putJsonObject("importance") {
                        put("type", "string")
                        putJsonArray("enum") {
                            add("low")
                            add("medium")
                            add("high")
                            add("critical")
                        }
                        put("description", "The importance level of the action")
                    }
                }
                putJsonArray("required") {
                    add("action")
                }
            }
        ),
        Tool(
            name = "fetchUserData",
            description = "Retrieve data about a specific user",
            parameters = buildJsonObject {
                put("type", "object")
                putJsonObject("properties") {
                    putJsonObject("userId") {
                        put("type", "string")
                        put("description", "ID of the user")
                    }
                    putJsonObject("fields") {
                        put("type", "array")
                        putJsonObject("items") {
                            put("type", "string")
                        }
                        put("description", "Fields to retrieve")
                    }
                }
                putJsonArray("required") {
                    add("userId")
                }
            }
        )
    )
    
    // Create context
    val context = listOf(
        Context(
            description = "Current user preferences",
            value = """{"theme": "dark", "language": "en"}"""
        )
    )
    
    // Connect to the agent
    val scope = CoroutineScope(Dispatchers.Default)
    
    scope.launch {
        try {
            client.runAgent(
                RunAgentParameters(
                    tools = tools,
                    context = context
                )
            ).collect { event ->
                handleEvent(event)
            }
        } catch (e: Exception) {
            println("Error: ${e.message}")
        }
    }
    
    // Keep the program running for demo purposes
    delay(60000) // 1 minute
    
    // Cancel the connection
    client.abortRun()
}

/**
 * Handles different types of events received from the agent.
 */
private suspend fun handleEvent(event: BaseEvent) {
    when (event) {
        is RunStartedEvent -> {
            println("🚀 Agent connection started (ID: ${event.runId})")
        }
        
        is RunFinishedEvent -> {
            println("✅ Agent connection finished")
        }
        
        is RunErrorEvent -> {
            println("❌ Error: ${event.message}")
        }
        
        is TextMessageStartEvent -> {
            print("\n🤖 Assistant: ")
        }
        
        is TextMessageContentEvent -> {
            print(event.delta)
        }
        
        is TextMessageEndEvent -> {
            println("\n")
        }
        
        is ToolCallStartEvent -> {
            println("🔧 Tool call: ${event.toolCallName} (ID: ${event.toolCallId})")
        }
        
        is ToolCallArgsEvent -> {
            // Accumulate tool arguments
            print("   Args: ${event.delta}")
        }
        
        is ToolCallEndEvent -> {
            println("\n   Tool call completed")
            
            // In a real application, you would execute the tool here
            // and send the result back to the agent
            simulateToolExecution(event.toolCallId)
        }
        
        is StateSnapshotEvent -> {
            println("📊 State updated: ${event.snapshot}")
        }
        
        is StateDeltaEvent -> {
            println("📝 State delta: ${event.delta}")
        }
        
        is MessagesSnapshotEvent -> {
            println("💬 Messages snapshot received (${event.messages.size} messages)")
        }
        
        is StepStartedEvent -> {
            println("📍 Step started: ${event.stepName}")
        }
        
        is StepFinishedEvent -> {
            println("📍 Step finished: ${event.stepName}")
        }
        
        else -> {
            println("❓ Unknown event: $event")
        }
    }
}

/**
 * Simulates tool execution for demo purposes.
 */
private suspend fun simulateToolExecution(toolCallId: String) {
    // In a real application, you would:
    // 1. Parse the tool call arguments
    // 2. Execute the tool (show UI, call API, etc.)
    // 3. Send the result back to the agent as a ToolMessage
    
    println("   [Simulating tool execution...]")
    delay(1000)
    println("   [Tool execution completed - in real usage, send result back to agent]")
}

/**
 * Example of creating a custom client implementation.
 */
@OptIn(ExperimentalCoroutinesApi::class, ExperimentalSerializationApi::class)
class CustomClient(config: AgentConfig) : com.contextable.agui4k.core.agent.AbstractAgent(config) {
    
    override suspend fun run(input: RunAgentInput): Flow<BaseEvent> = flow {
        // Emit run started event
        emit(RunStartedEvent(
            threadId = input.threadId,
            runId = input.runId
        ))
        
        // Simulate receiving a response from an agent
        val messageId = "msg_${System.currentTimeMillis()}"
        
        emit(TextMessageStartEvent(
            messageId = messageId,
            role = "assistant"
        ))
        
        val response = "This is a custom client implementation!"
        response.chunked(5).forEach { chunk ->
            emit(TextMessageContentEvent(
                messageId = messageId,
                delta = chunk
            ))
            delay(100) // Simulate streaming
        }
        
        emit(TextMessageEndEvent(
            messageId = messageId
        ))
        
        // Emit run finished event
        emit(RunFinishedEvent(
            threadId = input.threadId,
            runId = input.runId
        ))
    }
}