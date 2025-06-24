import com.contextable.agui4k.client.AgentClient
import com.contextable.agui4k.core.types.*
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

// File: agui4k-client/src/commonTest/kotlin/com/contextable/agui4k/client/AgentClientIntegrationTest.kt

class AgentClientIntegrationTest {

    @Test
    fun testAgentClientCreation() = runTest {
        val client = AgentClient("https://example.com/agent")
        assertNotNull(client)
    }

    @Test
    fun testAgentClientWithBearerToken() = runTest {
        val client = AgentClient("https://example.com/agent") {
            bearerToken = "test-token"
        }
        assertNotNull(client)
    }

    @Test
    fun testAgentClientWithSystemPrompt() = runTest {
        val client = AgentClient("https://example.com/agent") {
            systemPrompt = "You are a helpful assistant"
            maintainHistory = true
        }
        assertNotNull(client)
    }

    @Test
    fun testRunAgentInputCreation() = runTest {
        val client = AgentClient("https://example.com/agent")
        
        val input = RunAgentInput(
            threadId = "test-thread",
            runId = "test-run",
            messages = listOf(
                UserMessage(
                    id = "msg-1",
                    content = "Hello"
                )
            )
        )
        
        assertEquals("test-thread", input.threadId)
        assertEquals("test-run", input.runId)
        assertEquals(1, input.messages.size)
        assertEquals("Hello", (input.messages[0] as UserMessage).content)
    }
}