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
    @SerialName("RUN_STARTED") RUN_STARTED,
    @SerialName("RUN_FINISHED") RUN_FINISHED,
    @SerialName("RUN_ERROR") RUN_ERROR,
    @SerialName("STEP_STARTED") STEP_STARTED,
    @SerialName("STEP_FINISHED") STEP_FINISHED,

    // Text Message Events
    @SerialName("TEXT_MESSAGE_START") TEXT_MESSAGE_START,
    @SerialName("TEXT_MESSAGE_CONTENT") TEXT_MESSAGE_CONTENT,
    @SerialName("TEXT_MESSAGE_END") TEXT_MESSAGE_END,

    // Tool Call Events
    @SerialName("TOOL_CALL_START") TOOL_CALL_START,
    @SerialName("TOOL_CALL_ARGS") TOOL_CALL_ARGS,
    @SerialName("TOOL_CALL_END") TOOL_CALL_END,

    // State Management Events
    @SerialName("STATE_SNAPSHOT") STATE_SNAPSHOT,
    @SerialName("STATE_DELTA") STATE_DELTA,
    @SerialName("MESSAGES_SNAPSHOT") MESSAGES_SNAPSHOT,

    // Special Events
    @SerialName("RAW") RAW,
    @SerialName("CUSTOM") CUSTOM
}

/**
 * Base interface for all events in the AG-UI protocol.
 *
 */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonClassDiscriminator("type")
sealed interface BaseEvent {
    val type: EventType
    val timestamp: Long?
    val rawEvent: JsonElement?
}

// ============== Lifecycle Events (5) ==============

@Serializable
data class RunStartedEvent(
    override val type: EventType = EventType.RUN_STARTED,
    val threadId: String,
    val runId: String,
    override val timestamp: Long? = null,
    override val rawEvent: JsonElement? = null
) : BaseEvent

@Serializable
data class RunFinishedEvent(
    override val type: EventType = EventType.RUN_FINISHED,
    val threadId: String,
    val runId: String,
    override val timestamp: Long? = null,
    override val rawEvent: JsonElement? = null
) : BaseEvent

@Serializable
data class RunErrorEvent(
    override val type: EventType = EventType.RUN_ERROR,
    val message: String,
    val code: String? = null,
    override val timestamp: Long? = null,
    override val rawEvent: JsonElement? = null
) : BaseEvent

@Serializable
data class StepStartedEvent(
    override val type: EventType = EventType.STEP_STARTED,
    val stepName: String,
    override val timestamp: Long? = null,
    override val rawEvent: JsonElement? = null
) : BaseEvent

@Serializable
data class StepFinishedEvent(
    override val type: EventType = EventType.STEP_FINISHED,
    val stepName: String,
    override val timestamp: Long? = null,
    override val rawEvent: JsonElement? = null
) : BaseEvent

// ============== Text Message Events (3) ==============

@Serializable
data class TextMessageStartEvent(
    override val type: EventType = EventType.TEXT_MESSAGE_START,
    val messageId: String,
    val role: String = "assistant",
    override val timestamp: Long? = null,
    override val rawEvent: JsonElement? = null
) : BaseEvent

@Serializable
data class TextMessageContentEvent(
    override val type: EventType = EventType.TEXT_MESSAGE_CONTENT,
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
data class TextMessageEndEvent(
    override val type: EventType = EventType.TEXT_MESSAGE_END,
    val messageId: String,
    override val timestamp: Long? = null,
    override val rawEvent: JsonElement? = null
) : BaseEvent

// ============== Tool Call Events (3) ==============

@Serializable
data class ToolCallStartEvent(
    override val type: EventType = EventType.TOOL_CALL_START,
    val toolCallId: String,
    val toolCallName: String,
    val parentMessageId: String? = null,
    override val timestamp: Long? = null,
    override val rawEvent: JsonElement? = null
) : BaseEvent

@Serializable
data class ToolCallArgsEvent(
    override val type: EventType = EventType.TOOL_CALL_ARGS,
    val toolCallId: String,
    val delta: String,
    override val timestamp: Long? = null,
    override val rawEvent: JsonElement? = null
) : BaseEvent

@Serializable
data class ToolCallEndEvent(
    override val type: EventType = EventType.TOOL_CALL_END,
    val toolCallId: String,
    override val timestamp: Long? = null,
    override val rawEvent: JsonElement? = null
) : BaseEvent

// ============== State Management Events (3) ==============

@Serializable
data class StateSnapshotEvent(
    override val type: EventType = EventType.STATE_SNAPSHOT,
    val snapshot: JsonElement,
    override val timestamp: Long? = null,
    override val rawEvent: JsonElement? = null
) : BaseEvent

@Serializable
data class StateDeltaEvent(
    override val type: EventType = EventType.STATE_DELTA,
    val delta: JsonElement,  // JSON Patch array as defined in RFC 6902
    override val timestamp: Long? = null,
    override val rawEvent: JsonElement? = null
) : BaseEvent

@Serializable
data class MessagesSnapshotEvent(
    override val type: EventType = EventType.MESSAGES_SNAPSHOT,
    val messages: List<Message>,
    override val timestamp: Long? = null,
    override val rawEvent: JsonElement? = null
) : BaseEvent

// ============== Special Events (2) ==============

@Serializable
data class RawEvent(
    override val type: EventType = EventType.RAW,
    val event: JsonElement,
    val source: String? = null,
    override val timestamp: Long? = null,
    override val rawEvent: JsonElement? = null
) : BaseEvent

@Serializable
data class CustomEvent(
    override val type: EventType = EventType.CUSTOM,
    val name: String,
    val value: JsonElement,
    override val timestamp: Long? = null,
    override val rawEvent: JsonElement? = null
) : BaseEvent