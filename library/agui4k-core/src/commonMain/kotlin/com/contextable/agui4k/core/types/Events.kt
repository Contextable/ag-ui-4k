package com.contextable.agui4k.core.types

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator
import kotlinx.serialization.json.JsonElement

/**
 * Enum defining all possible event types in the AG-UI protocol.
 * Exactly 16 event types as specified in the protocol.
 *
 * Events are grouped by category:
 * - Lifecycle Events (5): RUN_STARTED, RUN_FINISHED, RUN_ERROR, STEP_STARTED, STEP_FINISHED
 * - Text Message Events (3): TEXT_MESSAGE_START, TEXT_MESSAGE_CONTENT, TEXT_MESSAGE_END
 * - Tool Call Events (3): TOOL_CALL_START, TOOL_CALL_ARGS, TOOL_CALL_END
 * - State Management Events (3): STATE_SNAPSHOT, STATE_DELTA, MESSAGES_SNAPSHOT
 * - Special Events (2): RAW, CUSTOM
 *
 * Total: 16 events
 */

@Serializable
enum class EventType {
    // Lifecycle Events
    RUN_STARTED,
    RUN_FINISHED,
    RUN_ERROR,
    STEP_STARTED,
    STEP_FINISHED,

    // Text Message Events
    TEXT_MESSAGE_START,
    TEXT_MESSAGE_CONTENT,
    TEXT_MESSAGE_END,

    // Tool Call Events
    TOOL_CALL_START,
    TOOL_CALL_ARGS,
    TOOL_CALL_END,

    // State Management Events
    STATE_SNAPSHOT,
    STATE_DELTA,
    MESSAGES_SNAPSHOT,

    // Special Events
    RAW,
    CUSTOM
}

/**
 * Base interface for all events in the AG-UI protocol.
 *
 */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonClassDiscriminator("type")
sealed interface BaseEvent {
    @SerialName("type")
    val eventType: EventType
    val timestamp: Long?
    val rawEvent: JsonElement?
}

// ============== Lifecycle Events (5) ==============

@Serializable
@SerialName("RUN_STARTED")
data class RunStartedEvent(
    override val eventType: EventType = EventType.RUN_STARTED,
    val threadId: String,
    val runId: String,
    override val timestamp: Long? = null,
    override val rawEvent: JsonElement? = null
) : BaseEvent

@Serializable
@SerialName("RUN_FINISHED")
data class RunFinishedEvent(
    override val eventType: EventType = EventType.RUN_FINISHED,
    val threadId: String,
    val runId: String,
    override val timestamp: Long? = null,
    override val rawEvent: JsonElement? = null
) : BaseEvent

@Serializable
@SerialName("RUN_ERROR")
data class RunErrorEvent(
    override val eventType: EventType = EventType.RUN_ERROR,
    val message: String,
    val code: String? = null,
    override val timestamp: Long? = null,
    override val rawEvent: JsonElement? = null
) : BaseEvent

@Serializable
@SerialName("STEP_STARTED")
data class StepStartedEvent(
    override val eventType: EventType = EventType.STEP_STARTED,
    val stepName: String,
    override val timestamp: Long? = null,
    override val rawEvent: JsonElement? = null
) : BaseEvent

@Serializable
@SerialName("STEP_FINISHED")
data class StepFinishedEvent(
    override val eventType: EventType = EventType.STEP_FINISHED,
    val stepName: String,
    override val timestamp: Long? = null,
    override val rawEvent: JsonElement? = null
) : BaseEvent

// ============== Text Message Events (3) ==============

@Serializable
@SerialName("TEXT_MESSAGE_START")
data class TextMessageStartEvent(
    override val eventType: EventType = EventType.TEXT_MESSAGE_START,
    val messageId: String,
    val role: String = "assistant",
    override val timestamp: Long? = null,
    override val rawEvent: JsonElement? = null
) : BaseEvent

@Serializable
@SerialName("TEXT_MESSAGE_CONTENT")
data class TextMessageContentEvent(
    override val eventType: EventType = EventType.TEXT_MESSAGE_CONTENT,
    val messageId: String,
    val delta: String,
    override val timestamp: Long? = null,
    override val rawEvent: JsonElement? = null
) : BaseEvent {
    init {
        require(delta.isNotEmpty()) { "Text message content delta cannot be empty" }
    }
}

@Serializable
@SerialName("TEXT_MESSAGE_END")
data class TextMessageEndEvent(
    override val eventType: EventType = EventType.TEXT_MESSAGE_END,
    val messageId: String,
    override val timestamp: Long? = null,
    override val rawEvent: JsonElement? = null
) : BaseEvent

// ============== Tool Call Events (3) ==============

@Serializable
@SerialName("TOOL_CALL_START")
data class ToolCallStartEvent(
    override val eventType: EventType = EventType.TOOL_CALL_START,
    val toolCallId: String,
    val toolCallName: String,
    val parentMessageId: String? = null,
    override val timestamp: Long? = null,
    override val rawEvent: JsonElement? = null
) : BaseEvent

@Serializable
@SerialName("TOOL_CALL_ARGS")
data class ToolCallArgsEvent(
    override val eventType: EventType = EventType.TOOL_CALL_ARGS,
    val toolCallId: String,
    val delta: String,
    override val timestamp: Long? = null,
    override val rawEvent: JsonElement? = null
) : BaseEvent

@Serializable
@SerialName("TOOL_CALL_END")
data class ToolCallEndEvent(
    override val eventType: EventType = EventType.TOOL_CALL_END,
    val toolCallId: String,
    override val timestamp: Long? = null,
    override val rawEvent: JsonElement? = null
) : BaseEvent

// ============== State Management Events (3) ==============

@Serializable
@SerialName("STATE_SNAPSHOT")
data class StateSnapshotEvent(
    override val eventType: EventType = EventType.STATE_SNAPSHOT,
    val snapshot: JsonElement,
    override val timestamp: Long? = null,
    override val rawEvent: JsonElement? = null
) : BaseEvent

@Serializable
@SerialName("STATE_DELTA")
data class StateDeltaEvent(
    override val eventType: EventType = EventType.STATE_DELTA,
    val delta: JsonElement,  // JSON Patch array as defined in RFC 6902
    override val timestamp: Long? = null,
    override val rawEvent: JsonElement? = null
) : BaseEvent

@Serializable
@SerialName("MESSAGES_SNAPSHOT")
data class MessagesSnapshotEvent(
    override val eventType: EventType = EventType.MESSAGES_SNAPSHOT,
    val messages: List<Message>,
    override val timestamp: Long? = null,
    override val rawEvent: JsonElement? = null
) : BaseEvent

// ============== Special Events (2) ==============

@Serializable
@SerialName("RAW")
data class RawEvent(
    override val eventType: EventType = EventType.RAW,
    val event: JsonElement,
    val source: String? = null,
    override val timestamp: Long? = null,
    override val rawEvent: JsonElement? = null
) : BaseEvent

@Serializable
@SerialName("CUSTOM")
data class CustomEvent(
    override val eventType: EventType = EventType.CUSTOM,
    val name: String,
    val value: JsonElement,
    override val timestamp: Long? = null,
    override val rawEvent: JsonElement? = null
) : BaseEvent