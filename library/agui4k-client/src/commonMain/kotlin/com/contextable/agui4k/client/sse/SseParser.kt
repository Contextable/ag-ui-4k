// agui4k-client/src/commonMain/kotlin/com/contextable/agui4k/client/sse/SseParser.kt
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