package com.contextable.agui4k.core.types

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * Enum representing the possible roles a message sender can have.
 */
@Serializable
enum class Role {
    @SerialName("developer")
    DEVELOPER,

    @SerialName("system")
    SYSTEM,

    @SerialName("assistant")
    ASSISTANT,

    @SerialName("user")
    USER,

    @SerialName("tool")
    TOOL
}

/**
 * Base interface for all message types in the AG-UI protocol.
 */
@OptIn(ExperimentalSerializationApi::class)
@Serializable(with = MessageSerializer::class)
sealed interface Message {
    val id: String
    val role: Role
    val content: String?
    val name: String?
}

/**
 * Represents a message from a developer.
 */
@Serializable
data class DeveloperMessage(
    override val id: String,
    override val role: Role = Role.DEVELOPER,
    override val content: String,
    override val name: String? = null
) : Message

/**
 * Represents a system message.
 */
@Serializable
data class SystemMessage(
    override val id: String,
    override val role: Role = Role.SYSTEM,
    override val content: String,
    override val name: String? = null
) : Message

/**
 * Represents a message from an assistant.
 */
@Serializable
data class AssistantMessage(
    override val id: String,
    override val role: Role = Role.ASSISTANT,
    override val content: String? = null,
    override val name: String? = null,
    val toolCalls: List<ToolCall>? = null
) : Message

/**
 * Represents a message from a user.
 */
@Serializable
data class UserMessage(
    override val id: String,
    override val role: Role = Role.USER,
    override val content: String,
    override val name: String? = null
) : Message

/**
 * Represents a message from a tool.
 */
@Serializable
data class ToolMessage(
    override val id: String,
    override val role: Role = Role.TOOL,
    override val content: String,
    val toolCallId: String,
    override val name: String? = null
) : Message

/**
 * Represents a tool call made by an agent.
 */
@Serializable
data class ToolCall(
    val id: String,
    val type: String = "function",
    val function: FunctionCall
)

/**
 * Represents function name and arguments in a tool call.
 */
@Serializable
data class FunctionCall(
    val name: String,
    val arguments: String // JSON-encoded string
)
