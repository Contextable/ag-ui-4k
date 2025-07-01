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
package com.contextable.agui4k.core.types

import kotlinx.serialization.json.Json

/**
 * Configured JSON instance for AG-UI protocol serialization.
 *
 * Configuration:
 * - Uses "type" as the class discriminator for polymorphic types
 * - Ignores unknown keys for forward compatibility
 * - Lenient parsing for flexibility
 * - Encodes defaults to ensure protocol compliance
 * - Does NOT include nulls by default (explicitNulls = false)
 */
val AgUiJson by lazy {
    Json {
        serializersModule = AgUiSerializersModule
        ignoreUnknownKeys = true     // Forward compatibility
        isLenient = true             // Allow flexibility in parsing
        encodeDefaults = true        // Ensure all fields are present
        explicitNulls = false        // Don't include null fields
        prettyPrint = false          // Compact output for efficiency
    }
}

/**
 * Pretty-printing JSON instance for debugging.
 */
val AgUiJsonPretty = Json(from = AgUiJson) {
    prettyPrint = true
}