package com.contextable.agui4k.core.agent

import com.contextable.agui4k.core.types.BaseEvent
import com.contextable.agui4k.core.types.Message
import com.contextable.agui4k.core.types.RunAgentInput
import com.contextable.agui4k.core.types.RunAgentParameters
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import mu.KotlinLogging
import kotlin.random.Random

private val logger = KotlinLogging.logger {}
private const val ID_LENGTH = 16

/**
 * Exception thrown when an agent operation fails.
 */
class AgentException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)

/**
 * Configuration options for a client.
 */
open class AgentConfig(
    val agentId: String? = null,  // Unique identifier for the client instance
    val description: String? = null,  // Human-readable description of the client
    val threadId: String? = null,  // Conversation thread identifier
    val initialMessages: List<Message> = emptyList(),  // Initial messages to send to the agent
    val initialState: JsonElement? = null  // Initial client-side state
)

/**
 * Abstract base class for all AG-UI client implementations.
 *
 * This class provides the foundation for:
 * - Managing conversation state with the agent
 * - Tracking message history
 * - Processing event streams from the agent
 * - Providing tools for the agent to use
 *
 * Extend this class and implement the [run] method to create custom client implementations.
 */
@OptIn(ExperimentalCoroutinesApi::class, ExperimentalSerializationApi::class)
abstract class AbstractAgent(config: AgentConfig = AgentConfig()) {

    /**
     * Unique identifier for the client instance.
     */
    val agentId: String = config.agentId ?: generateAgentId()

    /**
     * Human-readable description of the client.
     */
    val description: String? = config.description

    /**
     * Conversation thread identifier for communicating with the agent.
     */
    var threadId: String = config.threadId ?: generateThreadId()
        protected set

    /**
     * Current client-side state as a mutable state flow.
     */
    private val _state = MutableStateFlow(config.initialState ?: JsonNull)

    /**
     * Current client-side state as a read-only state flow.
     */
    val state: StateFlow<JsonElement> = _state.asStateFlow()

    /**
     * Conversation messages to/from the agent as a mutable state flow.
     */
    private val _messages = MutableStateFlow(config.initialMessages)

    /**
     * Conversation messages to/from the agent as a read-only state flow.
     */
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    /**
     * The primary method for connecting to an agent and receiving the event stream.
     *
     * @param parameters Optional parameters for the agent connection
     * @return A Flow of events from the agent
     * @throws CancellationException if the operation is cancelled
     * @throws AgentException if the agent operation fails
     */
    open suspend fun runAgent(
        parameters: RunAgentParameters = RunAgentParameters()
    ): Flow<BaseEvent> {
        val input = prepareRunAgentInput(parameters)

        logger.debug { "Connecting to agent via client $agentId with input: $input" }

        return try {
            val eventFlow = run(input)
            apply(input, eventFlow)
        } catch (e: CancellationException) {
            // Re-throw cancellation exceptions to maintain structured concurrency
            logger.debug { "Agent operation cancelled for client $agentId" }
            throw e
        } catch (e: AgentException) {
            // Re-throw agent exceptions as-is
            logger.error(e) { "Agent error in $agentId: ${e.message}" }
            onError(e)
            throw e
        } catch (e: IllegalArgumentException) {
            // Handle invalid arguments
            handleSpecificError(e, "Invalid argument: ${e.message}")
        } catch (e: IllegalStateException) {
            // Handle invalid state
            handleSpecificError(e, "Invalid state: ${e.message}")
        } catch (e: SerializationException) {
            // Handle serialization errors
            handleSpecificError(e, "Serialization error: ${e.message}")
        } catch (e: NoSuchElementException) {
            // Handle missing elements
            handleSpecificError(e, "Required element not found: ${e.message}")
        } catch (e: UnsupportedOperationException) {
            // Handle unsupported operations
            handleSpecificError(e, "Operation not supported: ${e.message}")
        }
        // Note: We don't catch generic Exception here - let unexpected errors propagate
    }

    /**
     * Handles specific errors that occur during agent operations.
     *
     * @param error The specific error that occurred
     * @param message The error message
     * @throws AgentException Always throws an AgentException wrapping the original error
     */
    private fun handleSpecificError(error: Exception, message: String): Nothing {
        logger.error(error) { "Error in agent $agentId: $message" }
        onError(error)
        throw AgentException(message, error)
    }

    /**
     * Cancels the current connection to the agent.
     * Subclasses should override this to implement cancellation logic.
     */
    open fun abortRun() {
        logger.debug { "Aborting connection for client $agentId" }
    }

    /**
     * Creates a deep copy of the client instance.
     *
     * @return A new client instance with the same configuration and state
     */
    abstract fun clone(): AbstractAgent

    /**
     * Adds a message to the conversation history.
     *
     * @param message The message to add
     */
    fun addMessage(message: Message) {
        _messages.value = _messages.value + message
    }

    /**
     * Updates the client's local state.
     *
     * @param newState The new state value
     */
    protected fun updateState(newState: JsonElement) {
        _state.value = newState
    }

    /**
     * Connects to the agent and returns a flow of events.
     * This is the main method that subclasses must implement.
     *
     * @param input The input parameters for the agent connection
     * @return A Flow of events from the agent
     */
    protected abstract suspend fun run(input: RunAgentInput): Flow<BaseEvent>

    /**
     * Processes events received from the agent and updates the client state.
     *
     * @param input The input parameters for the agent connection
     * @param events The flow of events from the agent
     * @return A processed flow of events
     */
    protected open suspend fun apply(
        input: RunAgentInput,
        events: Flow<BaseEvent>
    ): Flow<BaseEvent> {
        // Default implementation just passes through events
        // Subclasses can override to add event processing logic
        return events
    }

    /**
     * Prepares the input parameters for the agent connection.
     *
     * @param parameters The run parameters from the client
     * @return The prepared RunAgentInput
     */
    protected open fun prepareRunAgentInput(
        parameters: RunAgentParameters
    ): RunAgentInput {
        return RunAgentInput(
            threadId = threadId,
            runId = parameters.runId ?: generateRunId(),
            state = state.value,
            messages = messages.value,
            tools = parameters.tools,
            context = parameters.context,
            forwardedProps = parameters.forwardedProps
        )
    }

    /**
     * Lifecycle hook for error handling.
     *
     * @param error The error that occurred
     */
    protected open fun onError(error: Throwable) {
        logger.error(error) { "Error in client $agentId" }
    }

    /**
     * Lifecycle hook for cleanup operations.
     */
    protected open fun onFinalize() {
        logger.debug { "Finalizing client $agentId" }
    }

    companion object {
        /**
         * Generates a unique agent/client ID.
         */
        fun generateAgentId(): String = "agent_${generateId()}"

        /**
         * Generates a unique thread ID.
         */
        fun generateThreadId(): String = "thread_${generateId()}"

        /**
         * Generates a unique run ID.
         */
        fun generateRunId(): String = "run_${generateId()}"

        /**
         * Generates a random ID string.
         */
        private fun generateId(): String {
            val chars = "abcdefghijklmnopqrstuvwxyz0123456789"
            return (1..ID_LENGTH)
                .map { chars[Random.nextInt(chars.length)] }
                .joinToString("")
        }
    }
}
