package com.contextable.agui4k.core.serialization

import com.contextable.agui4k.core.types.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

/**
 * Serializers module for AG-UI protocol types.
 * Defines polymorphic serialization for all event types.
 */
val agUiSerializersModule = SerializersModule {
    // Polymorphic serialization for events
    polymorphic(BaseEvent::class) {
        // Lifecycle Events (5)
        subclass(RunStartedEvent::class)
        subclass(RunFinishedEvent::class)
        subclass(RunErrorEvent::class)
        subclass(StepStartedEvent::class)
        subclass(StepFinishedEvent::class)

        // Text Message Events (3)
        subclass(TextMessageStartEvent::class)
        subclass(TextMessageContentEvent::class)
        subclass(TextMessageEndEvent::class)

        // Tool Call Events (3)
        subclass(ToolCallStartEvent::class)
        subclass(ToolCallArgsEvent::class)
        subclass(ToolCallEndEvent::class)

        // State Management Events (3)
        subclass(StateSnapshotEvent::class)
        subclass(StateDeltaEvent::class)
        subclass(MessagesSnapshotEvent::class)

        // Special Events (2)
        subclass(RawEvent::class)
        subclass(CustomEvent::class)
    }
}

/**
 * Configured JSON instance for AG-UI protocol serialization.
 *
 * Configuration:
 * - Uses "type" as the class discriminator for polymorphic types
 * - Ignores unknown keys for forward compatibility
 * - Lenient parsing for flexibility
 * - Encodes defaults to ensure protocol compliance
 * - Does NOT include nulls by default (explicitNulls = false)
 */
val AgUiJson = Json {
    serializersModule = agUiSerializersModule
    classDiscriminator = "type"  // AG-UI protocol uses "type" for event discrimination
    ignoreUnknownKeys = true     // Forward compatibility
    isLenient = true             // Allow flexibility in parsing
    encodeDefaults = true        // Ensure all fields are present
    explicitNulls = false        // Don't include null fields
    prettyPrint = false          // Compact output for efficiency
}

/**
 * Pretty-printing JSON instance for debugging.
 */
val AgUiJsonPretty = Json(from = AgUiJson) {
    prettyPrint = true
}