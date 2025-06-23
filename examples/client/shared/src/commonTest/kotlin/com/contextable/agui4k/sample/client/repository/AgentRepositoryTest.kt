package com.contextable.agui4k.sample.client.repository

import com.contextable.agui4k.example.client.data.model.AgentConfig
import com.contextable.agui4k.example.client.data.model.AuthMethod
import com.contextable.agui4k.example.client.data.repository.AgentRepository
import com.contextable.agui4k.sample.client.test.TestSettings
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class AgentRepositoryTest {
    
    private lateinit var settings: TestSettings
    private lateinit var repository: AgentRepository

    @BeforeTest
    fun setup() {
        // Reset the singleton instance to ensure clean state
        AgentRepository.resetInstance()

        settings = TestSettings()
        repository = AgentRepository.getInstance(settings)
    }

    @AfterTest
    fun tearDown() {
        // Clean up after each test
        AgentRepository.resetInstance()
    }
    
    @Test
    fun testAddAgent() = runTest {
        val agent = AgentConfig(
            id = "test-1",
            name = "Test Agent",
            url = "https://test.com/agent",
            authMethod = AuthMethod.None()
        )
        
        repository.addAgent(agent)
        
        val agents = repository.agents.value
        assertEquals(1, agents.size)
        assertEquals(agent, agents.first())
    }
    
    @Test
    fun testUpdateAgent() = runTest {
        val agent = AgentConfig(
            id = "test-1",
            name = "Test Agent",
            url = "https://test.com/agent"
        )
        
        repository.addAgent(agent)
        
        val updatedAgent = agent.copy(name = "Updated Agent")
        repository.updateAgent(updatedAgent)
        
        val agents = repository.agents.value
        assertEquals(1, agents.size)
        assertEquals("Updated Agent", agents.first().name)
    }
    
    @Test
    fun testDeleteAgent() = runTest {
        val agent1 = AgentConfig(id = "1", name = "Agent 1", url = "https://test1.com")
        val agent2 = AgentConfig(id = "2", name = "Agent 2", url = "https://test2.com")
        
        repository.addAgent(agent1)
        repository.addAgent(agent2)
        
        assertEquals(2, repository.agents.value.size)
        
        repository.deleteAgent("1")
        
        val agents = repository.agents.value
        assertEquals(1, agents.size)
        assertEquals("2", agents.first().id)
    }
    
    @Test
    fun testSetActiveAgent() = runTest {
        val agent = AgentConfig(
            id = "test-1",
            name = "Test Agent",
            url = "https://test.com/agent"
        )
        
        repository.addAgent(agent)
        repository.setActiveAgent(agent)
        
        assertEquals(agent.id, repository.activeAgent.value?.id)
        assertNotNull(repository.currentSession.value)
    }
}
