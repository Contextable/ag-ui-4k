package com.contextable.agui4k.client

import com.contextable.agui4k.core.types.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.delay
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Configuration for event ordering behavior.
 */
data class EventOrderingConfig(
    val maxBufferSize: Int = 1000,
    val maxWaitTimeMs: Long = 5000L,
    val strictOrdering: Boolean = false
)

/**
 * Handles out-of-order events by buffering and reordering them when possible.
 * 
 * This is useful for:
 * - Network conditions that cause events to arrive out of order
 * - Multiple concurrent operations that emit events
 * - Ensuring UI updates happen in logical sequence
 */
class EventOrderingProcessor(
    private val config: EventOrderingConfig = EventOrderingConfig()
) {
    
    /**
     * Processes an event stream, reordering events when possible.
     * 
     * @param events The input event stream
     * @return An ordered event stream
     */
    fun processEvents(events: Flow<BaseEvent>): Flow<BaseEvent> = flow {
        val buffer = mutableMapOf<String, MutableList<BaseEvent>>()
        val expectedSequences = mutableMapOf<String, Long>()
        
        events.collect { event ->
            val groupKey = getEventGroupKey(event)
            val sequence = getEventSequence(event)
            
            if (groupKey == null || sequence == null) {
                // Events without ordering info are emitted immediately
                emit(event)
                return@collect
            }
            
            // Initialize expected sequence if needed
            if (groupKey !in expectedSequences) {
                expectedSequences[groupKey] = sequence
            }
            
            val expectedSeq = expectedSequences[groupKey]!!
            
            when {
                sequence == expectedSeq -> {
                    // Event is in order - emit it and check buffer
                    emit(event)
                    expectedSequences[groupKey] = expectedSeq + 1
                    
                    // Emit any buffered events that are now in order
                    emitBufferedEventsInOrder(groupKey, buffer, expectedSequences) { bufferedEvent ->
                        emit(bufferedEvent)
                    }
                }
                
                sequence > expectedSeq -> {
                    // Event is ahead - buffer it
                    buffer.getOrPut(groupKey) { mutableListOf() }.add(event)
                    
                    // Check buffer size limits
                    if (buffer[groupKey]!!.size > config.maxBufferSize) {
                        logger.warn { "Event buffer for $groupKey exceeded limit, forcing emission" }
                        forceEmitBufferedEvents(groupKey, buffer, expectedSequences) { bufferedEvent ->
                            emit(bufferedEvent)
                        }
                    }
                }
                
                sequence < expectedSeq -> {
                    if (config.strictOrdering) {
                        logger.warn { "Received late event for $groupKey: sequence $sequence (expected $expectedSeq)" }
                        // In strict mode, we might want to emit anyway or handle specially
                        emit(event)
                    } else {
                        // In non-strict mode, emit late events immediately
                        emit(event)
                    }
                }
            }
        }
        
        // Emit any remaining buffered events when stream completes
        buffer.forEach { (groupKey, events) ->
            events.sortedBy { getEventSequence(it) }.forEach { event ->
                emit(event)
            }
        }
    }
    
    /**
     * Gets a grouping key for events that should be ordered relative to each other.
     */
    private fun getEventGroupKey(event: BaseEvent): String? {
        return when (event) {
            is RunStartedEvent -> "run_${event.runId}"
            is RunFinishedEvent -> "run_${event.runId}"
            is RunErrorEvent -> "run_error"
            is TextMessageStartEvent -> "text_${event.messageId}"
            is TextMessageContentEvent -> "text_${event.messageId}"
            is TextMessageEndEvent -> "text_${event.messageId}"
            is ToolCallStartEvent -> "tool_${event.toolCallId}"
            is ToolCallArgsEvent -> "tool_${event.toolCallId}"
            is ToolCallEndEvent -> "tool_${event.toolCallId}"
            is StepStartedEvent -> "step_${event.stepName}"
            is StepFinishedEvent -> "step_${event.stepName}"
            else -> null // Events without clear ordering requirements
        }
    }
    
    /**
     * Gets a sequence number for ordering events within a group.
     * This is a simplified approach - in practice, you might use timestamps,
     * explicit sequence numbers from the protocol, or other ordering mechanisms.
     */
    private fun getEventSequence(event: BaseEvent): Long? {
        return when (event) {
            is RunStartedEvent -> 1L
            is RunFinishedEvent -> 999L
            is RunErrorEvent -> 998L
            is TextMessageStartEvent -> 10L
            is TextMessageContentEvent -> 20L
            is TextMessageEndEvent -> 30L
            is ToolCallStartEvent -> 50L
            is ToolCallArgsEvent -> 60L
            is ToolCallEndEvent -> 70L
            is StepStartedEvent -> 40L
            is StepFinishedEvent -> 80L
            else -> event.timestamp ?: System.currentTimeMillis() // Fallback to timestamp
        }
    }
    
    /**
     * Emits buffered events that are now in correct order.
     */
    private suspend fun emitBufferedEventsInOrder(
        groupKey: String,
        buffer: MutableMap<String, MutableList<BaseEvent>>,
        expectedSequences: MutableMap<String, Long>,
        emitter: suspend (BaseEvent) -> Unit
    ) {
        val bufferedEvents = buffer[groupKey] ?: return
        val expectedSeq = expectedSequences[groupKey] ?: return
        
        // Sort buffered events by sequence
        bufferedEvents.sortBy { getEventSequence(it) }
        
        val iterator = bufferedEvents.iterator()
        while (iterator.hasNext()) {
            val event = iterator.next()
            val sequence = getEventSequence(event)
            
            if (sequence == expectedSequences[groupKey]) {
                emitter(event)
                iterator.remove()
                expectedSequences[groupKey] = (expectedSequences[groupKey] ?: 0) + 1
            } else {
                break // Stop at first gap in sequence
            }
        }
        
        // Clean up empty buffers
        if (bufferedEvents.isEmpty()) {
            buffer.remove(groupKey)
        }
    }
    
    /**
     * Forces emission of all buffered events for a group (when buffer is full).
     */
    private suspend fun forceEmitBufferedEvents(
        groupKey: String,
        buffer: MutableMap<String, MutableList<BaseEvent>>,
        expectedSequences: MutableMap<String, Long>,
        emitter: suspend (BaseEvent) -> Unit
    ) {
        val bufferedEvents = buffer[groupKey] ?: return
        
        // Emit all buffered events in sequence order
        bufferedEvents.sortedBy { getEventSequence(it) }.forEach { event ->
            emitter(event)
        }
        
        // Update expected sequence to highest seen + 1
        val maxSequence = bufferedEvents.maxOfOrNull { getEventSequence(it) ?: 0L } ?: 0L
        expectedSequences[groupKey] = maxSequence + 1
        
        // Clear buffer
        buffer.remove(groupKey)
    }
}

/**
 * A flow transformer that adds event ordering to any event stream.
 */
fun Flow<BaseEvent>.withOrdering(
    config: EventOrderingConfig = EventOrderingConfig()
): Flow<BaseEvent> {
    val processor = EventOrderingProcessor(config)
    return processor.processEvents(this)
}

/**
 * A flow transformer that adds timeout-based event ordering.
 * Events are buffered and reordered, but buffered events are automatically
 * emitted after a timeout to prevent indefinite blocking.
 */
fun Flow<BaseEvent>.withOrderingAndTimeout(
    config: EventOrderingConfig = EventOrderingConfig()
): Flow<BaseEvent> = channelFlow {
    val processor = EventOrderingProcessor(config)
    val orderedEvents = processor.processEvents(this@withOrderingAndTimeout)
    
    // Collect ordered events with timeout handling
    val buffer = mutableListOf<BaseEvent>()
    var lastEventTime = System.currentTimeMillis()
    
    orderedEvents.collect { event ->
        buffer.add(event)
        lastEventTime = System.currentTimeMillis()
        
        // Emit buffered events immediately or after a small delay for batching
        if (buffer.size >= 10) {
            buffer.forEach { send(it) }
            buffer.clear()
        } else {
            // Use a small delay to allow for batching
            delay(10)
            if (System.currentTimeMillis() - lastEventTime >= 10) {
                buffer.forEach { send(it) }
                buffer.clear()
            }
        }
    }
    
    // Emit any remaining buffered events
    buffer.forEach { send(it) }
}