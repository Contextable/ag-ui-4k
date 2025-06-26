// agui4k-agent-sdk/src/commonTest/kotlin/com/contextable/agui4k/sdk/StatefulAgUi4KAgentConfigTest.kt
package com.contextable.agui4k.sdk

import kotlinx.serialization.json.JsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StatefulAgUi4KAgentConfigTest {

    @Test
    fun testDefaultStatefulConfiguration() {
        val config = StatefulAgUi4kAgentConfig()
        
        assertTrue(config.initialState is JsonObject)
        assertTrue((config.initialState as JsonObject).isEmpty())
        assertEquals(100, config.maxHistoryLength)
    }

    @Test
    fun testStatefulConfigurationInheritance() {
        val config = StatefulAgUi4kAgentConfig().apply {
            bearerToken = "stateful-token"
            maxHistoryLength = 50
        }
        
        val headers = config.buildHeaders()
        assertEquals("Bearer stateful-token", headers["Authorization"])
        assertEquals(50, config.maxHistoryLength)
    }
}