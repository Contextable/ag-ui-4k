package com.contextable.agui4k.tests

import com.contextable.agui4k.core.serialization.AgUiJson
import com.contextable.agui4k.core.types.*
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.json.*
import kotlin.test.*
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class ConversationTest {

    private val json = AgUiJson

    @Test
    fun testThreadCreation() {
        val thread = Thread(
            threadId = "thread_123",
            runs = emptyList(),
            metadata = buildJsonObject {
                put("source", "test")
            }
        )

        assertEquals("thread_123", thread.threadId)
        assertTrue(thread.runs.isEmpty())
        assertNotNull(thread.metadata)
        assertEquals("test", thread.metadata?.jsonObject?.get("source")?.jsonPrimitive?.content)
    }

    @Test
    fun testThreadGetAllMessages() {
        val messages1 = listOf(
            UserMessage(id = "1", content = "Hello"),
            AssistantMessage(id = "2", content = "Hi there")
        )
        val messages2 = listOf(
            UserMessage(id = "3", content = "How are you?"),
            AssistantMessage(id = "4", content = "I'm doing well")
        )

        val run1 = Run(
            runId = "run_1",
            threadId = "thread_1",
            messages = messages1,
            status = RunStatus.COMPLETED,
            startTime = Clock.System.now()
        )

        val run2 = Run(
            runId = "run_2",
            threadId = "thread_1",
            messages = messages2,
            status = RunStatus.COMPLETED,
            startTime = Clock.System.now()
        )

        val thread = Thread(
            threadId = "thread_1",
            runs = listOf(run1, run2)
        )

        val allMessages = thread.getAllMessages()
        assertEquals(4, allMessages.size)
        assertEquals("1", allMessages[0].id)
        assertEquals("2", allMessages[1].id)
        assertEquals("3", allMessages[2].id)
        assertEquals("4", allMessages[3].id)
    }

    @Test
    fun testThreadGetLatestRun() {
        val earlierTime = Clock.System.now() - 10.minutes
        val laterTime = Clock.System.now() - 5.minutes

        val run1 = Run(
            runId = "run_1",
            threadId = "thread_1",
            status = RunStatus.COMPLETED,
            startTime = earlierTime
        )

        val run2 = Run(
            runId = "run_2",
            threadId = "thread_1",
            status = RunStatus.COMPLETED,
            startTime = laterTime
        )

        val thread = Thread(
            threadId = "thread_1",
            runs = listOf(run1, run2)
        )

        val latest = thread.getLatestRun()
        assertNotNull(latest)
        assertEquals("run_2", latest.runId)
    }

    @Test
    fun testThreadGetLatestRunMessages() {
        val messages = listOf(
            UserMessage(id = "1", content = "Latest message")
        )

        val run = Run(
            runId = "run_1",
            threadId = "thread_1",
            messages = messages,
            status = RunStatus.COMPLETED,
            startTime = Clock.System.now()
        )

        val thread = Thread(
            threadId = "thread_1",
            runs = listOf(run)
        )

        val latestMessages = thread.getLatestRunMessages()
        assertEquals(1, latestMessages.size)
        assertEquals("Latest message", latestMessages[0].content)
    }

    @Test
    fun testRunCreation() {
        val startTime = Clock.System.now()
        val messages = listOf(
            UserMessage(id = "1", content = "Test")
        )

        val run = Run(
            runId = "run_123",
            threadId = "thread_456",
            messages = messages,
            status = RunStatus.STARTED,
            startTime = startTime
        )

        assertEquals("run_123", run.runId)
        assertEquals("thread_456", run.threadId)
        assertEquals(1, run.messages.size)
        assertEquals(RunStatus.STARTED, run.status)
        assertEquals(startTime, run.startTime)
        assertNull(run.endTime)
        assertNull(run.error)
        assertNull(run.duration)
        assertFalse(run.isSuccessful)
    }

    @Test
    fun testRunCompletion() {
        val startTime = Clock.System.now() - 30.seconds
        val endTime = Clock.System.now()

        val run = Run(
            runId = "run_123",
            threadId = "thread_456",
            status = RunStatus.COMPLETED,
            startTime = startTime,
            endTime = endTime
        )

        assertTrue(run.isSuccessful)
        assertNotNull(run.duration)
        assertEquals(endTime - startTime, run.duration)
    }

    @Test
    fun testRunError() {
        val error = RunError(
            message = "Something went wrong",
            code = "ERR_001"
        )

        val run = Run(
            runId = "run_123",
            threadId = "thread_456",
            status = RunStatus.ERROR,
            startTime = Clock.System.now(),
            endTime = Clock.System.now(),
            error = error
        )

        assertFalse(run.isSuccessful)
        assertNotNull(run.error)
        assertEquals("Something went wrong", run.error?.message)
        assertEquals("ERR_001", run.error?.code)
    }

    @Test
    fun testRunStatusEnum() {
        assertEquals(3, RunStatus.entries.size)
        assertTrue(RunStatus.STARTED in RunStatus.entries)
        assertTrue(RunStatus.COMPLETED in RunStatus.entries)
        assertTrue(RunStatus.ERROR in RunStatus.entries)
    }

    @Test
    fun testRunErrorSerialization() {
        val error = RunError(
            message = "Test error",
            code = "TEST_001",
            timestamp = Instant.fromEpochMilliseconds(1234567890000)
        )

        val jsonString = json.encodeToString(RunError.serializer(), error)
        val decoded = json.decodeFromString(RunError.serializer(), jsonString)

        assertEquals(error.message, decoded.message)
        assertEquals(error.code, decoded.code)
        assertEquals(error.timestamp, decoded.timestamp)
    }

    @Test
    fun testAgentStateCreation() {
        val globalState = buildJsonObject {
            put("version", "1.0")
            put("feature_flags", buildJsonObject {
                put("new_ui", true)
            })
        }

        val thread = Thread(
            threadId = "thread_1",
            runs = emptyList()
        )

        val agentState = AgentState(
            globalState = globalState,
            threads = mapOf("thread_1" to thread),
            currentThreadId = "thread_1"
        )

        assertNotNull(agentState.globalState)
        assertEquals(1, agentState.threads.size)
        assertEquals("thread_1", agentState.currentThreadId)

        val currentThread = agentState.getCurrentThread()
        assertNotNull(currentThread)
        assertEquals("thread_1", currentThread.threadId)
    }

    @Test
    fun testAgentStateGetAllMessages() {
        val messages1 = listOf(UserMessage(id = "1", content = "Thread 1"))
        val messages2 = listOf(UserMessage(id = "2", content = "Thread 2"))

        val run1 = Run(
            runId = "run_1",
            threadId = "thread_1",
            messages = messages1,
            status = RunStatus.COMPLETED,
            startTime = Clock.System.now()
        )

        val run2 = Run(
            runId = "run_2",
            threadId = "thread_2",
            messages = messages2,
            status = RunStatus.COMPLETED,
            startTime = Clock.System.now()
        )

        val thread1 = Thread(threadId = "thread_1", runs = listOf(run1))
        val thread2 = Thread(threadId = "thread_2", runs = listOf(run2))

        val agentState = AgentState(
            threads = mapOf(
                "thread_1" to thread1,
                "thread_2" to thread2
            )
        )

        val allMessages = agentState.getAllMessages()
        assertEquals(2, allMessages.size)
    }

    @Test
    fun testRunContextCreation() {
        val context = RunContext(
            threadId = "thread_123",
            runId = "run_456"
        )

        assertEquals("thread_123", context.threadId)
        assertEquals("run_456", context.runId)
        assertTrue(context.messages.isEmpty())
        assertEquals(RunStatus.STARTED, context.status)
        assertNull(context.error)
        assertNotNull(context.startTime)
    }

    @Test
    fun testRunContextToRun() {
        val context = RunContext(
            threadId = "thread_123",
            runId = "run_456"
        )

        // Add some messages
        context.messages.add(UserMessage(id = "1", content = "Hello"))
        context.messages.add(AssistantMessage(id = "2", content = "Hi"))

        // Complete the run
        context.status = RunStatus.COMPLETED

        val run = context.toRun()

        assertEquals("thread_123", run.threadId)
        assertEquals("run_456", run.runId)
        assertEquals(2, run.messages.size)
        assertEquals(RunStatus.COMPLETED, run.status)
        assertEquals(context.startTime, run.startTime)
        assertNotNull(run.endTime) // Should be set when status is COMPLETED
    }

    @Test
    fun testRunContextErrorHandling() {
        val context = RunContext(
            threadId = "thread_123",
            runId = "run_456"
        )

        val error = RunError(
            message = "Failed to process",
            code = "PROC_ERR"
        )

        context.status = RunStatus.ERROR
        context.error = error

        val run = context.toRun()

        assertEquals(RunStatus.ERROR, run.status)
        assertNotNull(run.error)
        assertEquals("Failed to process", run.error?.message)
        assertEquals("PROC_ERR", run.error?.code)
        assertNotNull(run.endTime)
    }

    @Test
    fun testThreadSerialization() {
        val thread = Thread(
            threadId = "thread_test",
            runs = listOf(
                Run(
                    runId = "run_1",
                    threadId = "thread_test",
                    status = RunStatus.COMPLETED,
                    startTime = Clock.System.now()
                )
            ),
            metadata = buildJsonObject {
                put("test", true)
            }
        )

        val jsonString = json.encodeToString(Thread.serializer(), thread)
        val decoded = json.decodeFromString(Thread.serializer(), jsonString)

        assertEquals(thread.threadId, decoded.threadId)
        assertEquals(thread.runs.size, decoded.runs.size)
        assertEquals(thread.runs[0].runId, decoded.runs[0].runId)
        assertNotNull(decoded.metadata)
    }

    @Test
    fun testAgentStateSerialization() {
        val agentState = AgentState(
            globalState = buildJsonObject {
                put("initialized", true)
            },
            threads = mapOf(
                "thread_1" to Thread(threadId = "thread_1")
            ),
            currentThreadId = "thread_1",
            metadata = buildJsonObject {
                put("version", "1.0")
            }
        )

        val jsonString = json.encodeToString(AgentState.serializer(), agentState)
        val decoded = json.decodeFromString(AgentState.serializer(), jsonString)

        assertNotNull(decoded.globalState)
        assertEquals(1, decoded.threads.size)
        assertEquals("thread_1", decoded.currentThreadId)
        assertNotNull(decoded.metadata)
    }

    @Test
    fun testEmptyThreadOperations() {
        val thread = Thread(threadId = "empty_thread")

        assertTrue(thread.runs.isEmpty())
        assertTrue(thread.getAllMessages().isEmpty())
        assertNull(thread.getLatestRun())
        assertTrue(thread.getLatestRunMessages().isEmpty())
    }

    @Test
    fun testNullMetadataHandling() {
        val thread = Thread(threadId = "thread_1", metadata = null)
        val run = Run(
            runId = "run_1",
            threadId = "thread_1",
            status = RunStatus.STARTED,
            startTime = Clock.System.now(),
            metadata = null
        )
        val agentState = AgentState(metadata = null)

        assertNull(thread.metadata)
        assertNull(run.metadata)
        assertNull(agentState.metadata)
    }
}