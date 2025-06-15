package com.contextable.agui4k.client.ui.screens.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.contextable.agui4k.client.ui.screens.chat.DisplayMessage
import com.contextable.agui4k.client.ui.screens.chat.MessageRole
import com.contextable.agui4k.client.ui.theme.*
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@Composable
fun MessageBubble(
    message: DisplayMessage,
    showTimestamp: Boolean = true
) {
    val isUser = message.role == MessageRole.USER
    val isError = message.role == MessageRole.ERROR
    val isSystem = message.role == MessageRole.SYSTEM
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
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
            }
            
            if (showTimestamp && !message.isStreaming) {
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
