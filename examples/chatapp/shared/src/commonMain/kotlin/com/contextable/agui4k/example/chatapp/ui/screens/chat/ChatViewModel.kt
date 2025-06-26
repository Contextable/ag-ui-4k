package com.contextable.agui4k.example.chatapp.ui.screens.chat

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.contextable.agui4k.sdk.AgUi4KAgent
import com.contextable.agui4k.tools.DefaultToolRegistry
import com.contextable.agui4k.tools.builtin.ConfirmationToolExecutor
import com.contextable.agui4k.tools.builtin.ConfirmationHandler
import com.contextable.agui4k.tools.builtin.ConfirmationRequest
import com.contextable.agui4k.example.chatapp.data.auth.AuthManager
import com.contextable.agui4k.example.chatapp.data.model.AgentConfig
import com.contextable.agui4k.example.chatapp.data.repository.AgentRepository
import com.contextable.agui4k.core.types.*
import com.contextable.agui4k.example.chatapp.util.getPlatformSettings
import com.contextable.agui4k.example.chatapp.util.Strings
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
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

    private var currentClient: AgUi4KAgent? = null
    private var currentJob: Job? = null
    private var currentThreadId: String? = null
    private val streamingMessages = mutableMapOf<String, StringBuilder>()
    private var pendingConfirmationContinuation: CancellableContinuation<Boolean>? = null

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

            // Create confirmation tool with a handler that integrates with our UI
            val confirmationHandler = object : ConfirmationHandler {
                override suspend fun requestConfirmation(request: ConfirmationRequest): Boolean {
                    // Show the confirmation dialog and wait for user response
                    return suspendCancellableCoroutine { continuation ->
                        _state.update {
                            it.copy(
                                pendingConfirmation = UserConfirmationRequest(
                                    toolCallId = request.toolCallId,
                                    action = request.message,
                                    impact = request.importance,
                                    details = mapOf("details" to (request.details ?: "")),
                                    timeout = 30
                                )
                            )
                        }
                        
                        // Store the continuation so we can resume it when user responds
                        pendingConfirmationContinuation = continuation
                    }
                }
            }
            val confirmationTool = ConfirmationToolExecutor(confirmationHandler)
            
            // Create new agent client with the new SDK API
            val toolRegistry = DefaultToolRegistry().apply {
                registerTool(confirmationTool)
            }
            
            currentClient = AgUi4KAgent(url = agentConfig.url) {
                // Add all headers (including auth headers set by AuthManager)
                this.headers.putAll(headers)
                
                // Set tool registry
                this.toolRegistry = toolRegistry
            }
            
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
        
        // Cancel any pending confirmations
        pendingConfirmationContinuation?.cancel()
        pendingConfirmationContinuation = null
        
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
                currentClient?.sendMessage(
                    message = content,
                    threadId = currentThreadId ?: "default"
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

                logger.debug { "Tool call ended: $toolName" }

                // Clear ephemeral message for tools after a short delay
                // (confirmation tools will be handled by the confirmation handler)
                if (toolName != "user_confirmation") {
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

        // Resume the confirmation handler with true (confirmed)
        pendingConfirmationContinuation?.resume(true)
        pendingConfirmationContinuation = null

        // Clear the confirmation dialog
        _state.update { it.copy(pendingConfirmation = null) }
    }

    fun rejectAction() {
        val confirmation = _state.value.pendingConfirmation ?: return

        // Resume the confirmation handler with false (rejected)
        pendingConfirmationContinuation?.resume(false)
        pendingConfirmationContinuation = null

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
        
        // Cancel any pending confirmations
        pendingConfirmationContinuation?.cancel()
        pendingConfirmationContinuation = null
        
        _state.update { it.copy(isLoading = false, pendingConfirmation = null) }
        finalizeStreamingMessages()
        // Clear ephemeral messages on cancel
        ephemeralMessageIds.keys.toList().forEach { type ->
            clearEphemeralMessage(type)
        }
    }
}