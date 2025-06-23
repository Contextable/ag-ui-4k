package com.contextable.agui4k.transport

import com.contextable.agui4k.core.types.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.*

/**
 * Mock implementation of ClientTransport for testing.
 * Captures all parameters passed to startRun and provides configurable responses.
 */
class MockClientTransport(
    private val responseEvents: List<BaseEvent> = emptyList(),
    private val shouldError: Boolean = false,
    private val errorMessage: String = "Mock error"
) : ClientTransport {
    
    // Capture parameters for verification
    var capturedMessages: List<Message>? = null
        private set
    var capturedThreadId: String? = null
        private set
    var capturedRunId: String? = null
        private set
    var capturedState: JsonElement? = null
        private set
    var capturedTools: List<Tool>? = null
        private set
    var capturedContext: List<Context>? = null
        private set
    var capturedForwardedProps: JsonElement? = null
        private set
    
    var startRunCallCount = 0
        private set
    
    override suspend fun startRun(
        messages: List<Message>,
        threadId: String?,
        runId: String?,
        state: JsonElement?,
        tools: List<Tool>?,
        context: List<Context>?,
        forwardedProps: JsonElement?
    ): RunSession {
        // Capture all parameters
        capturedMessages = messages
        capturedThreadId = threadId
        capturedRunId = runId
        capturedState = state
        capturedTools = tools
        capturedContext = context
        capturedForwardedProps = forwardedProps
        startRunCallCount++
        
        if (shouldError) {
            throw TransportConnectionException(errorMessage)
        }
        
        return MockRunSession(responseEvents)
    }
    
    fun reset() {
        capturedMessages = null
        capturedThreadId = null
        capturedRunId = null
        capturedState = null
        capturedTools = null
        capturedContext = null
        capturedForwardedProps = null
        startRunCallCount = 0
    }
}

/**
 * Mock implementation of RunSession for testing.
 */
class MockRunSession(
    private val responseEvents: List<BaseEvent>
) : RunSession {
    private val _isActive = MutableStateFlow(true)
    override val isActive: StateFlow<Boolean> = _isActive.asStateFlow()
    
    private val sentMessages = mutableListOf<Message>()
    val capturedSentMessages: List<Message> get() = sentMessages.toList()
    
    override val events: Flow<BaseEvent> = flow {
        for (event in responseEvents) {
            emit(event)
            if (event is RunFinishedEvent || event is RunErrorEvent) {
                _isActive.value = false
            }
        }
    }
    
    override suspend fun sendMessage(message: Message) {
        if (!isActive.value) {
            throw RunSessionClosedException("Session is closed")
        }
        sentMessages.add(message)
    }
    
    override suspend fun close() {
        _isActive.value = false
    }
}