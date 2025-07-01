/*
 * MIT License
 *
 * Copyright (c) 2025 Mark Fogle
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.contextable.agui4k.core.types

import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

/**
 * Defines polymorphic serialization for all AG-UI Data Types.
 */
val AgUiSerializersModule by lazy {
    SerializersModule {
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

        polymorphic(Message::class) {
            subclass(DeveloperMessage::class)
            subclass(SystemMessage::class)
            subclass(AssistantMessage::class)
            subclass(UserMessage::class)
            subclass(ToolMessage::class)
        }
    }
}