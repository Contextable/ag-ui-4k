package com.contextable.agui4k.transport

import kotlinx.coroutines.delay
import kotlin.math.min
import kotlin.math.pow
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Defines a retry policy for transport operations.
 */
interface RetryPolicy {
    /**
     * Determines if an exception should trigger a retry.
     */
    fun shouldRetry(exception: Throwable, attemptNumber: Int): Boolean
    
    /**
     * Calculates the delay before the next retry attempt.
     */
    fun calculateDelay(attemptNumber: Int): Duration
    
    /**
     * Maximum number of retry attempts.
     */
    val maxAttempts: Int
}

/**
 * No retry policy - fails immediately on any error.
 */
object NoRetryPolicy : RetryPolicy {
    override fun shouldRetry(exception: Throwable, attemptNumber: Int): Boolean = false
    override fun calculateDelay(attemptNumber: Int): Duration = Duration.ZERO
    override val maxAttempts: Int = 1
}

/**
 * Exponential backoff retry policy with jitter.
 */
class ExponentialBackoffRetryPolicy(
    override val maxAttempts: Int = 3,
    private val baseDelayMs: Long = 1000L,
    private val maxDelayMs: Long = 30000L,
    private val multiplier: Double = 2.0,
    private val jitterFactor: Double = 0.1
) : RetryPolicy {
    
    override fun shouldRetry(exception: Throwable, attemptNumber: Int): Boolean {
        if (attemptNumber >= maxAttempts) return false
        
        return when (exception) {
            is HttpClientTransportException -> exception.isRetryable
            is TransportTimeoutException -> true
            is TransportConnectionException -> true
            is TransportParsingException -> false  // Parsing errors are not retryable
            is RunSessionClosedException -> false  // Session closed is not retryable
            else -> true  // Other exceptions may be transient
        }
    }
    
    override fun calculateDelay(attemptNumber: Int): Duration {
        val exponentialDelay = baseDelayMs * multiplier.pow((attemptNumber - 1).toDouble())
        val cappedDelay = min(exponentialDelay, maxDelayMs.toDouble())
        
        // Add jitter to avoid thundering herd
        val jitter = cappedDelay * jitterFactor * (kotlin.random.Random.nextDouble() - 0.5) * 2
        val finalDelay = cappedDelay + jitter
        
        return finalDelay.toLong().milliseconds
    }
}

/**
 * Fixed delay retry policy.
 */
class FixedDelayRetryPolicy(
    override val maxAttempts: Int = 3,
    private val delay: Duration = 1.seconds
) : RetryPolicy {
    
    override fun shouldRetry(exception: Throwable, attemptNumber: Int): Boolean {
        if (attemptNumber >= maxAttempts) return false
        
        return when (exception) {
            is HttpClientTransportException -> exception.isRetryable
            is TransportTimeoutException -> true
            is TransportConnectionException -> true
            is TransportParsingException -> false
            is RunSessionClosedException -> false
            else -> true
        }
    }
    
    override fun calculateDelay(attemptNumber: Int): Duration = delay
}

/**
 * Executes a block with retry logic according to the given policy.
 */
suspend fun <T> retryWithPolicy(
    retryPolicy: RetryPolicy,
    block: suspend (attemptNumber: Int) -> T
): T {
    var lastException: Throwable? = null
    
    for (attempt in 1..retryPolicy.maxAttempts) {
        try {
            return block(attempt)
        } catch (e: Exception) {
            lastException = e
            
            if (!retryPolicy.shouldRetry(e, attempt)) {
                throw e
            }
            
            if (attempt < retryPolicy.maxAttempts) {
                val delayDuration = retryPolicy.calculateDelay(attempt)
                delay(delayDuration)
            }
        }
    }
    
    // This should never be reached, but just in case
    throw TransportRetryExhaustedException(
        "Retry attempts exhausted after ${retryPolicy.maxAttempts} attempts",
        retryPolicy.maxAttempts,
        lastException ?: Exception("Unknown error")
    )
}