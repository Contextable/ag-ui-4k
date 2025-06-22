package com.contextable.agui4k.sample.client.ui.screens.chat

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.contextable.agui4k.client.StatefulClient
import com.contextable.agui4k.transport.HttpClientTransport
import com.contextable.agui4k.tools.DefaultToolRegistry
import com.contextable.agui4k.tools.builtin.ConfirmationToolExecutor
import com.contextable.agui4k.tools.builtin.ConfirmationHandler
import com.contextable.agui4k.tools.builtin.ConfirmationRequest
import com.contextable.agui4k.transport.HttpClientTransportConfig
import com.contextable.agui4k.sample.client.data.auth.AuthManager
import com.contextable.agui4k.sample.client.data.model.AgentConfig
import com.contextable.agui4k.sample.client.data.repository.AgentRepository
import com.contextable.agui4k.core.types.*
import com.contextable.agui4k.sample.client.util.getPlatformSettings
import com.contextable.agui4k.sample.client.util.Strings
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.serialization.json.*
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

data class ChatState(
    val activeAgent: AgentConfig? = null,
    val messages: List<DisplayMessage> = emptyList(),
    val ephemeralMessage: DisplayMessage? = null,
    val isLoading: Boolean = false,
    val isConnected: Boolean = false,
    val error: String? = null,
    val pendingConfirmation: UserConfirmationRequest? = null
)

data class DisplayMessage(
    val id: String,
    val role: MessageRole,
    val content: String,
    val timestamp: Long = Clock.System.now().toEpochMilliseconds(),
    val isStreaming: Boolean = false,
    val ephemeralGroupId: String? = null,
    val ephemeralType: EphemeralType? = null
)

data class UserConfirmationRequest(
    val toolCallId: String,
    val action: String,
    val impact: String,
    val details: Map<String, String> = emptyMap(),
    val timeout: Int = 30
)

enum class MessageRole {
    USER, ASSISTANT, SYSTEM, ERROR, TOOL_CALL, STEP_INFO
}

enum class EphemeralType {
    TOOL_CALL, STEP
}

class ChatViewModel : ScreenModel {
    private val settings = getPlatformSettings()
    private val agentRepository = AgentRepository.getInstance(settings)
    private val authManager = AuthManager()

    // Track ephemeral messages by type
    private val ephemeralMessageIds = mutableMapOf<EphemeralType, String>()
    private val toolCallBuffer = mutableMapOf<String, StringBuilder>()
    private val pendingToolCalls = mutableMapOf<String, String>() // toolCallId -> toolName

    private val _state = MutableStateFlow(ChatState())
    val state: StateFlow<ChatState> = _state.asStateFlow()

    private var currentClient: StatefulClient? = null
    private var currentJob: Job? = null
    private var currentThreadId: String? = null
    private val streamingMessages = mutableMapOf<String, StringBuilder>()

    init {
        screenModelScope.launch {
            // Observe active agent changes
            agentRepository.activeAgent.collect { agent ->
                _state.update { it.copy(activeAgent = agent) }
                if (agent != null) {
                    connectToAgent(agent)
                } else {
                    disconnectFromAgent()
                }
            }
        }
    }

    private fun setEphemeralMessage(content: String, type: EphemeralType, icon: String = "") {
        _state.update { state ->
            // Remove the old ephemeral message of this type if it exists
            val oldId = ephemeralMessageIds[type]
            val filtered = if (oldId != null) {
                state.messages.filter { it.id != oldId }
            } else {
                state.messages
            }

            // Create new message with icon
            val newMessage = DisplayMessage(
                id = generateMessageId(),
                role = when (type) {
                    EphemeralType.TOOL_CALL -> MessageRole.TOOL_CALL
                    EphemeralType.STEP -> MessageRole.STEP_INFO
                },
                content = "$icon $content".trim(),
                ephemeralGroupId = type.name,
                ephemeralType = type
            )

            // Track the new ID
            ephemeralMessageIds[type] = newMessage.id

            state.copy(messages = filtered + newMessage)
        }
    }

    private fun clearEphemeralMessage(type: EphemeralType) {
        val messageId = ephemeralMessageIds[type]
        if (messageId != null) {
            _state.update { state ->
                state.copy(
                    messages = state.messages.filter { it.id != messageId }
                )
            }
            ephemeralMessageIds.remove(type)
        }
    }

    private suspend fun connectToAgent(agentConfig: AgentConfig) {
        disconnectFromAgent()

        try {
            // Apply authentication
            val headers = agentConfig.customHeaders.toMutableMap()
            authManager.applyAuth(agentConfig.authMethod, headers)

            // Create new HTTP transport
            val transportConfig = HttpClientTransportConfig(
                url = agentConfig.url,
                headers = headers
            )
            val transport = HttpClientTransport(transportConfig)
            
            // Create tool registry with built-in tools
            val toolRegistry = DefaultToolRegistry().apply {
                // Register confirmation tool with a handler that integrates with our UI
                val confirmationHandler = object : ConfirmationHandler {
                    override suspend fun requestConfirmation(request: ConfirmationRequest): Boolean {
                        // The UI will handle showing the dialog - we'll wait for the response
                        // For now, always return false to prevent automatic execution
                        // The actual confirmation is handled via the UI dialog
                        return false
                    }
                }
                registerTool(ConfirmationToolExecutor(confirmationHandler))
            }
            
            // Create new stateful client
            currentClient = StatefulClient(transport, toolRegistry = toolRegistry)
            
            // Generate new thread ID for this session
            currentThreadId = "thread_${Clock.System.now().toEpochMilliseconds()}"

            _state.update { it.copy(isConnected = true, error = null) }

            // Add system message
            addDisplayMessage(
                DisplayMessage(
                    id = generateMessageId(),
                    role = MessageRole.SYSTEM,
                    content = "${Strings.CONNECTED_TO_PREFIX}${agentConfig.name}"
                )
            )
        } catch (e: Exception) {
            logger.error(e) { "Failed to connect to agent" }
            _state.update {
                it.copy(
                    isConnected = false,
                    error = "${Strings.FAILED_TO_CONNECT_PREFIX}${e.message}"
                )
            }
        }
    }

    private fun disconnectFromAgent() {
        currentJob?.cancel()
        currentJob = null
        currentClient = null
        currentThreadId = null
        streamingMessages.clear()
        toolCallBuffer.clear()
        pendingToolCalls.clear()
        ephemeralMessageIds.clear()
        _state.update {
            it.copy(
                isConnected = false,
                messages = emptyList(),
                pendingConfirmation = null
            )
        }
    }

    fun sendMessage(content: String) {
        if (content.isBlank() || currentClient == null) return

        // Add user message to display
        addDisplayMessage(
            DisplayMessage(
                id = generateMessageId(),
                role = MessageRole.USER,
                content = content.trim()
            )
        )

        // Start conversation with stateful client
        startConversation(content.trim())
    }

    private fun startConversation(content: String) {
        currentJob?.cancel()

        currentJob = screenModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            try {
                currentClient?.continueConversation(
                    content = content,
                    threadId = currentThreadId
                )?.collect { event ->
                    handleAgentEvent(event)
                }
            } catch (e: Exception) {
                logger.error(e) { "Error running agent" }
                addDisplayMessage(
                    DisplayMessage(
                        id = generateMessageId(),
                        role = MessageRole.ERROR,
                        content = "${Strings.ERROR_PREFIX}${e.message}"
                    )
                )
            } finally {
                _state.update { it.copy(isLoading = false) }
                // Finalize any streaming messages
                finalizeStreamingMessages()
                // Clear any remaining ephemeral messages
                ephemeralMessageIds.keys.toList().forEach { type ->
                    clearEphemeralMessage(type)
                }
            }
        }
    }

    internal fun handleAgentEvent(event: BaseEvent) {
        logger.debug { "Handling event: ${event::class.simpleName}" }

        when (event) {
            // Tool Call Events
            is ToolCallStartEvent -> {
                logger.debug { "Tool call started: ${event.toolCallName} (${event.toolCallId})" }
                toolCallBuffer[event.toolCallId] = StringBuilder()
                pendingToolCalls[event.toolCallId] = event.toolCallName

                // Only show ephemeral message for non-confirmation tools
                if (event.toolCallName != "user_confirmation") {
                    setEphemeralMessage(
                        "Calling ${event.toolCallName}...",
                        EphemeralType.TOOL_CALL,
                        "🔧"
                    )
                }
            }

            is ToolCallArgsEvent -> {
                toolCallBuffer[event.toolCallId]?.append(event.delta)
                val currentArgs = toolCallBuffer[event.toolCallId]?.toString() ?: ""
                logger.debug { "Tool call args for ${event.toolCallId}: $currentArgs" }

                val toolName = pendingToolCalls[event.toolCallId]
                if (toolName != "user_confirmation") {
                    setEphemeralMessage(
                        "Calling tool with: ${currentArgs.take(50)}${if (currentArgs.length > 50) "..." else ""}",
                        EphemeralType.TOOL_CALL,
                        "🔧"
                    )
                }
            }

            is ToolCallEndEvent -> {
                val toolName = pendingToolCalls[event.toolCallId]
                val args = toolCallBuffer[event.toolCallId]?.toString()

                logger.debug { "Tool call ended: $toolName with args: $args" }

                if (toolName == "user_confirmation" && args != null) {
                    // Parse the confirmation request
                    try {
                        val jsonArgs = Json.parseToJsonElement(args).jsonObject
                        val action = jsonArgs["action"]?.jsonPrimitive?.content ?: "Unknown action"
                        val impact = jsonArgs["impact"]?.jsonPrimitive?.content ?: "medium"
                        val detailsJson = jsonArgs["details"]?.jsonObject ?: emptyMap<String, JsonElement>()
                        val details = detailsJson.mapValues {
                            it.value.jsonPrimitive.content
                        }
                        val timeout = jsonArgs["timeout_seconds"]?.jsonPrimitive?.intOrNull ?: 30

                        logger.debug { "Showing confirmation dialog for: $action (impact: $impact)" }

                        // Show confirmation dialog
                        _state.update {
                            it.copy(
                                pendingConfirmation = UserConfirmationRequest(
                                    toolCallId = event.toolCallId,
                                    action = action,
                                    impact = impact,
                                    details = details,
                                    timeout = timeout
                                )
                            )
                        }
                    } catch (e: Exception) {
                        logger.error(e) { "Failed to parse user confirmation request: $args" }
                    }
                } else {
                    // Clear ephemeral message for other tools after a short delay
                    screenModelScope.launch {
                        delay(1000)
                        clearEphemeralMessage(EphemeralType.TOOL_CALL)
                    }
                }

                toolCallBuffer.remove(event.toolCallId)
                pendingToolCalls.remove(event.toolCallId)
            }

            // Step Events
            is StepStartedEvent -> {
                setEphemeralMessage(
                    event.stepName,
                    EphemeralType.STEP,
                    "●"
                )
            }

            is StepFinishedEvent -> {
                // Clear step message after a short delay
                screenModelScope.launch {
                    delay(500) // Quick flash for steps
                    clearEphemeralMessage(EphemeralType.STEP)
                }
            }

            // Text Message Events
            is TextMessageStartEvent -> {
                streamingMessages[event.messageId] = StringBuilder()
                addDisplayMessage(
                    DisplayMessage(
                        id = event.messageId,
                        role = MessageRole.ASSISTANT,
                        content = "",
                        isStreaming = true
                    )
                )
            }

            is TextMessageContentEvent -> {
                streamingMessages[event.messageId]?.append(event.delta)
                updateStreamingMessage(event.messageId, event.delta)
            }

            is TextMessageEndEvent -> {
                finalizeStreamingMessage(event.messageId)
            }

            is RunErrorEvent -> {
                addDisplayMessage(
                    DisplayMessage(
                        id = generateMessageId(),
                        role = MessageRole.ERROR,
                        content = "${Strings.AGENT_ERROR_PREFIX}${event.message}"
                    )
                )
            }

            is RunFinishedEvent -> {
                // Clear all ephemeral messages when run finishes
                ephemeralMessageIds.keys.toList().forEach { type ->
                    clearEphemeralMessage(type)
                }
            }

            // Skip state events - we don't want to show them
            is StateDeltaEvent, is StateSnapshotEvent -> {
                // Do nothing - no ephemeral messages for state changes
            }

            else -> {
                logger.debug { "Received event: $event" }
            }
        }
    }

    fun confirmAction() {
        val confirmation = _state.value.pendingConfirmation ?: return

        // Send tool response through stateful client
        screenModelScope.launch {
            try {
                currentClient?.sendToolResponse(
                    threadId = currentThreadId ?: return@launch,
                    toolCallId = confirmation.toolCallId,
                    content = """{"status": "confirmed", "user_response": "approved", "timestamp": "${Clock.System.now()}"}"""
                )?.collect { event ->
                    handleAgentEvent(event)
                }
            } catch (e: Exception) {
                logger.error(e) { "Error sending tool response" }
            }
        }

        // Clear the confirmation dialog
        _state.update { it.copy(pendingConfirmation = null) }
    }

    fun rejectAction() {
        val confirmation = _state.value.pendingConfirmation ?: return

        // Send tool response through stateful client
        screenModelScope.launch {
            try {
                currentClient?.sendToolResponse(
                    threadId = currentThreadId ?: return@launch,
                    toolCallId = confirmation.toolCallId,
                    content = """{"status": "rejected", "user_response": "cancelled", "timestamp": "${Clock.System.now()}"}"""
                )?.collect { event ->
                    handleAgentEvent(event)
                }
            } catch (e: Exception) {
                logger.error(e) { "Error sending tool response" }
            }
        }

        // Clear the confirmation dialog
        _state.update { it.copy(pendingConfirmation = null) }
    }

    private fun updateStreamingMessage(messageId: String, delta: String) {
        _state.update { state ->
            state.copy(
                messages = state.messages.map { msg ->
                    if (msg.id == messageId) {
                        msg.copy(content = msg.content + delta)
                    } else {
                        msg
                    }
                }
            )
        }
    }

    private fun finalizeStreamingMessage(messageId: String) {
        _state.update { state ->
            state.copy(
                messages = state.messages.map { msg ->
                    if (msg.id == messageId) {
                        msg.copy(isStreaming = false)
                    } else {
                        msg
                    }
                }
            )
        }
        streamingMessages.remove(messageId)
    }

    private fun finalizeStreamingMessages() {
        streamingMessages.keys.forEach { messageId ->
            finalizeStreamingMessage(messageId)
        }
    }

    private fun addDisplayMessage(message: DisplayMessage) {
        _state.update { state ->
            state.copy(messages = state.messages + message)
        }
    }

    private fun generateMessageId(): String {
        return "msg_${Clock.System.now().toEpochMilliseconds()}"
    }

    fun cancelCurrentOperation() {
        currentJob?.cancel()
        // Note: StatefulClient doesn't have abortRun - we rely on job cancellation
        _state.update { it.copy(isLoading = false) }
        finalizeStreamingMessages()
        // Clear ephemeral messages on cancel
        ephemeralMessageIds.keys.toList().forEach { type ->
            clearEphemeralMessage(type)
        }
    }
}