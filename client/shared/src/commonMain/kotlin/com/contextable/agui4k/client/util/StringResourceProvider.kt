package com.contextable.agui4k.client.util

import org.jetbrains.compose.resources.StringResource
import agui4kclient.shared.generated.resources.*

/**
 * Provider for accessing string resources outside of Composable functions.
 * This is useful for ViewModels, repositories, and other non-UI classes.
 *
 * Note: This is a simplified approach. In a production app, you might want
 * to use a more sophisticated localization system that can handle plurals,
 * formatting, and other advanced features.
 */
object StringResourceProvider {

    // Connection messages
    fun getConnectedToAgent(agentName: String): String {
        return "Connected to $agentName" // In real app, format with string resource
    }

    fun getFailedToConnect(error: String): String {
        return "Failed to connect: $error" // In real app, format with string resource
    }

    fun getAgentError(error: String): String {
        return "Agent error: $error" // In real app, format with string resource
    }

    fun getErrorPrefix(error: String): String {
        return "Error: $error" // In real app, format with string resource
    }

    // Validation messages
    fun getNameRequired(): String {
        return "Name is required" // In real app, get from string resource
    }

    fun getUrlRequired(): String {
        return "URL is required" // In real app, get from string resource
    }

    fun getUrlInvalid(): String {
        return "URL must start with http:// or https://" // In real app, get from string resource
    }

    // Auth method labels
    fun getAuthMethodLabel(authMethod: com.contextable.agui4k.client.data.model.AuthMethod): String {
        return when (authMethod) {
            is com.contextable.agui4k.client.data.model.AuthMethod.None -> "No Authentication"
            is com.contextable.agui4k.client.data.model.AuthMethod.ApiKey -> "API Key"
            is com.contextable.agui4k.client.data.model.AuthMethod.BearerToken -> "Bearer Token"
            is com.contextable.agui4k.client.data.model.AuthMethod.BasicAuth -> "Basic Auth"
            is com.contextable.agui4k.client.data.model.AuthMethod.OAuth2 -> "OAuth 2.0"
            is com.contextable.agui4k.client.data.model.AuthMethod.Custom -> "Custom"
        }
    }
}

/**
 * Extension functions to make string resource access easier
 */
object Strings {

    // Common strings that are frequently used in non-Composable contexts
    const val ERROR_PREFIX = "Error: "
    const val AGENT_ERROR_PREFIX = "Agent error: "
    const val FAILED_TO_CONNECT_PREFIX = "Failed to connect: "
    const val CONNECTED_TO_PREFIX = "Connected to "

    // Validation messages
    const val NAME_REQUIRED = "Name is required"
    const val URL_REQUIRED = "URL is required"
    const val URL_INVALID = "URL must start with http:// or https://"
}