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

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AgUi4KAgentConfigTest {

    @Test
    fun testDefaultConfiguration() {
        val config = AgUi4kAgentConfig()
        
        assertEquals("X-API-Key", config.apiKeyHeader)
        assertEquals(false, config.debug)
        assertEquals(600_000L, config.requestTimeout)
        assertEquals(30_000L, config.connectTimeout)
        assertNotNull(config.headers)
        assertTrue(config.headers.isEmpty())
        assertTrue(config.context.isEmpty())
    }

    @Test
    fun testBuildHeadersWithBearerToken() {
        val config = AgUi4kAgentConfig().apply {
            bearerToken = "test-token"
        }
        
        val headers = config.buildHeaders()
        assertEquals("Bearer test-token", headers["Authorization"])
    }

    @Test
    fun testBuildHeadersWithApiKey() {
        val config = AgUi4kAgentConfig().apply {
            apiKey = "test-api-key"
            apiKeyHeader = "X-Custom-Key"
        }
        
        val headers = config.buildHeaders()
        assertEquals("test-api-key", headers["X-Custom-Key"])
    }

    @Test
    fun testBuildHeadersWithCustomHeaders() {
        val config = AgUi4kAgentConfig().apply {
            headers["Custom-Header"] = "custom-value"
        }
        
        val headers = config.buildHeaders()
        assertEquals("custom-value", headers["Custom-Header"])
    }
}