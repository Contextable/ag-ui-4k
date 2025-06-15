package com.contextable.agui4k.client.ui.screens.settings

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.contextable.agui4k.client.data.model.AgentConfig
import com.contextable.agui4k.client.data.repository.AgentRepository
import com.contextable.agui4k.client.data.repository.PreferencesRepository
import com.contextable.agui4k.client.util.getPlatformSettings
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class SettingsState(
    val agents: List<AgentConfig> = emptyList(),
    val activeAgent: AgentConfig? = null,
    val editingAgent: AgentConfig? = null,
    val isDarkMode: Boolean = false,
    val showTimestamps: Boolean = true
)

class SettingsViewModel : ScreenModel {
    private val settings = getPlatformSettings()
    private val agentRepository = AgentRepository(settings)
    private val preferencesRepository = PreferencesRepository(settings)
    
    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()
    
    init {
        screenModelScope.launch {
            // Combine all flows
            combine(
                agentRepository.agents,
                agentRepository.activeAgent,
                preferencesRepository.isDarkModeFlow,
                preferencesRepository.fontSizeFlow
            ) { agents, activeAgent, isDarkMode, _ ->
                SettingsState(
                    agents = agents,
                    activeAgent = activeAgent,
                    isDarkMode = isDarkMode,
                    showTimestamps = preferencesRepository.showTimestamps
                )
            }.collect { newState ->
                _state.value = newState
            }
        }
    }
    
    fun addAgent(config: AgentConfig) {
        screenModelScope.launch {
            agentRepository.addAgent(config)
        }
    }
    
    fun updateAgent(config: AgentConfig) {
        screenModelScope.launch {
            agentRepository.updateAgent(config)
            _state.update { it.copy(editingAgent = null) }
        }
    }
    
    fun deleteAgent(agentId: String) {
        screenModelScope.launch {
            agentRepository.deleteAgent(agentId)
        }
    }
    
    fun setActiveAgent(agent: AgentConfig) {
        screenModelScope.launch {
            agentRepository.setActiveAgent(agent)
        }
    }
    
    fun editAgent(agent: AgentConfig) {
        _state.update { it.copy(editingAgent = agent) }
    }
    
    fun cancelEdit() {
        _state.update { it.copy(editingAgent = null) }
    }
    
    fun toggleDarkMode() {
        preferencesRepository.isDarkMode = !preferencesRepository.isDarkMode
    }
    
    fun toggleTimestamps() {
        preferencesRepository.showTimestamps = !preferencesRepository.showTimestamps
        _state.update { it.copy(showTimestamps = preferencesRepository.showTimestamps) }
    }
}