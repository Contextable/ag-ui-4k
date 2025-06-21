package com.contextable.agui4k.core.protocol

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
enum class EventType {
    // Lifecycle Events (5)
    RUN_STARTED,
    RUN_FINISHED,
    RUN_ERROR,
    STEP_STARTED,
    STEP_FINISHED,

    // Text Message Events (3)
    TEXT_MESSAGE_START,
    TEXT_MESSAGE_CONTENT,
    TEXT_MESSAGE_END,

    // Tool Call Events (3)
    TOOL_CALL_START,
    TOOL_CALL_ARGS,
    TOOL_CALL_END,

    // State Management Events (3)
    STATE_SNAPSHOT,
    STATE_DELTA,
    MESSAGES_SNAPSHOT,

    // Special Events (2)
    RAW,
    CUSTOM;

    companion object {
        /**
         * Total number of event types in the protocol.
         */
        const val TOTAL_EVENT_TYPES = 16

        /**
         * Verify we have exactly 16 event types.
         */
        init {
            require(entries.size == TOTAL_EVENT_TYPES) {
                "AG-UI protocol specifies exactly $TOTAL_EVENT_TYPES event types, but found ${entries.size}"
            }
        }
    }
}