package com.contextable.agui4k.client.data.model

import kotlinx.serialization.Serializable

/**
 * Represents different authentication methods supported by agents.
 */
@Serializable
sealed class AuthMethod {
    @Serializable
    data class None(val id: String = "none") : AuthMethod()
    
    @Serializable
    data class ApiKey(
        val key: String,
        val headerName: String = "X-API-Key"
    ) : AuthMethod()
    
    @Serializable
    data class BearerToken(
        val token: String
    ) : AuthMethod()
    
    @Serializable
    data class BasicAuth(
        val username: String,
        val password: String
    ) : AuthMethod()
    
    @Serializable
    data class OAuth2(
        val clientId: String,
        val clientSecret: String? = null,
        val authorizationUrl: String,
        val tokenUrl: String,
        val scopes: List<String> = emptyList(),
        val accessToken: String? = null,
        val refreshToken: String? = null
    ) : AuthMethod()
    
    @Serializable
    data class Custom(
        val type: String,
        val config: Map<String, String>
    ) : AuthMethod()
}
