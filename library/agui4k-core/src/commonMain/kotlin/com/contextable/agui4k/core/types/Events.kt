package com.contextable.agui4k.core.types

import com.contextable.agui4k.core.protocol.EventType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * Base interface for all events in the AG-UI protocol.
 *
 * IMPORTANT: The 'type' field is NOT serialized as part of the event data.
 * It's only used as a discriminator for polymorphic serialization.
 * This ensures compliance with the AG-UI protocol specification.
 */
@Serializable
sealed interface BaseEvent {
    val timestamp: Long?
    val rawEvent: JsonElement?
}

// ============== Lifecycle Events (5) ==============

@Serializable
@SerialName("RUN_STARTED")
data class RunStartedEvent(
    val threadId: String,
    val runId: String,
    override val timestamp: Long? = null,
    override val rawEvent: JsonElement? = null
) : BaseEvent

@Serializable
@SerialName("RUN_FINISHED")
data class RunFinishedEvent(
    val threadId: String,
    val runId: String,
    override val timestamp: Long? = null,
    override val rawEvent: JsonElement? = null
) : BaseEvent

@Serializable
@SerialName("RUN_ERROR")
data class RunErrorEvent(
    val message: String,
    val code: String? = null,
    override val timestamp: Long? = null,
    override val rawEvent: JsonElement? = null
) : BaseEvent

@Serializable
@SerialName("STEP_STARTED")
data class StepStartedEvent(
    val stepName: String,
    override val timestamp: Long? = null,
    override val rawEvent: JsonElement? = null
) : BaseEvent

@Serializable
@SerialName("STEP_FINISHED")
data class StepFinishedEvent(
    val stepName: String,
    override val timestamp: Long? = null,
    override val rawEvent: JsonElement? = null
) : BaseEvent

// ============== Text Message Events (3) ==============

@Serializable
@SerialName("TEXT_MESSAGE_START")
data class TextMessageStartEvent(
    val messageId: String,
    val role: String = "assistant",
    override val timestamp: Long? = null,
    override val rawEvent: JsonElement? = null
) : BaseEvent

@Serializable
@SerialName("TEXT_MESSAGE_CONTENT")
data class TextMessageContentEvent(
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
    val messageId: String,
    override val timestamp: Long? = null,
    override val rawEvent: JsonElement? = null
) : BaseEvent

// ============== Tool Call Events (3) ==============

@Serializable
@SerialName("TOOL_CALL_START")
data class ToolCallStartEvent(
    val toolCallId: String,
    val toolCallName: String,
    val parentMessageId: String? = null,
    override val timestamp: Long? = null,
    override val rawEvent: JsonElement? = null
) : BaseEvent

@Serializable
@SerialName("TOOL_CALL_ARGS")
data class ToolCallArgsEvent(
    val toolCallId: String,
    val delta: String,
    override val timestamp: Long? = null,
    override val rawEvent: JsonElement? = null
) : BaseEvent

@Serializable
@SerialName("TOOL_CALL_END")
data class ToolCallEndEvent(
    val toolCallId: String,
    override val timestamp: Long? = null,
    override val rawEvent: JsonElement? = null
) : BaseEvent

// ============== State Management Events (3) ==============

@Serializable
@SerialName("STATE_SNAPSHOT")
data class StateSnapshotEvent(
    val snapshot: JsonElement,
    override val timestamp: Long? = null,
    override val rawEvent: JsonElement? = null
) : BaseEvent

@Serializable
@SerialName("STATE_DELTA")
data class StateDeltaEvent(
    val delta: JsonElement,  // JSON Patch array as defined in RFC 6902
    override val timestamp: Long? = null,
    override val rawEvent: JsonElement? = null
) : BaseEvent

@Serializable
@SerialName("MESSAGES_SNAPSHOT")
data class MessagesSnapshotEvent(
    val messages: List<Message>,
    override val timestamp: Long? = null,
    override val rawEvent: JsonElement? = null
) : BaseEvent

// ============== Special Events (2) ==============

@Serializable
@SerialName("RAW")
data class RawEvent(
    val event: JsonElement,
    val source: String? = null,
    override val timestamp: Long? = null,
    override val rawEvent: JsonElement? = null
) : BaseEvent

@Serializable
@SerialName("CUSTOM")
data class CustomEvent(
    val name: String,
    val value: JsonElement,
    override val timestamp: Long? = null,
    override val rawEvent: JsonElement? = null
) : BaseEvent