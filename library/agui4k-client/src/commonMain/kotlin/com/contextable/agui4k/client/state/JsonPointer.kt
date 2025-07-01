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
package com.contextable.agui4k.client.state

import kotlinx.serialization.json.*

/**
 * JSON Pointer utilities for path evaluation.
 * Implements RFC 6901 JSON Pointer specification.
 */
object JsonPointer {

    /**
     * Evaluates a JSON Pointer path against a JSON element.
     *
     * @param element The JSON element to evaluate against
     * @param path The JSON Pointer path (e.g., "/foo/bar/0")
     * @return The element at the path, or null if not found
     */
    fun evaluate(element: JsonElement, path: String): JsonElement? {
        if (path.isEmpty() || path == "/") return element

        // Split path and decode segments
        val segments = path.trimStart('/').split('/')
            .map { decodeSegment(it) }

        // Navigate through the JSON structure
        return segments.fold(element as JsonElement?) { current, segment ->
            when (current) {
                is JsonObject -> current[segment]
                is JsonArray -> {
                    val index = segment.toIntOrNull()
                    if (index != null && index in 0 until current.size) {
                        current[index]
                    } else {
                        null
                    }
                }
                else -> null
            }
        }
    }

    /**
     * Decodes a JSON Pointer segment.
     * Handles escape sequences: ~0 -> ~ and ~1 -> /
     */
    private fun decodeSegment(segment: String): String {
        return segment
            .replace("~1", "/")
            .replace("~0", "~")
    }

    /**
     * Encodes a string for use as a JSON Pointer segment.
     */
    fun encodeSegment(segment: String): String {
        return segment
            .replace("~", "~0")
            .replace("/", "~1")
    }

    /**
     * Creates a JSON Pointer path from segments.
     */
    fun createPath(vararg segments: String): String {
        return "/" + segments.joinToString("/") { encodeSegment(it) }
    }
}