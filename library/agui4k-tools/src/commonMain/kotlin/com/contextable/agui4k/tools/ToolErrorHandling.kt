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
package com.contextable.agui4k.tools

import com.contextable.agui4k.core.types.ToolCall
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Comprehensive error handling and recovery system for tool execution.
 */
class ToolErrorHandler(
    private val config: ToolErrorConfig = ToolErrorConfig()
) {
    
    private val executionHistory = mutableMapOf<String, MutableList<ToolExecutionAttempt>>()
    private val circuitBreakers = mutableMapOf<String, CircuitBreaker>()
    
    /**
     * Handles a tool execution error and determines the appropriate response.
     * 
     * @param error The error that occurred
     * @param context The execution context
     * @param attempt The current attempt number
     * @return Error handling decision
     */
    suspend fun handleError(
        error: Throwable,
        context: ToolExecutionContext,
        attempt: Int
    ): ToolErrorDecision {
        val toolName = context.toolCall.function.name
        val now = Clock.System.now()
        
        // Record the execution attempt
        recordExecutionAttempt(context, error, attempt, now)
        
        // Check circuit breaker
        val circuitBreaker = getOrCreateCircuitBreaker(toolName)
        if (circuitBreaker.isOpen()) {
            logger.warn { "Circuit breaker is open for tool: $toolName" }
            return ToolErrorDecision.Fail(
                message = "Tool '$toolName' is temporarily unavailable due to repeated failures",
                shouldReport = false
            )
        }
        
        // Determine if we should retry
        val shouldRetry = shouldRetryError(error, context, attempt)
        
        if (shouldRetry) {
            val retryDelay = calculateRetryDelay(attempt)
            logger.info { "Retrying tool execution: $toolName (attempt $attempt) after ${retryDelay}ms" }
            
            return ToolErrorDecision.Retry(
                delayMs = retryDelay,
                maxAttempts = config.maxRetryAttempts
            )
        } else {
            // Record failure in circuit breaker
            circuitBreaker.recordFailure()
            
            val errorCategory = categorizeError(error)
            val userMessage = generateUserFriendlyMessage(error, errorCategory, toolName)
            
            logger.error(error) { "Tool execution failed permanently: $toolName after $attempt attempts" }
            
            return ToolErrorDecision.Fail(
                message = userMessage,
                shouldReport = errorCategory.shouldReport
            )
        }
    }
    
    /**
     * Records a successful tool execution to reset circuit breakers and clear history.
     */
    fun recordSuccess(toolName: String) {
        circuitBreakers[toolName]?.recordSuccess()
        executionHistory[toolName]?.clear()
    }
    
    /**
     * Gets error statistics for a specific tool.
     */
    fun getErrorStats(toolName: String): ToolErrorStats {
        val attempts = executionHistory[toolName] ?: emptyList()
        val circuitBreaker = circuitBreakers[toolName]
        val oneHourAgoMs = Clock.System.now().toEpochMilliseconds() - (60 * 60 * 1000) // 1 hour in ms
        val oneHourAgo = kotlinx.datetime.Instant.fromEpochMilliseconds(oneHourAgoMs)
        
        return ToolErrorStats(
            toolName = toolName,
            totalAttempts = attempts.size,
            recentFailures = attempts.count { it.timestamp > oneHourAgo },
            circuitBreakerState = circuitBreaker?.getState() ?: CircuitBreakerState.CLOSED,
            lastErrorTime = attempts.maxByOrNull { it.timestamp }?.timestamp
        )
    }
    
    /**
     * Resets all error state for a tool (useful for manual recovery).
     */
    fun resetErrorState(toolName: String) {
        executionHistory[toolName]?.clear()
        circuitBreakers[toolName]?.reset()
        logger.info { "Reset error state for tool: $toolName" }
    }
    
    private fun shouldRetryError(error: Throwable, context: ToolExecutionContext, attempt: Int): Boolean {
        // Don't retry if we've exceeded max attempts
        if (attempt >= config.maxRetryAttempts) {
            return false
        }
        
        // Check error type for retry eligibility
        return when (error) {
            is ToolNotFoundException -> false // Tool doesn't exist, no point in retrying
            is ToolValidationException -> false // Validation errors are permanent
            is IllegalStateException -> false // Security violations are permanent
            is ToolTimeoutException -> true // Timeouts can be transient
            is ToolNetworkException -> true // Network issues can be transient
            is ToolResourceException -> config.retryOnResourceErrors // Configurable
            else -> config.retryOnUnknownErrors // Configurable
        }
    }
    
    private fun calculateRetryDelay(attempt: Int): Long {
        return when (config.retryStrategy) {
            RetryStrategy.FIXED -> config.baseRetryDelayMs
            RetryStrategy.LINEAR -> config.baseRetryDelayMs * attempt
            RetryStrategy.EXPONENTIAL -> {
                val delay = config.baseRetryDelayMs * (1 shl (attempt - 1))
                minOf(delay, config.maxRetryDelayMs)
            }
            RetryStrategy.EXPONENTIAL_JITTER -> {
                val delay = config.baseRetryDelayMs * (1 shl (attempt - 1))
                val jitter = (delay * 0.1 * kotlin.random.Random.nextDouble()).toLong()
                minOf(delay + jitter, config.maxRetryDelayMs)
            }
        }
    }
    
    private fun categorizeError(error: Throwable): ErrorCategory {
        return when (error) {
            is ToolNotFoundException -> ErrorCategory.CONFIGURATION_ERROR
            is ToolValidationException -> ErrorCategory.USER_ERROR
            is IllegalStateException -> ErrorCategory.SECURITY_ERROR
            is ToolTimeoutException -> ErrorCategory.TRANSIENT_ERROR
            is ToolNetworkException -> ErrorCategory.TRANSIENT_ERROR
            is ToolResourceException -> ErrorCategory.RESOURCE_ERROR
            else -> ErrorCategory.UNKNOWN_ERROR
        }
    }
    
    private fun generateUserFriendlyMessage(error: Throwable, category: ErrorCategory, toolName: String): String {
        return when (category) {
            ErrorCategory.CONFIGURATION_ERROR -> 
                "The tool '$toolName' is not properly configured or is unavailable."
            ErrorCategory.USER_ERROR -> 
                "Invalid parameters provided to tool '$toolName': ${error.message}"
            ErrorCategory.SECURITY_ERROR -> 
                "Access denied for tool '$toolName'. Please check permissions."
            ErrorCategory.TRANSIENT_ERROR -> 
                "Tool '$toolName' is temporarily unavailable. Please try again later."
            ErrorCategory.RESOURCE_ERROR -> 
                "Tool '$toolName' failed due to resource constraints. Please try again later."
            ErrorCategory.UNKNOWN_ERROR -> 
                "Tool '$toolName' encountered an unexpected error: ${error.message}"
        }
    }
    
    private fun recordExecutionAttempt(
        context: ToolExecutionContext,
        error: Throwable,
        attempt: Int,
        timestamp: Instant
    ) {
        val toolName = context.toolCall.function.name
        val attempts = executionHistory.getOrPut(toolName) { mutableListOf() }
        
        attempts.add(ToolExecutionAttempt(
            toolCall = context.toolCall,
            error = error,
            attempt = attempt,
            timestamp = timestamp
        ))
        
        // Limit history size
        if (attempts.size > config.maxHistorySize) {
            attempts.removeAt(0)
        }
    }
    
    private fun getOrCreateCircuitBreaker(toolName: String): CircuitBreaker {
        return circuitBreakers.getOrPut(toolName) {
            CircuitBreaker(config.circuitBreakerConfig)
        }
    }
}

/**
 * Configuration for tool error handling.
 */
data class ToolErrorConfig(
    val maxRetryAttempts: Int = 3,
    val baseRetryDelayMs: Long = 1000L,
    val maxRetryDelayMs: Long = 30000L,
    val retryStrategy: RetryStrategy = RetryStrategy.EXPONENTIAL_JITTER,
    val retryOnResourceErrors: Boolean = true,
    val retryOnUnknownErrors: Boolean = false,
    val maxHistorySize: Int = 100,
    val circuitBreakerConfig: CircuitBreakerConfig = CircuitBreakerConfig()
)

/**
 * Circuit breaker configuration.
 */
data class CircuitBreakerConfig(
    val failureThreshold: Int = 5,
    val recoveryTimeoutMs: Long = 60000L, // 1 minute
    val successThreshold: Int = 2
)

/**
 * Retry strategies for tool execution.
 */
enum class RetryStrategy {
    FIXED,
    LINEAR,
    EXPONENTIAL,
    EXPONENTIAL_JITTER
}

/**
 * Error categories for classification.
 */
enum class ErrorCategory(val shouldReport: Boolean) {
    CONFIGURATION_ERROR(true),
    USER_ERROR(false),
    SECURITY_ERROR(true),
    TRANSIENT_ERROR(false),
    RESOURCE_ERROR(true),
    UNKNOWN_ERROR(true)
}

/**
 * Decision on how to handle a tool error.
 */
sealed class ToolErrorDecision {
    data class Retry(
        val delayMs: Long,
        val maxAttempts: Int
    ) : ToolErrorDecision()
    
    data class Fail(
        val message: String,
        val shouldReport: Boolean
    ) : ToolErrorDecision()
}

/**
 * Statistics about tool execution errors.
 */
data class ToolErrorStats(
    val toolName: String,
    val totalAttempts: Int,
    val recentFailures: Int,
    val circuitBreakerState: CircuitBreakerState,
    val lastErrorTime: Instant?
)

/**
 * Record of a tool execution attempt.
 */
data class ToolExecutionAttempt(
    val toolCall: ToolCall,
    val error: Throwable,
    val attempt: Int,
    val timestamp: Instant
)

/**
 * Circuit breaker states.
 */
enum class CircuitBreakerState {
    CLOSED,    // Normal operation
    OPEN,      // Failing fast
    HALF_OPEN  // Testing recovery
}

/**
 * Simple circuit breaker implementation for tool execution.
 */
class CircuitBreaker(private val config: CircuitBreakerConfig) {
    
    private var _state = CircuitBreakerState.CLOSED
    private var failures = 0
    private var successes = 0
    private var lastFailureTime: Instant? = null
    
    fun getState(): CircuitBreakerState = _state
    
    fun isOpen(): Boolean {
        return when (_state) {
            CircuitBreakerState.OPEN -> {
                val lastFailure = lastFailureTime
                if (lastFailure != null && 
                    Clock.System.now().toEpochMilliseconds() - lastFailure.toEpochMilliseconds() > config.recoveryTimeoutMs) {
                    // Transition to half-open for testing
                    _state = CircuitBreakerState.HALF_OPEN
                    false
                } else {
                    true
                }
            }
            CircuitBreakerState.HALF_OPEN -> false
            CircuitBreakerState.CLOSED -> false
        }
    }
    
    fun recordFailure() {
        failures++
        lastFailureTime = Clock.System.now()
        
        when (_state) {
            CircuitBreakerState.CLOSED -> {
                if (failures >= config.failureThreshold) {
                    _state = CircuitBreakerState.OPEN
                    logger.warn { "Circuit breaker opened after $failures failures" }
                }
            }
            CircuitBreakerState.HALF_OPEN -> {
                _state = CircuitBreakerState.OPEN
                logger.warn { "Circuit breaker reopened after failure during recovery" }
            }
            CircuitBreakerState.OPEN -> {
                // Already open, just update counters
            }
        }
    }
    
    fun recordSuccess() {
        when (_state) {
            CircuitBreakerState.CLOSED -> {
                // Reset failure count on success
                failures = 0
            }
            CircuitBreakerState.HALF_OPEN -> {
                successes++
                if (successes >= config.successThreshold) {
                    _state = CircuitBreakerState.CLOSED
                    failures = 0
                    successes = 0
                    logger.info { "Circuit breaker closed after recovery" }
                }
            }
            CircuitBreakerState.OPEN -> {
                // Should not happen, but reset state
                _state = CircuitBreakerState.CLOSED
                failures = 0
                successes = 0
            }
        }
    }
    
    fun reset() {
        _state = CircuitBreakerState.CLOSED
        failures = 0
        successes = 0
        lastFailureTime = null
    }
}

// Specific tool exception types for better error handling

open class ToolValidationException(message: String, cause: Throwable? = null) : Exception(message, cause)

open class ToolTimeoutException(message: String, cause: Throwable? = null) : Exception(message, cause)

open class ToolNetworkException(message: String, cause: Throwable? = null) : Exception(message, cause)

open class ToolResourceException(message: String, cause: Throwable? = null) : Exception(message, cause)