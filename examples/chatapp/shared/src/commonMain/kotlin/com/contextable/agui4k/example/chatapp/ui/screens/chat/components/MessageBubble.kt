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
package com.contextable.agui4k.example.chatapp.ui.screens.chat.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.contextable.agui4k.example.chatapp.ui.screens.chat.DisplayMessage
import com.contextable.agui4k.example.chatapp.ui.screens.chat.MessageRole
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@Composable
fun MessageBubble(
    message: DisplayMessage,
    modifier: Modifier = Modifier
) {
    val isUser = message.role == MessageRole.USER
    val isError = message.role == MessageRole.ERROR
    val isSystem = message.role == MessageRole.SYSTEM
    val isToolCall = message.role == MessageRole.TOOL_CALL
    val isStepInfo = message.role == MessageRole.STEP_INFO
    val isEphemeral = message.ephemeralGroupId != null

    // Enhanced fade-in animation
    val animatedAlpha = remember(message.id) { Animatable(0f) }
    LaunchedEffect(message.id) {
        if (isEphemeral) {
            // Slower, more noticeable fade for ephemeral messages
            animatedAlpha.animateTo(
                targetValue = 0.8f,  // Don't go fully opaque
                animationSpec = tween(
                    durationMillis = 800,  // Slower fade
                    easing = FastOutSlowInEasing
                )
            )
        } else {
            // Quick fade for regular messages
            animatedAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 200)
            )
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .alpha(animatedAlpha.value),  // Apply fade
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 12.dp,
                        topEnd = 12.dp,
                        bottomStart = if (isUser) 12.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 12.dp
                    )
                )
                .background(
                    when {
                        isUser -> MaterialTheme.colorScheme.primary
                        isError -> MaterialTheme.colorScheme.error
                        isSystem -> MaterialTheme.colorScheme.tertiary
                        isToolCall -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
                        isStepInfo -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f)
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                )
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            if (message.isStreaming) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = message.content,
                        style = MaterialTheme.typography.bodyLarge,
                        color = when {
                            isUser -> MaterialTheme.colorScheme.onPrimary
                            isError -> MaterialTheme.colorScheme.onError
                            isSystem -> MaterialTheme.colorScheme.onTertiary
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                    CircularProgressIndicator(
                        modifier = Modifier.size(12.dp),
                        strokeWidth = 2.dp,
                        color = if (isUser) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.primary
                        }
                    )
                }
            } else {
                // Main message text
                if (isEphemeral) {
                    // For ephemeral messages, create a shimmering text
                    val infiniteTransition = rememberInfiniteTransition(label = "textShimmer")
                    val shimmerTranslateAnim by infiniteTransition.animateFloat(
                        initialValue = 0f,
                        targetValue = 200f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(800, easing = LinearEasing),
                            repeatMode = RepeatMode.Restart
                        ),
                        label = "textShimmer"
                    )

                    val textColor = when {
                        isUser -> MaterialTheme.colorScheme.onPrimary
                        isError -> MaterialTheme.colorScheme.onError
                        isSystem -> MaterialTheme.colorScheme.onTertiary
                        isToolCall -> MaterialTheme.colorScheme.onSecondaryContainer
                        isStepInfo -> MaterialTheme.colorScheme.onTertiaryContainer
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }

                    Box {
                        Text(
                            text = message.content,
                            style = MaterialTheme.typography.bodyMedium,
                            color = textColor,
                            modifier = Modifier.drawWithContent {
                                drawContent()

                                // Draw shimmer overlay
                                val shimmerBrush = Brush.linearGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.White.copy(alpha = 0.3f),
                                        Color.Transparent
                                    ),
                                    start = Offset(shimmerTranslateAnim - 100f, 0f),
                                    end = Offset(shimmerTranslateAnim + 100f, 0f)
                                )

                                drawRect(
                                    brush = shimmerBrush,
                                    blendMode = BlendMode.SrcOver
                                )
                            }
                        )
                    }
                } else {
                    // Regular text for non-ephemeral messages
                    Text(
                        text = message.content,
                        style = MaterialTheme.typography.bodyLarge,
                        color = when {
                            isUser -> MaterialTheme.colorScheme.onPrimary
                            isError -> MaterialTheme.colorScheme.onError
                            isSystem -> MaterialTheme.colorScheme.onTertiary
                            isToolCall -> MaterialTheme.colorScheme.onSecondaryContainer
                            isStepInfo -> MaterialTheme.colorScheme.onTertiaryContainer
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }

            // Always show timestamp when message is complete
            if (!message.isStreaming && !isEphemeral) {  // Don't show timestamp for ephemeral messages
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formatTimestamp(message.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = when {
                        isUser -> MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                        isError -> MaterialTheme.colorScheme.onError.copy(alpha = 0.7f)
                        isSystem -> MaterialTheme.colorScheme.onTertiary.copy(alpha = 0.7f)
                        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    },
                    textAlign = if (isUser) TextAlign.End else TextAlign.Start,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun formatTimestamp(timestamp: Long): String {
    val instant = Instant.fromEpochMilliseconds(timestamp)
    val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    return "${localDateTime.hour.toString().padStart(2, '0')}:${localDateTime.minute.toString().padStart(2, '0')}"
}