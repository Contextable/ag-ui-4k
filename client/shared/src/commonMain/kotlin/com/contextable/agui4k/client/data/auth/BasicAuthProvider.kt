package com.contextable.agui4k.client.data.auth

import com.contextable.agui4k.client.data.model.AuthMethod
import kotlinx.io.bytestring.encodeToByteString

class BasicAuthProvider : AuthProvider {
    override fun canHandle(authMethod: AuthMethod): Boolean {
        return authMethod is AuthMethod.BasicAuth
    }
    
    override suspend fun applyAuth(authMethod: AuthMethod, headers: MutableMap<String, String>) {
        when (authMethod) {
            is AuthMethod.BasicAuth -> {
                val credentials = "${authMethod.username}:${authMethod.password}"
                val encoded = credentials.encodeToByteArray().encodeBase64()
                headers["Authorization"] = "Basic $encoded"
            }
            else -> throw IllegalArgumentException("Unsupported auth method")
        }
    }
    
    override suspend fun refreshAuth(authMethod: AuthMethod): AuthMethod {
        return authMethod
    }
    
    override suspend fun isAuthValid(authMethod: AuthMethod): Boolean {
        return authMethod is AuthMethod.BasicAuth && 
               authMethod.username.isNotBlank() && 
               authMethod.password.isNotBlank()
    }
}

private fun ByteArray.encodeBase64(): String {
    val table = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"
    val output = StringBuilder()
    var padding = 0
    
    for (i in indices step 3) {
        val b1 = this[i].toInt() and 0xFF
        val b2 = if (i + 1 < size) this[i + 1].toInt() and 0xFF else 0.also { padding++ }
        val b3 = if (i + 2 < size) this[i + 2].toInt() and 0xFF else 0.also { padding++ }
        
        val triple = (b1 shl 16) or (b2 shl 8) or b3
        
        output.append(table[(triple shr 18) and 0x3F])
        output.append(table[(triple shr 12) and 0x3F])
        output.append(if (padding < 2) table[(triple shr 6) and 0x3F] else '=')
        output.append(if (padding < 1) table[triple and 0x3F] else '=')
    }
    
    return output.toString()
}
