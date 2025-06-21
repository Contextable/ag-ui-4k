package com.contextable.agui4k.client

import com.contextable.agui4k.core.types.*
import kotlinx.coroutines.flow.*

/**
 * Represents a pattern that can match sequences of events.
 */
interface EventPattern {
    /**
     * Checks if this pattern matches the given event in the current context.
     * 
     * @param event The event to check
     * @param context The current pattern matching context
     * @return True if the pattern matches
     */
    fun matches(event: BaseEvent, context: PatternContext): Boolean
    
    /**
     * Updates the pattern context after processing an event.
     * This allows patterns to maintain state across multiple events.
     * 
     * @param event The event that was processed
     * @param context The current pattern context
     * @return Updated pattern context
     */
    fun updateContext(event: BaseEvent, context: PatternContext): PatternContext
}

/**
 * Context for pattern matching that tracks state across events.
 */
data class PatternContext(
    val threadId: String?,
    val runId: String?,
    val eventHistory: List<BaseEvent> = emptyList(),
    val customData: Map<String, Any> = emptyMap()
) {
    /**
     * Adds an event to the history (keeping only recent events for memory efficiency).
     */
    fun addEvent(event: BaseEvent, maxHistorySize: Int = 100): PatternContext {
        val newHistory = (eventHistory + event).takeLast(maxHistorySize)
        return copy(eventHistory = newHistory)
    }
    
    /**
     * Updates custom data in the context.
     */
    fun withData(key: String, value: Any): PatternContext {
        return copy(customData = customData + (key to value))
    }
}

/**
 * A simple pattern that matches events by type.
 */
class EventTypePattern<T : BaseEvent>(
    private val eventClass: Class<T>
) : EventPattern {
    
    override fun matches(event: BaseEvent, context: PatternContext): Boolean {
        return eventClass.isInstance(event)
    }
    
    override fun updateContext(event: BaseEvent, context: PatternContext): PatternContext {
        return context.addEvent(event)
    }
    
    companion object {
        inline fun <reified T : BaseEvent> create(): EventTypePattern<T> {
            return EventTypePattern(T::class.java)
        }
    }
}

/**
 * A pattern that matches a sequence of event types in order.
 */
class EventSequencePattern(
    private val eventTypes: List<Class<out BaseEvent>>
) : EventPattern {
    
    override fun matches(event: BaseEvent, context: PatternContext): Boolean {
        val currentPosition = context.customData["sequence_position"] as? Int ?: 0
        
        if (currentPosition >= eventTypes.size) {
            return false // Sequence already completed
        }
        
        return eventTypes[currentPosition].isInstance(event)
    }
    
    override fun updateContext(event: BaseEvent, context: PatternContext): PatternContext {
        val currentPosition = context.customData["sequence_position"] as? Int ?: 0
        
        return if (matches(event, context)) {
            val newPosition = currentPosition + 1
            context.addEvent(event).withData("sequence_position", newPosition)
        } else {
            // Reset sequence on mismatch
            context.addEvent(event).withData("sequence_position", 0)
        }
    }
    
    /**
     * Checks if the sequence pattern is complete.
     */
    fun isComplete(context: PatternContext): Boolean {
        val position = context.customData["sequence_position"] as? Int ?: 0
        return position >= eventTypes.size
    }
}

/**
 * A pattern that matches when a tool call starts and completes successfully.
 */
class ToolCallCompletionPattern(
    private val toolName: String? = null // null matches any tool
) : EventPattern {
    
    override fun matches(event: BaseEvent, context: PatternContext): Boolean {
        return when (event) {
            is ToolCallStartEvent -> {
                toolName == null || event.toolCallName == toolName
            }
            is ToolCallEndEvent -> {
                val startedToolCallId = context.customData["tool_call_id"] as? String
                startedToolCallId == event.toolCallId
            }
            else -> false
        }
    }
    
    override fun updateContext(event: BaseEvent, context: PatternContext): PatternContext {
        return when (event) {
            is ToolCallStartEvent -> {
                context.addEvent(event)
                    .withData("started_tool_name", event.toolCallName)
                    .withData("tool_call_id", event.toolCallId)
            }
            is ToolCallEndEvent -> {
                val startedToolCallId = context.customData["tool_call_id"] as? String
                if (startedToolCallId == event.toolCallId) {
                    context.addEvent(event).withData("completed", true)
                } else {
                    context.addEvent(event)
                }
            }
            else -> context.addEvent(event)
        }
    }
    
    /**
     * Checks if a tool call has completed successfully.
     */
    fun isCompleted(context: PatternContext): Boolean {
        return context.customData["completed"] as? Boolean == true
    }
}

/**
 * A pattern that matches run lifecycle events (started -> finished or error).
 */
class RunLifecyclePattern : EventPattern {
    
    override fun matches(event: BaseEvent, context: PatternContext): Boolean {
        return when (event) {
            is RunStartedEvent, is RunFinishedEvent, is RunErrorEvent -> true
            else -> false
        }
    }
    
    override fun updateContext(event: BaseEvent, context: PatternContext): PatternContext {
        return when (event) {
            is RunStartedEvent -> {
                context.addEvent(event)
                    .withData("run_started", true)
                    .withData("run_id", event.runId)
            }
            is RunFinishedEvent -> {
                val startedRunId = context.customData["run_id"] as? String
                if (startedRunId == event.runId) {
                    context.addEvent(event).withData("run_completed", true)
                } else {
                    context.addEvent(event)
                }
            }
            is RunErrorEvent -> {
                context.addEvent(event).withData("run_failed", true)
            }
            else -> context.addEvent(event)
        }
    }
    
    fun isRunCompleted(context: PatternContext): Boolean {
        return context.customData["run_completed"] as? Boolean == true
    }
    
    fun isRunFailed(context: PatternContext): Boolean {
        return context.customData["run_failed"] as? Boolean == true
    }
}

/**
 * Extension functions for creating common patterns.
 */
object EventPatterns {
    
    /**
     * Creates a pattern that matches text message start events.
     */
    fun textMessageStart(): EventTypePattern<TextMessageStartEvent> {
        return EventTypePattern.create<TextMessageStartEvent>()
    }
    
    /**
     * Creates a pattern that matches text streaming completion.
     */
    fun textStreamCompleted(): EventSequencePattern {
        return EventSequencePattern(
            listOf(
                TextMessageStartEvent::class.java,
                TextMessageContentEvent::class.java,
                TextMessageEndEvent::class.java
            )
        )
    }
    
    /**
     * Creates a pattern that matches a complete run lifecycle.
     */
    fun runLifecycle(): EventSequencePattern {
        return EventSequencePattern(
            listOf(
                RunStartedEvent::class.java,
                RunFinishedEvent::class.java
            )
        )
    }
    
    /**
     * Creates a pattern that matches any tool call completion.
     */
    fun anyToolCallCompletion(): ToolCallCompletionPattern {
        return ToolCallCompletionPattern()
    }
    
    /**
     * Creates a pattern that matches a specific tool call completion.
     */
    fun toolCallCompletion(toolName: String): ToolCallCompletionPattern {
        return ToolCallCompletionPattern(toolName)
    }
}

/**
 * Event processor that can apply pattern-based filtering and processing.
 */
class PatternAwareEventProcessor(
    private val patterns: List<Pair<EventPattern, suspend (BaseEvent, PatternContext) -> BaseEvent?>>
) {
    
    /**
     * Processes an event stream, applying pattern-based logic.
     */
    fun processEvents(
        events: Flow<BaseEvent>,
        initialContext: PatternContext = PatternContext(null, null)
    ): Flow<BaseEvent> = flow {
        var context = initialContext
        
        events.collect { event ->
            var processedEvent: BaseEvent? = event
            
            // Apply each pattern and its associated processor
            for ((pattern, processor) in patterns) {
                if (pattern.matches(event, context)) {
                    processedEvent = processor(event, context)
                    context = pattern.updateContext(event, context)
                    
                    // If processor filters out the event, stop processing
                    if (processedEvent == null) break
                }
            }
            
            // Update context with the event regardless of pattern matches
            context = context.addEvent(event)
            
            // Emit the processed event if it wasn't filtered out
            if (processedEvent != null) {
                emit(processedEvent)
            }
        }
    }
}