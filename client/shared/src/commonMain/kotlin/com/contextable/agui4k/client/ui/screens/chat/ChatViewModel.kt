package com.contextable.agui4k.client.ui.screens.chat

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.contextable.agui4k.client.HttpAgent
import com.contextable.agui4k.client.HttpAgentConfig
import com.contextable.agui4k.client.data.auth.AuthManager
import com.contextable.agui4k.client.data.model.AgentConfig
import com.contextable.agui4k.client.data.repository.AgentRepository
import com.contextable.agui4k.core.types.*
import com.contextable.agui4k.client.util.getPlatformSettings
import com.contextable.agui4k.client.util.Strings
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

data class ChatState(
    val activeAgent: AgentConfig? = null,
    val messages: List<DisplayMessage> = emptyList(),
    val ephemeralMessage: DisplayMessage? = null,
    val isLoading: Boolean = false,
    val isConnected: Boolean = false,
    val error: String? = null
)

data class DisplayMessage(
    val id: String,
    val role: MessageRole,
    val content: String,
    val timestamp: Long = Clock.System.now().toEpochMilliseconds(),
    val isStreaming: Boolean = false,
    val ephemeralGroupId: String? = null  // Add this to group replaceable messages
)

enum class MessageRole {
    USER, ASSISTANT, SYSTEM, ERROR, STATE_UPDATE
}

class ChatViewModel : ScreenModel {
    private val settings = getPlatformSettings()
    private val agentRepository = AgentRepository.getInstance(settings)
    private val authManager = AuthManager()
    private var currentEphemeralMessageId: String? = null

    private val _state = MutableStateFlow(ChatState())
    val state: StateFlow<ChatState> = _state.asStateFlow()

    private var currentAgent: HttpAgent? = null
    private var currentJob: Job? = null
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

    private fun setEphemeralMessage(content: String) {
        _state.update { state ->
            // Remove the old ephemeral message if it exists
            val filtered = if (currentEphemeralMessageId != null) {
                state.messages.filter { it.id != currentEphemeralMessageId }
            } else {
                state.messages
            }

            // Create new message
            val newMessage = DisplayMessage(
                id = generateMessageId(),
                role = MessageRole.STATE_UPDATE,
                content = content,
                ephemeralGroupId = "ephemeral"
            )

            // Track the new ID
            currentEphemeralMessageId = newMessage.id

            state.copy(messages = filtered + newMessage)
        }
    }

    private suspend fun connectToAgent(agentConfig: AgentConfig) {
        disconnectFromAgent()

        try {
            // Apply authentication
            val headers = agentConfig.customHeaders.toMutableMap()
            authManager.applyAuth(agentConfig.authMethod, headers)

            // Create new agent client
            currentAgent = HttpAgent(
                HttpAgentConfig(
                    url = agentConfig.url,
                    headers = headers,
                    description = agentConfig.description
                )
            )

            _state.update { it.copy(isConnected = true, error = null) }

            // Add system message - using localized string constants
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
        currentAgent = null
        streamingMessages.clear()
        _state.update {
            it.copy(
                isConnected = false,
                messages = emptyList()
            )
        }
    }

    fun sendMessage(content: String) {
        if (content.isBlank() || currentAgent == null) return

        val userMessage = UserMessage(
            id = generateMessageId(),
            content = content.trim()
        )

        // Add user message to display
        addDisplayMessage(
            DisplayMessage(
                id = userMessage.id,
                role = MessageRole.USER,
                content = userMessage.content
            )
        )

        // Add to agent
        currentAgent?.addMessage(userMessage)

        // Start agent response
        runAgent()
    }

    private fun runAgent() {
        currentJob?.cancel()

        currentJob = screenModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            try {
                currentAgent?.runAgent()?.collect { event ->
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
            }
        }
    }

    private fun handleAgentEvent(event: BaseEvent) {
        when (event) {

            is StateDeltaEvent -> {
                val changes = formatStateDelta(event.delta)
                if (changes != null) {
                    setEphemeralMessage(changes)
                }
            }

            is StateSnapshotEvent -> {
                setEphemeralMessage("Full state synchronized")
            }

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
                // Clear the ephemeral message
                if (currentEphemeralMessageId != null) {
                    _state.update { state ->
                        state.copy(
                            messages = state.messages.filter { it.id != currentEphemeralMessageId }
                        )
                    }
                    currentEphemeralMessageId = null
                }
            }

            else -> {
                logger.debug { "Received event: $event" }
            }
        }
    }

    private fun formatStateDelta(delta: List<JsonPatchOperation>): String? {
        val meaningful = delta.filter { op ->
            // Filter out operations that just clear values
            !(op.op == "replace" && op.value == null)
        }

        if (meaningful.isEmpty()) return null

        return meaningful.joinToString(", ") { op ->
            when (op.op) {
                "add" -> "Added ${op.path}"
                "remove" -> "Removed ${op.path}"
                "replace" -> "Updated ${op.path}"
                "move" -> "Moved from ${op.from} to ${op.path}"
                else -> "${op.op} ${op.path}"
            }
        }
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
        currentAgent?.abortRun()
        _state.update { it.copy(isLoading = false) }
        finalizeStreamingMessages()
    }
}