package com.contextable.agui4k.core.serialization

import com.contextable.agui4k.core.types.*
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

val eventSerializersModule = SerializersModule {
    polymorphic(BaseEvent::class) {
        subclass(RunStartedEvent::class)
        subclass(RunFinishedEvent::class)
        subclass(RunErrorEvent::class)
        subclass(StepStartedEvent::class)
        subclass(StepFinishedEvent::class)
        subclass(TextMessageStartEvent::class)
        subclass(TextMessageContentEvent::class)
        subclass(TextMessageEndEvent::class)
        subclass(ToolCallStartEvent::class)
        subclass(ToolCallArgsEvent::class)
        subclass(ToolCallEndEvent::class)
        subclass(StateSnapshotEvent::class)
        subclass(StateDeltaEvent::class)
        subclass(MessagesSnapshotEvent::class)
        subclass(RawEvent::class)
        subclass(CustomEvent::class)
    }
}