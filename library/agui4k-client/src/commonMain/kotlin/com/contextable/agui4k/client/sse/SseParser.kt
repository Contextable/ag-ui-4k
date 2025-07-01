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
package com.contextable.agui4k.client.sse

import com.contextable.agui4k.core.types.BaseEvent
import com.contextable.agui4k.core.types.AgUiJson
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.Json
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Parses a stream of SSE data into AG-UI events.
 * Each chunk received is already a complete JSON event from the SSE client.
 */
class SseParser(
    private val json: Json = AgUiJson
) {
    /**
     * Transform raw JSON strings into parsed events
     */
    fun parseFlow(source: Flow<String>): Flow<BaseEvent> = source.mapNotNull { jsonStr ->
        try {
            val event = json.decodeFromString<BaseEvent>(jsonStr.trim())
            logger.debug { "Successfully parsed event: ${event.eventType}" }
            event
        } catch (e: Exception) {
            logger.error(e) { "Failed to parse JSON event: $jsonStr" }
            null
        }
    }
}