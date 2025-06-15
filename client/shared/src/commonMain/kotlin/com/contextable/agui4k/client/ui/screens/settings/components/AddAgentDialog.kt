package com.contextable.agui4k.client.ui.screens.settings.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.contextable.agui4k.client.data.model.AgentConfig
import com.contextable.agui4k.client.data.model.AuthMethod

fun getAuthMethodLabel(authMethod: AuthMethod): String {
    return when (authMethod) {
        is AuthMethod.None -> "No Authentication"
        is AuthMethod.ApiKey -> "API Key"
        is AuthMethod.BearerToken -> "Bearer Token"
        is AuthMethod.BasicAuth -> "Basic Auth"
        is AuthMethod.OAuth2 -> "OAuth 2.0"
        is AuthMethod.Custom -> "Custom"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAgentDialog(
    agent: AgentConfig? = null,
    onDismiss: () -> Unit,
    onConfirm: (AgentConfig) -> Unit
) {
    // ... rest of the composable code remains the same
    var name by remember { mutableStateOf(agent?.name ?: "") }
    var url by remember { mutableStateOf(agent?.url ?: "") }
    var description by remember { mutableStateOf(agent?.description ?: "") }
    var authMethod by remember { mutableStateOf(agent?.authMethod ?: AuthMethod.None()) }

    var nameError by remember { mutableStateOf<String?>(null) }
    var urlError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (agent != null) "Edit Agent" else "Add Agent")
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Name field
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        nameError = null
                    },
                    label = { Text("Name") },
                    placeholder = { Text("My Agent") },
                    singleLine = true,
                    isError = nameError != null,
                    supportingText = nameError?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth()
                )

                // URL field
                OutlinedTextField(
                    value = url,
                    onValueChange = {
                        url = it
                        urlError = null
                    },
                    label = { Text("URL") },
                    placeholder = { Text("https://api.example.com/agent") },
                    singleLine = true,
                    isError = urlError != null,
                    supportingText = urlError?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth()
                )

                // Description field
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (optional)") },
                    placeholder = { Text("A brief description of this agent") },
                    minLines = 2,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                // Auth method section
                AuthMethodSelector(
                    authMethod = authMethod,
                    onAuthMethodChange = { authMethod = it }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    // Validate inputs
                    var hasError = false

                    if (name.isBlank()) {
                        nameError = "Name is required"
                        hasError = true
                    }

                    if (url.isBlank()) {
                        urlError = "URL is required"
                        hasError = true
                    } else if (!url.startsWith("http://") && !url.startsWith("https://")) {
                        urlError = "URL must start with http:// or https://"
                        hasError = true
                    }

                    if (!hasError) {
                        val config = if (agent != null) {
                            agent.copy(
                                name = name.trim(),
                                url = url.trim(),
                                description = description.trim().takeIf { it.isNotEmpty() },
                                authMethod = authMethod
                            )
                        } else {
                            AgentConfig(
                                id = AgentConfig.generateId(),
                                name = name.trim(),
                                url = url.trim(),
                                description = description.trim().takeIf { it.isNotEmpty() },
                                authMethod = authMethod
                            )
                        }
                        onConfirm(config)
                    }
                }
            ) {
                Text(if (agent != null) "Save" else "Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AuthMethodSelector(
    authMethod: AuthMethod,
    onAuthMethodChange: (AuthMethod) -> Unit
) {
    // ... rest of the AuthMethodSelector code remains the same
    var expanded by remember { mutableStateOf(false) }

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Authentication",
            style = MaterialTheme.typography.labelLarge
        )

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it }
        ) {
            OutlinedTextField(
                value = getAuthMethodLabel(authMethod),
                onValueChange = { },
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("No Authentication") },
                    onClick = {
                        onAuthMethodChange(AuthMethod.None())
                        expanded = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("API Key") },
                    onClick = {
                        onAuthMethodChange(AuthMethod.ApiKey("", "X-API-Key"))
                        expanded = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("Bearer Token") },
                    onClick = {
                        onAuthMethodChange(AuthMethod.BearerToken(""))
                        expanded = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("Basic Auth") },
                    onClick = {
                        onAuthMethodChange(AuthMethod.BasicAuth("", ""))
                        expanded = false
                    }
                )
            }
        }

        // Auth method specific fields
        when (authMethod) {
            is AuthMethod.ApiKey -> {
                var apiKey by remember(authMethod) { mutableStateOf(authMethod.key) }
                var headerName by remember(authMethod) { mutableStateOf(authMethod.headerName) }

                OutlinedTextField(
                    value = apiKey,
                    onValueChange = {
                        apiKey = it
                        onAuthMethodChange(authMethod.copy(key = it))
                    },
                    label = { Text("API Key") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = headerName,
                    onValueChange = {
                        headerName = it
                        onAuthMethodChange(authMethod.copy(headerName = it))
                    },
                    label = { Text("Header Name") },
                    placeholder = { Text("X-API-Key") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            is AuthMethod.BearerToken -> {
                var token by remember(authMethod) { mutableStateOf(authMethod.token) }

                OutlinedTextField(
                    value = token,
                    onValueChange = {
                        token = it
                        onAuthMethodChange(authMethod.copy(token = it))
                    },
                    label = { Text("Bearer Token") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            is AuthMethod.BasicAuth -> {
                var username by remember(authMethod) { mutableStateOf(authMethod.username) }
                var password by remember(authMethod) { mutableStateOf(authMethod.password) }

                OutlinedTextField(
                    value = username,
                    onValueChange = {
                        username = it
                        onAuthMethodChange(authMethod.copy(username = it))
                    },
                    label = { Text("Username") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        onAuthMethodChange(authMethod.copy(password = it))
                    },
                    label = { Text("Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            else -> {
                // No additional fields for None or other auth methods
            }
        }
    }
}