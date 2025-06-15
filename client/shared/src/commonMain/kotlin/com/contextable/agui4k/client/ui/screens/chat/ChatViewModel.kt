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
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

data class ChatState(
    val activeAgent: AgentConfig? = null,
    val messages: List<DisplayMessage> = emptyList(),
    val isLoading: Boolean = false,
    val isConnected: Boolean = false,
    val error: String? = null
)

data class DisplayMessage(
    val id: String,
    val role: MessageRole,
    val content: String,
    val timestamp: Long = Clock.System.now().toEpochMilliseconds(),
    val isStreaming: Boolean = false
)

enum class MessageRole {
    USER, ASSISTANT, SYSTEM, ERROR
}

class ChatViewModel : ScreenModel {
    private val settings = getPlatformSettings()
    private val agentRepository = AgentRepository.getInstance(settings)
    private val authManager = AuthManager()
    
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
            
            // Add system message
            addDisplayMessage(
                DisplayMessage(
                    id = generateMessageId(),
                    role = MessageRole.SYSTEM,
                    content = "Connected to ${agentConfig.name}"
                )
            )
        } catch (e: Exception) {
            logger.error(e) { "Failed to connect to agent" }
            _state.update { 
                it.copy(
                    isConnected = false, 
                    error = "Failed to connect: ${e.message}"
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
                        content = "Error: ${e.message}"
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
                        content = "Agent error: ${event.message}"
                    )
                )
            }
            
            // Handle other events as needed
            else -> {
                logger.debug { "Received event: $event" }
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
