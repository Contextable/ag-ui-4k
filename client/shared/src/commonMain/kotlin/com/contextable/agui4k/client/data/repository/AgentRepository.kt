package com.contextable.agui4k.client.data.repository

import com.contextable.agui4k.client.data.model.AgentConfig
import com.contextable.agui4k.client.data.model.ChatSession
import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import org.jetbrains.annotations.TestOnly

class AgentRepository private constructor(
    private val settings: Settings
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val _agents = MutableStateFlow<List<AgentConfig>>(emptyList())
    val agents: StateFlow<List<AgentConfig>> = _agents.asStateFlow()

    private val _activeAgent = MutableStateFlow<AgentConfig?>(null)
    val activeAgent: StateFlow<AgentConfig?> = _activeAgent.asStateFlow()

    private val _currentSession = MutableStateFlow<ChatSession?>(null)
    val currentSession: StateFlow<ChatSession?> = _currentSession.asStateFlow()

    init {
        loadAgents()
        loadActiveAgent()
    }

    private fun loadAgents() {
        val agentsJson = settings.getStringOrNull(KEY_AGENTS)
        if (agentsJson != null) {
            try {
                _agents.value = json.decodeFromString<List<AgentConfig>>(agentsJson)
            } catch (e: Exception) {
                // Handle corrupted data
                _agents.value = emptyList()
            }
        }
    }

    private fun loadActiveAgent() {
        val activeAgentId = settings.getStringOrNull(KEY_ACTIVE_AGENT)
        if (activeAgentId != null) {
            _activeAgent.value = _agents.value.find { it.id == activeAgentId }
        }
    }

    suspend fun addAgent(agent: AgentConfig) {
        val updatedAgents = _agents.value + agent
        _agents.value = updatedAgents
        saveAgents()
    }

    suspend fun updateAgent(agent: AgentConfig) {
        val updatedAgents = _agents.value.map {
            if (it.id == agent.id) agent else it
        }
        _agents.value = updatedAgents
        saveAgents()

        // Update active agent if it's the one being updated
        if (_activeAgent.value?.id == agent.id) {
            _activeAgent.value = agent
        }
    }

    suspend fun deleteAgent(agentId: String) {
        val updatedAgents = _agents.value.filter { it.id != agentId }
        _agents.value = updatedAgents
        saveAgents()

        // Clear active agent if it's the one being deleted
        if (_activeAgent.value?.id == agentId) {
            setActiveAgent(null)
        }
    }

    suspend fun setActiveAgent(agent: AgentConfig?) {
        _activeAgent.value = agent

        if (agent != null) {
            settings.putString(KEY_ACTIVE_AGENT, agent.id)

            // Update last used time
            val updatedAgent = agent.copy(
                lastUsedAt = kotlinx.datetime.Clock.System.now()
            )
            updateAgent(updatedAgent)

            // Start new session
            _currentSession.value = ChatSession(
                agentId = agent.id,
                threadId = com.contextable.agui4k.core.agent.AbstractAgent.generateThreadId()
            )
        } else {
            settings.remove(KEY_ACTIVE_AGENT)
            _currentSession.value = null
        }
    }

    suspend fun getAgent(id: String): AgentConfig? {
        return _agents.value.find { it.id == id }
    }

    private suspend fun saveAgents() {
        val agentsJson = json.encodeToString(_agents.value)
        settings.putString(KEY_AGENTS, agentsJson)
    }

    companion object {
        private const val KEY_AGENTS = "agents"
        private const val KEY_ACTIVE_AGENT = "active_agent"

        @Volatile
        private var INSTANCE: AgentRepository? = null

        fun getInstance(settings: Settings): AgentRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AgentRepository(settings).also { INSTANCE = it }
            }
        }

        @TestOnly
        fun resetInstance() {
            synchronized(this) {
                INSTANCE = null
            }
        }
    }
}