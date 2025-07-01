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