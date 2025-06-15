package com.contextable.agui4k.core.types

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * Defines a tool that can be called by an agent.
 * 
 * Tools are functions that agents can call to request specific information,
 * perform actions in external systems, ask for human input or confirmation,
 * or access specialized capabilities.
 */
@Serializable
data class Tool(
    val name: String,
    val description: String,
    val parameters: JsonElement // JSON Schema defining the parameters
)

/**
 * Represents a piece of contextual information provided to an agent.
 * 
 * Context provides additional information that helps the agent understand
 * the current situation and make better decisions.
 */
@Serializable
data class Context(
    val description: String,
    val value: String
)

/**
 * Input parameters for connecting to an agent.
 * This is the body of the POST request sent to the agent's HTTP endpoint.
 */
@Serializable
data class RunAgentInput(
    val threadId: String,
    val runId: String,
    val state: JsonElement? = null,
    val messages: List<@Serializable(with = MessageSerializer::class) Message> = emptyList(),
    val tools: List<Tool> = emptyList(),
    val context: List<Context> = emptyList(),
    val forwardedProps: JsonElement? = null
)

/**
 * Parameters for connecting to an agent from the client side.
 * These are the parameters passed to the runAgent() method.
 */
data class RunAgentParameters(
    val runId: String? = null,
    val tools: List<Tool> = emptyList(),
    val context: List<Context> = emptyList(),
    val forwardedProps: JsonElement? = null
)
