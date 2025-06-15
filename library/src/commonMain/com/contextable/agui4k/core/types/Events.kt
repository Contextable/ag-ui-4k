package com.contextable.agui4k.core.types

import com.contextable.agui4k.core.protocol.EventType
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * Base interface for all events in the AG-UI protocol.
 */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
sealed interface BaseEvent {
    val type: EventType
    val timestamp: Long?
    val rawEvent: JsonElement?
}

// Lifecycle Events

@Serializable
@SerialName("RUN_STARTED")
data class RunStartedEvent(
    override val type: EventType = EventType.RUN_STARTED,
    val threadId: String,
    val runId: String,
    override val timestamp: Long? = null,
    override val rawEvent: JsonElement? = null
) : BaseEvent

@Serializable
@SerialName("RUN_FINISHED")
data class RunFinishedEvent(
    override val type: EventType = EventType.RUN_FINISHED,
    val threadId: String,
    val runId: String,
    override val timestamp: Long? = null,
    override val rawEvent: JsonElement? = null
) : BaseEvent

@Serializable
@SerialName("RUN_ERROR")
data class RunErrorEvent(
    override val type: EventType = EventType.RUN_ERROR,
    val message: String,
    val code: String? = null,
    override val timestamp: Long? = null,
    override val rawEvent: JsonElement? = null
) : BaseEvent

@Serializable
@SerialName("STEP_STARTED")
data class StepStartedEvent(
    override val type: EventType = EventType.STEP_STARTED,
    val stepName: String,
    override val timestamp: Long? = null,
    override val rawEvent: JsonElement? = null
) : BaseEvent

@Serializable
@SerialName("STEP_FINISHED")
data class StepFinishedEvent(
    override val type: EventType = EventType.STEP_FINISHED,
    val stepName: String,
    override val timestamp: Long? = null,
    override val rawEvent: JsonElement? = null
) : BaseEvent

// Text Message Events

@Serializable
@SerialName("TEXT_MESSAGE_START")
data class TextMessageStartEvent(
    override val type: EventType = EventType.TEXT_MESSAGE_START,
    val messageId: String,
    val role: String = "assistant",
    override val timestamp: Long? = null,
    override val rawEvent: JsonElement? = null
) : BaseEvent

@Serializable
@SerialName("TEXT_MESSAGE_CONTENT")
data class TextMessageContentEvent(
    override val type: EventType = EventType.TEXT_MESSAGE_CONTENT,
    val messageId: String,
    val delta: String,
    override val timestamp: Long? = null,
    override val rawEvent: JsonElement? = null
) : BaseEvent

@Serializable
@SerialName("TEXT_MESSAGE_END")
data class TextMessageEndEvent(
    override val type: EventType = EventType.TEXT_MESSAGE_END,
    val messageId: String,
    override val timestamp: Long? = null,
    override val rawEvent: JsonElement? = null
) : BaseEvent

// Tool Call Events

@Serializable
@SerialName("TOOL_CALL_START")
data class ToolCallStartEvent(
    override val type: EventType = EventType.TOOL_CALL_START,
    val toolCallId: String,
    val toolCallName: String,
    val parentMessageId: String? = null,
    override val timestamp: Long? = null,
    override val rawEvent: JsonElement? = null
) : BaseEvent

@Serializable
@SerialName("TOOL_CALL_ARGS")
data class ToolCallArgsEvent(
    override val type: EventType = EventType.TOOL_CALL_ARGS,
    val toolCallId: String,
    val delta: String,
    override val timestamp: Long? = null,
    override val rawEvent: JsonElement? = null
) : BaseEvent

@Serializable
@SerialName("TOOL_CALL_END")
data class ToolCallEndEvent(
    override val type: EventType = EventType.TOOL_CALL_END,
    val toolCallId: String,
    override val timestamp: Long? = null,
    override val rawEvent: JsonElement? = null
) : BaseEvent

// State Management Events

@Serializable
@SerialName("STATE_SNAPSHOT")
data class StateSnapshotEvent(
    override val type: EventType = EventType.STATE_SNAPSHOT,
    val snapshot: JsonElement,
    override val timestamp: Long? = null,
    override val rawEvent: JsonElement? = null
) : BaseEvent

@Serializable
@SerialName("STATE_DELTA")
data class StateDeltaEvent(
    override val type: EventType = EventType.STATE_DELTA,
    val delta: List<JsonPatchOperation>,
    override val timestamp: Long? = null,
    override val rawEvent: JsonElement? = null
) : BaseEvent

@Serializable
@SerialName("MESSAGES_SNAPSHOT")
data class MessagesSnapshotEvent(
    override val type: EventType = EventType.MESSAGES_SNAPSHOT,
    val messages: List<Message>,
    override val timestamp: Long? = null,
    override val rawEvent: JsonElement? = null
) : BaseEvent

// Special Events

@Serializable
@SerialName("RAW")
data class RawEvent(
    override val type: EventType = EventType.RAW,
    val event: JsonElement,
    val source: String? = null,
    override val timestamp: Long? = null,
    override val rawEvent: JsonElement? = null
) : BaseEvent

@Serializable
@SerialName("CUSTOM")
data class CustomEvent(
    override val type: EventType = EventType.CUSTOM,
    val name: String,
    val value: JsonElement,
    override val timestamp: Long? = null,
    override val rawEvent: JsonElement? = null
) : BaseEvent

/**
 * JSON Patch operation as defined in RFC 6902
 */
@Serializable
data class JsonPatchOperation(
    val op: String, // "add", "remove", "replace", "move", "copy", "test"
    val path: String,
    val value: JsonElement? = null,
    val from: String? = null
)