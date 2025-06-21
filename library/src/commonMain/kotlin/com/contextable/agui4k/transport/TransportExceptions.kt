package com.contextable.agui4k.transport

import io.ktor.http.*

/**
 * Base exception for all transport-related errors.
 */
abstract class TransportException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)

/**
 * Exception thrown when a transport connection fails.
 */
class TransportConnectionException(
    message: String,
    cause: Throwable? = null
) : TransportException(message, cause)

/**
 * Exception thrown when a transport operation times out.
 */
class TransportTimeoutException(
    message: String,
    val timeoutMillis: Long,
    cause: Throwable? = null
) : TransportException(message, cause)

/**
 * Exception thrown when transport data cannot be parsed.
 */
class TransportParsingException(
    message: String,
    val rawData: String? = null,
    cause: Throwable? = null
) : TransportException(message, cause)

/**
 * Exception thrown for HTTP-specific transport errors.
 */
class HttpClientTransportException(
    message: String,
    val statusCode: HttpStatusCode? = null,
    val responseBody: String? = null,
    cause: Throwable? = null
) : TransportException(message, cause) {
    
    val isClientError: Boolean
        get() = statusCode?.value in 400..499
    
    val isServerError: Boolean
        get() = statusCode?.value in 500..599
    
    val isRetryable: Boolean
        get() = when {
            isClientError -> false  // Client errors are not retryable
            isServerError -> true   // Server errors may be transient
            statusCode == null -> true  // Unknown errors may be transient
            else -> false
        }
}

/**
 * Exception thrown when a run session is used after it has been closed.
 */
class RunSessionClosedException(
    message: String = "Run session has been closed"
) : TransportException(message)

/**
 * Exception thrown when the maximum number of retry attempts is exceeded.
 */
class TransportRetryExhaustedException(
    message: String,
    val attemptCount: Int,
    val lastException: Throwable
) : TransportException(message, lastException)