package com.contextable.agui4k.core.protocol

/**
 * Enum defining all possible event types in the AG-UI protocol.
 * 
 * With K2 compiler optimizations, enum exhaustiveness checking is improved,
 * providing better compile-time safety for when expressions.
 */
enum class EventType {
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
    CUSTOM,
    
    // Lifecycle Events
    RUN_STARTED,
    RUN_FINISHED,
    RUN_ERROR,
    STEP_STARTED,
    STEP_FINISHED
}
