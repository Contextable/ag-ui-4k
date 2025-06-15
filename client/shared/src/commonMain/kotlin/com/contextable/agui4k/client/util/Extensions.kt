package com.contextable.agui4k.client.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.flow.StateFlow
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * Extension function to safely collect StateFlow as State in Compose.
 */
@Composable
fun <T> StateFlow<T>.collectAsStateWithLifecycle(
    context: CoroutineContext = EmptyCoroutineContext
): State<T> = collectAsState(context)

/**
 * Extension function to format file sizes.
 */
fun Long.formatFileSize(): String {
    if (this <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (kotlin.math.log10(this.toDouble()) / kotlin.math.log10(1024.0)).toInt()
    return "%.1f %s".format(this / kotlin.math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
}

/**
 * Extension function to truncate strings with ellipsis.
 */
fun String.truncate(maxLength: Int): String {
    return if (length <= maxLength) this else "${substring(0, maxLength - 3)}..."
}