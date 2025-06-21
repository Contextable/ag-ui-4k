package com.contextable.agui4k.transport

import io.ktor.http.*
import kotlin.test.*

class TransportExceptionsTest {
    
    @Test
    fun testTransportException() {
        val exception = object : TransportException("test message") {}
        
        assertEquals("test message", exception.message)
        assertNull(exception.cause)
    }
    
    @Test
    fun testTransportConnectionException() {
        val cause = RuntimeException("connection failed")
        val exception = TransportConnectionException("Connection lost", cause)
        
        assertEquals("Connection lost", exception.message)
        assertEquals(cause, exception.cause)
    }
    
    @Test
    fun testTransportTimeoutException() {
        val exception = TransportTimeoutException("Request timed out", 30000L)
        
        assertEquals("Request timed out", exception.message)
        assertEquals(30000L, exception.timeoutMillis)
        assertNull(exception.cause)
    }
    
    @Test
    fun testTransportParsingException() {
        val rawData = """{"invalid": json}"""
        val cause = RuntimeException("parse error")
        val exception = TransportParsingException("Failed to parse", rawData, cause)
        
        assertEquals("Failed to parse", exception.message)
        assertEquals(rawData, exception.rawData)
        assertEquals(cause, exception.cause)
    }
    
    @Test
    fun testHttpClientTransportException() {
        val responseBody = """{"error": "invalid request"}"""
        val exception = HttpClientTransportException(
            "HTTP error",
            HttpStatusCode.BadRequest,
            responseBody
        )
        
        assertEquals("HTTP error", exception.message)
        assertEquals(HttpStatusCode.BadRequest, exception.statusCode)
        assertEquals(responseBody, exception.responseBody)
        assertTrue(exception.isClientError)
        assertFalse(exception.isServerError)
        assertFalse(exception.isRetryable)
    }
    
    @Test
    fun testHttpClientTransportExceptionServerError() {
        val exception = HttpClientTransportException(
            "Server error",
            HttpStatusCode.InternalServerError
        )
        
        assertFalse(exception.isClientError)
        assertTrue(exception.isServerError)
        assertTrue(exception.isRetryable)
    }
    
    @Test
    fun testHttpClientTransportExceptionWithoutStatusCode() {
        val exception = HttpClientTransportException("Unknown error")
        
        assertFalse(exception.isClientError)
        assertFalse(exception.isServerError)
        assertTrue(exception.isRetryable)  // Unknown errors are considered retryable
    }
    
    @Test
    fun testRunSessionClosedException() {
        val exception = RunSessionClosedException()
        
        assertEquals("Run session has been closed", exception.message)
        
        val customException = RunSessionClosedException("Custom message")
        assertEquals("Custom message", customException.message)
    }
    
    @Test
    fun testTransportRetryExhaustedException() {
        val lastException = TransportTimeoutException("timeout", 5000L)
        val exception = TransportRetryExhaustedException(
            "All retries failed",
            3,
            lastException
        )
        
        assertEquals("All retries failed", exception.message)
        assertEquals(3, exception.attemptCount)
        assertEquals(lastException, exception.lastException)
        assertEquals(lastException, exception.cause)
    }
}