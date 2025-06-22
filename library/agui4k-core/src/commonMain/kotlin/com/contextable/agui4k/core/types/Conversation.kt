package com.contextable.agui4k.core.types

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.json.JsonElement

/**
 * Represents a conversation thread containing multiple runs.
 * This is the top-level container for all messages in a conversation.
 */
data class Thread(
    val threadId: String,
    val runs: List<Run> = emptyList(),
    val metadata: JsonElement? = null
) {
    /**
     * Gets all messages from all runs in this thread.
     */
    fun getAllMessages(): List<Message> = runs.flatMap { it.messages }

    /**
     * Gets the most recent run in this thread.
     */
    fun getLatestRun(): Run? = runs.maxByOrNull { it.startTime }

    /**
     * Gets all messages from the latest run.
     */
    fun getLatestRunMessages(): List<Message> = getLatestRun()?.messages ?: emptyList()
}

/**
 * Represents a single execution run within a thread.
 * Each run contains messages exchanged during that execution.
 */
data class Run(
    val runId: String,
    val threadId: String,
    val messages: List<Message> = emptyList(),
    val status: RunStatus,
    val startTime: Instant,
    val endTime: Instant? = null,
    val error: RunError? = null,
    val metadata: JsonElement? = null
) {
    /**
     * Duration of the run, or null if not completed.
     */
    val duration: kotlin.time.Duration?
        get() = endTime?.let { it - startTime }

    /**
     * Whether this run completed successfully.
     */
    val isSuccessful: Boolean
        get() = status == RunStatus.COMPLETED && error == null
}

/**
 * Status of a run execution.
 */
enum class RunStatus {
    STARTED,
    COMPLETED,
    ERROR
}

/**
 * Error information for a failed run.
 */
data class RunError(
    val message: String,
    val code: String? = null,
    val timestamp: Instant = Clock.System.now()
)

/**
 * Global agent state separate from conversation state.
 * This includes the global state and all conversation threads.
 */
data class AgentState(
    val globalState: JsonElement? = null,
    val threads: Map<String, Thread> = emptyMap(),
    val currentThreadId: String? = null,
    val metadata: JsonElement? = null
) {
    /**
     * Gets the current thread if one is selected.
     */
    fun getCurrentThread(): Thread? = currentThreadId?.let { threads[it] }

    /**
     * Gets all messages across all threads.
     */
    fun getAllMessages(): List<Message> = threads.values.flatMap { it.getAllMessages() }
}

/**
 * Context for an active run, used during execution.
 */
data class RunContext(
    val threadId: String,
    val runId: String,
    val messages: MutableList<Message> = mutableListOf(),
    val startTime: Instant = Clock.System.now(),
    var status: RunStatus = RunStatus.STARTED,
    var error: RunError? = null
) {
    /**
     * Converts this context to an immutable Run.
     */
    fun toRun(): Run = Run(
        runId = runId,
        threadId = threadId,
        messages = messages.toList(),
        status = status,
        startTime = startTime,
        endTime = if (status != RunStatus.STARTED) Clock.System.now() else null,
        error = error
    )
}