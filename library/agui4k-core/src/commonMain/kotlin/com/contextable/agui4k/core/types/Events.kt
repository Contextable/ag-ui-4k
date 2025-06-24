package com.contextable.agui4k.core.types

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.JsonArray
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
    @SerialName("RUN_STARTED")
    RUN_STARTED,
    @SerialName("RUN_FINISHED")
    RUN_FINISHED,
    @SerialName("RUN_ERROR")
    RUN_ERROR,
    @SerialName("STEP_STARTED")
    STEP_STARTED,
    @SerialName("STEP_FINISHED")
    STEP_FINISHED,

    // Text Message Events
    @SerialName("TEXT_MESSAGE_START")
    TEXT_MESSAGE_START,
    @SerialName("TEXT_MESSAGE_CONTENT")
    TEXT_MESSAGE_CONTENT,
    @SerialName("TEXT_MESSAGE_END")
    TEXT_MESSAGE_END,

    // Tool Call Events
    @SerialName("TOOL_CALL_START")
    TOOL_CALL_START,
    @SerialName("TOOL_CALL_ARGS")
    TOOL_CALL_ARGS,
    @SerialName("TOOL_CALL_END")
    TOOL_CALL_END,

    // State Management Events
    @SerialName("STATE_SNAPSHOT")
    STATE_SNAPSHOT,
    @SerialName("STATE_DELTA")
    STATE_DELTA,
    @SerialName("MESSAGES_SNAPSHOT")
    MESSAGES_SNAPSHOT,

    // Special Events
    @SerialName("RAW")
    RAW,
    @SerialName("CUSTOM")
    CUSTOM

    // Note: The protocol definitions (i.e., events.py and  events.ts) in the current version of
    // the offical AG-UI Python and Typescript SDKs have several additional event types. Specifically
    //
    // TEXT_MESSAGE_CHUNK
    // TOOL_CALL_CHUNK
    // THINKING_TEXT_MESSAGE_START
    // THINKING_TEXT_MESSAGE_CONTENT
    // THINKING_TEXT_MESSAGE_END
    // THINKING_START
    // THINKING_END
    //
    // These are left out for now as they do not appear in the actual protocol documentation,
    // but could be added if needed.
}

/**
 * Base interface for all events in the AG-UI protocol.
 *
 */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonClassDiscriminator("type")
sealed class BaseEvent {
    // Necessary to deal with Kotlinx polymorphic serialization; without this, there's a conflict.
    // Note: This property is not serialized - the "type" field comes from @JsonClassDiscriminator
    abstract val eventType: EventType
    // The type of timestamp is somewhat nebulous.  In the Typescript version of the protocol,
    // it is an optional number which would be "Double?" in Kotlin.  But in the Python version,
    // it is "int", which is closer to "Long?".  Going off of the generally accepted meaning of
    // timestamp, we will therefore stick with "Long?".
    abstract val timestamp: Long?
    abstract val rawEvent: JsonElement?
}

// ============== Lifecycle Events (5) ==============

@Serializable
@SerialName("RUN_STARTED")
data class RunStartedEvent(
    val threadId: String,
    val runId: String,
    override val timestamp: Long? = null,
    override val rawEvent: JsonElement? = null
) : BaseEvent () {
    @Transient
    override val eventType: EventType = EventType.RUN_STARTED
}

@Serializable
@SerialName("RUN_FINISHED")
data class RunFinishedEvent(
    val threadId: String,
    val runId: String,
    override val timestamp: Long? = null,
    override val rawEvent: JsonElement? = null
) : BaseEvent () {
    @Transient
    override val eventType: EventType = EventType.RUN_FINISHED
}

@Serializable
@SerialName("RUN_ERROR")
data class RunErrorEvent(
    val message: String,
    val code: String? = null,
    override val timestamp: Long? = null,
    override val rawEvent: JsonElement? = null
) : BaseEvent () {
    @Transient
    override val eventType: EventType = EventType.RUN_ERROR
}

@Serializable
@SerialName("STEP_STARTED")
data class StepStartedEvent(
    val stepName: String,
    override val timestamp: Long? = null,
    override val rawEvent: JsonElement? = null
) : BaseEvent () {
    @Transient
    override val eventType: EventType = EventType.STEP_STARTED
}

@Serializable
@SerialName("STEP_FINISHED")
data class StepFinishedEvent(
    val stepName: String,
    override val timestamp: Long? = null,
    override val rawEvent: JsonElement? = null
) : BaseEvent () {
    @Transient
    override val eventType: EventType = EventType.STEP_FINISHED
}

// ============== Text Message Events (3) ==============

@Serializable
@SerialName("TEXT_MESSAGE_START")
data class TextMessageStartEvent(
    val messageId: String,
    override val timestamp: Long? = null,
    override val rawEvent: JsonElement? = null
) : BaseEvent () {
    @Transient
    override val eventType: EventType = EventType.TEXT_MESSAGE_START
    // Needed for serialization/deserialization for protocol correctness
    val role : String = "assistant"
}

@Serializable
@SerialName("TEXT_MESSAGE_CONTENT")
data class TextMessageContentEvent(
    val messageId: String,
    val delta: String,
    override val timestamp: Long? = null,
    override val rawEvent: JsonElement? = null
) : BaseEvent () {
    @Transient
    override val eventType: EventType = EventType.TEXT_MESSAGE_CONTENT
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
) : BaseEvent () {
    @Transient
    override val eventType: EventType = EventType.TEXT_MESSAGE_END
}

// ============== Tool Call Events (3) ==============

@Serializable
@SerialName("TOOL_CALL_START")
data class ToolCallStartEvent(
    val toolCallId: String,
    val toolCallName: String,
    val parentMessageId: String? = null,
    override val timestamp: Long? = null,
    override val rawEvent: JsonElement? = null
) : BaseEvent () {
    @Transient
    override val eventType: EventType = EventType.TOOL_CALL_START
}

@Serializable
@SerialName("TOOL_CALL_ARGS")
data class ToolCallArgsEvent(
    val toolCallId: String,
    val delta: String,
    override val timestamp: Long? = null,
    override val rawEvent: JsonElement? = null
) : BaseEvent () {
    @Transient
    override val eventType: EventType = EventType.TOOL_CALL_ARGS
}

@Serializable
@SerialName("TOOL_CALL_END")
data class ToolCallEndEvent(
    val toolCallId: String,
    override val timestamp: Long? = null,
    override val rawEvent: JsonElement? = null
) : BaseEvent () {
    @Transient
    override val eventType: EventType = EventType.TOOL_CALL_END
}

// ============== State Management Events (3) ==============

@Serializable
@SerialName("STATE_SNAPSHOT")
data class StateSnapshotEvent(
    val snapshot: State,
    override val timestamp: Long? = null,
    override val rawEvent: JsonElement? = null
) : BaseEvent () {
    @Transient
    override val eventType: EventType = EventType.STATE_SNAPSHOT
}

@Serializable
@SerialName("STATE_DELTA")
data class StateDeltaEvent(
    val delta: JsonArray,  // JSON Patch array as defined in RFC 6902
    override val timestamp: Long? = null,
    override val rawEvent: JsonElement? = null
) : BaseEvent () {
    @Transient
    override val eventType: EventType = EventType.STATE_DELTA
}

@Serializable
@SerialName("MESSAGES_SNAPSHOT")
data class MessagesSnapshotEvent(
    val messages: List<Message>,
    override val timestamp: Long? = null,
    override val rawEvent: JsonElement? = null
) : BaseEvent () {
    @Transient
    override val eventType: EventType = EventType.MESSAGES_SNAPSHOT
}

// ============== Special Events (2) ==============

@Serializable
@SerialName("RAW")
data class RawEvent(
    val event: JsonElement,
    val source: String? = null,
    override val timestamp: Long? = null,
    override val rawEvent: JsonElement? = null
) : BaseEvent () {
    @Transient
    override val eventType: EventType = EventType.RAW
}

@Serializable
@SerialName("CUSTOM")
data class CustomEvent(
    val name: String,
    val value: JsonElement,
    override val timestamp: Long? = null,
    override val rawEvent: JsonElement? = null
) : BaseEvent () {
    @Transient
    override val eventType: EventType = EventType.CUSTOM
}