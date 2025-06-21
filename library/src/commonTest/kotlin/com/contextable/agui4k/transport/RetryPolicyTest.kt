package com.contextable.agui4k.transport

import io.ktor.http.*
import kotlinx.coroutines.test.runTest
import kotlin.test.*
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class RetryPolicyTest {
    
    @Test
    fun testNoRetryPolicy() {
        val policy = NoRetryPolicy
        
        assertEquals(1, policy.maxAttempts)
        assertFalse(policy.shouldRetry(RuntimeException("test"), 1))
        assertEquals(0.milliseconds, policy.calculateDelay(1))
    }
    
    @Test
    fun testExponentialBackoffRetryPolicy() {
        val policy = ExponentialBackoffRetryPolicy(
            maxAttempts = 3,
            baseDelayMs = 1000L,
            maxDelayMs = 10000L,
            multiplier = 2.0,
            jitterFactor = 0.0  // No jitter for predictable testing
        )
        
        assertEquals(3, policy.maxAttempts)
        
        // Test retryable exceptions
        assertTrue(policy.shouldRetry(TransportTimeoutException("timeout", 5000L), 1))
        assertTrue(policy.shouldRetry(TransportConnectionException("connection failed"), 2))
        
        // Test non-retryable exceptions
        assertFalse(policy.shouldRetry(TransportParsingException("parse error"), 1))
        assertFalse(policy.shouldRetry(RunSessionClosedException(), 1))
        
        // Test max attempts
        assertFalse(policy.shouldRetry(TransportTimeoutException("timeout", 5000L), 3))
        
        // Test delay calculation (without jitter)
        assertEquals(1000.milliseconds, policy.calculateDelay(1))
        assertEquals(2000.milliseconds, policy.calculateDelay(2))
        assertEquals(4000.milliseconds, policy.calculateDelay(3))
    }
    
    @Test
    fun testHttpClientTransportExceptionRetryability() {
        val policy = ExponentialBackoffRetryPolicy()
        
        // Client errors (4xx) should not be retryable
        val clientError = HttpClientTransportException(
            "Bad request",
            HttpStatusCode.BadRequest
        )
        assertFalse(policy.shouldRetry(clientError, 1))
        
        // Server errors (5xx) should be retryable
        val serverError = HttpClientTransportException(
            "Internal server error",
            HttpStatusCode.InternalServerError
        )
        assertTrue(policy.shouldRetry(serverError, 1))
        
        // Unknown status should be retryable
        val unknownError = HttpClientTransportException("Unknown error")
        assertTrue(policy.shouldRetry(unknownError, 1))
    }
    
    @Test
    fun testFixedDelayRetryPolicy() {
        val policy = FixedDelayRetryPolicy(
            maxAttempts = 2,
            delay = 500.milliseconds
        )
        
        assertEquals(2, policy.maxAttempts)
        assertEquals(500.milliseconds, policy.calculateDelay(1))
        assertEquals(500.milliseconds, policy.calculateDelay(2))
        assertEquals(500.milliseconds, policy.calculateDelay(5)) // Same delay regardless of attempt
        
        assertTrue(policy.shouldRetry(TransportTimeoutException("timeout", 1000L), 1))
        assertFalse(policy.shouldRetry(TransportTimeoutException("timeout", 1000L), 2))
    }
    
    @Test
    fun testRetryWithPolicySuccess() = runTest {
        var attempts = 0
        
        val result = retryWithPolicy(ExponentialBackoffRetryPolicy(maxAttempts = 3)) { attemptNumber ->
            attempts++
            assertEquals(attemptNumber, attempts)
            "success"
        }
        
        assertEquals("success", result)
        assertEquals(1, attempts)
    }
    
    @Test
    fun testRetryWithPolicySuccessAfterFailures() = runTest {
        var attempts = 0
        
        val result = retryWithPolicy(ExponentialBackoffRetryPolicy(maxAttempts = 3)) { attemptNumber ->
            attempts++
            if (attempts < 3) {
                throw TransportTimeoutException("timeout", 1000L)
            }
            "success"
        }
        
        assertEquals("success", result)
        assertEquals(3, attempts)
    }
    
    @Test
    fun testRetryWithPolicyNonRetryableException() = runTest {
        var attempts = 0
        
        assertFailsWith<TransportParsingException> {
            retryWithPolicy(ExponentialBackoffRetryPolicy(maxAttempts = 3)) { attemptNumber ->
                attempts++
                throw TransportParsingException("parse error")
            }
        }
        
        assertEquals(1, attempts) // Should not retry parsing errors
    }
    
    @Test
    fun testRetryWithPolicyExhaustion() = runTest {
        var attempts = 0
        
        // When retry attempts are exhausted, it throws the original exception
        assertFailsWith<TransportTimeoutException> {
            retryWithPolicy(ExponentialBackoffRetryPolicy(maxAttempts = 2)) { attemptNumber ->
                attempts++
                throw TransportTimeoutException("timeout", 1000L)
            }
        }
        
        assertEquals(2, attempts)
    }
}