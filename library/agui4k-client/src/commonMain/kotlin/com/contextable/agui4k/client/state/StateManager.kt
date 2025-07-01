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
 * Provides reactive state management with StateFlow and handles both
 * full state snapshots and incremental JSON Patch deltas.
 * 
 * @property handler Optional callback handler for state change notifications
 * @param initialState The initial state as a JsonElement (defaults to empty JsonObject)
 */
class StateManager(
    private val handler: StateChangeHandler? = null,
    initialState: JsonElement = JsonObject(emptyMap())
) {
    private val _currentState = MutableStateFlow(initialState)
    val currentState: StateFlow<JsonElement> = _currentState.asStateFlow()

    /**
     * Processes AG-UI events and updates state.
     * Handles StateSnapshotEvent and StateDeltaEvent to maintain current state.
     * Other event types are ignored as they don't affect state.
     * 
     * @param event The AG-UI event to process
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
     * 
     * @param path JSON Pointer path (e.g., "/user/name" or "/items/0")
     * @return JsonElement? the value at the specified path, or null if not found or on error
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