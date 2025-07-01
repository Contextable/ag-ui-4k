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

import kotlinx.serialization.json.*

/**
 * Interface for handling state changes in the AG-UI client.
 */
interface StateChangeHandler {
    /**
     * Called when the state is replaced with a snapshot.
     */
    suspend fun onStateSnapshot(snapshot: JsonElement) {}

    /**
     * Called when the state is updated with a delta (JSON Patch operations).
     */
    suspend fun onStateDelta(delta: JsonArray) {}

    /**
     * Called when a state update fails.
     */
    suspend fun onStateError(error: Throwable, delta: JsonArray?) {}
}

/**
 * Creates a state change handler using lambda functions.
 */
fun stateHandler(
    onSnapshot: suspend (JsonElement) -> Unit = {},
    onDelta: suspend (JsonArray) -> Unit = {},
    onError: suspend (Throwable, JsonArray?) -> Unit = { _, _ -> }
): StateChangeHandler = object : StateChangeHandler {
    override suspend fun onStateSnapshot(snapshot: JsonElement) = onSnapshot(snapshot)
    override suspend fun onStateDelta(delta: JsonArray) = onDelta(delta)
    override suspend fun onStateError(error: Throwable, delta: JsonArray?) = onError(error, delta)
}

/**
 * A composite state handler that delegates to multiple handlers.
 */
class CompositeStateHandler(
    internal val handlers: List<StateChangeHandler>
) : StateChangeHandler {

    constructor(vararg handlers: StateChangeHandler) : this(handlers.toList())

    override suspend fun onStateSnapshot(snapshot: JsonElement) {
        handlers.forEach { it.onStateSnapshot(snapshot) }
    }

    override suspend fun onStateDelta(delta: JsonArray) {
        handlers.forEach { it.onStateDelta(delta) }
    }

    override suspend fun onStateError(error: Throwable, delta: JsonArray?) {
        handlers.forEach { it.onStateError(error, delta) }
    }
}

/**
 * Extension function to combine state handlers.
 */
operator fun StateChangeHandler.plus(other: StateChangeHandler): StateChangeHandler {
    return when {
        this is CompositeStateHandler && other is CompositeStateHandler ->
            CompositeStateHandler(this.handlers + other.handlers)
        this is CompositeStateHandler ->
            CompositeStateHandler(this.handlers + other)
        other is CompositeStateHandler ->
            CompositeStateHandler(listOf(this) + other.handlers)
        else ->
            CompositeStateHandler(this, other)
    }
}