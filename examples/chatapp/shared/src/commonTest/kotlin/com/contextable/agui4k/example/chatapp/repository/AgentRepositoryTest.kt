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
package com.contextable.agui4k.example.chatapp.repository

import com.contextable.agui4k.example.chatapp.data.model.AgentConfig
import com.contextable.agui4k.example.chatapp.data.model.AuthMethod
import com.contextable.agui4k.example.chatapp.data.repository.AgentRepository
import com.contextable.agui4k.example.chatapp.test.TestSettings
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
