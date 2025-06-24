// agui4k-client/src/commonMain/kotlin/com/contextable/agui4k/client/state/StateManager.kt

package com.contextable.agui4k.client.state

import com.contextable.agui4k.core.types.*
import com.reidsync.kxjsonpatch.JsonPatch
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.*
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Manages client-side state with JSON Patch support.
 * Uses kotlin-json-patch (io.github.reidsync:kotlin-json-patch).
 */
class StateManager(
    private val handler: StateChangeHandler? = null,
    initialState: JsonElement = JsonObject(emptyMap())
) {
    private val _currentState = MutableStateFlow(initialState)
    val currentState: StateFlow<JsonElement> = _currentState.asStateFlow()

    /**
     * Processes AG-UI events and updates state.
     */
    suspend fun processEvent(event: BaseEvent) {
        when (event) {
            is StateSnapshotEvent -> applySnapshot(event.snapshot)
            is StateDeltaEvent -> applyDelta(event.delta)
            else -> {} // Other events don't affect state
        }
    }

    private suspend fun applySnapshot(snapshot: JsonElement) {
        logger.debug { "Applying state snapshot" }
        _currentState.value = snapshot
        handler?.onStateSnapshot(snapshot)
    }

    private suspend fun applyDelta(delta: JsonArray) {
        logger.debug { "Applying ${delta.size} state operations" }

        try {
            // Use JsonPatch library
            val newState = JsonPatch.apply(delta, currentState.value)

            _currentState.value = newState
            handler?.onStateDelta(delta)
        } catch (e: Exception) {
            logger.error(e) { "Failed to apply state delta" }
            handler?.onStateError(e, delta)
        }
    }

    /**
     * Gets a value by JSON Pointer path.
     * Note: The 'kotlin-json-patch' library does not provide a public
     * implementation of JSON Pointer, so we've implemented one.
     */
    fun getValue(path: String): JsonElement? {
        return try {
            JsonPointer.evaluate(currentState.value, path)
        } catch (e: Exception) {
            logger.error(e) { "Failed to get value at: $path" }
            null
        }
    }

    /**
     * Gets a typed value by path.
     */
    private inline fun <reified T> getValueAs(path: String): T? {
        val element = getValue(path) ?: return null
        return try {
            Json.decodeFromJsonElement(element) // Assuming you have a Json instance
        } catch (e: Exception) {
            logger.error(e) { "Failed to decode value at: $path" }
            null
        }
    }
}