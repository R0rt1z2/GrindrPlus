package com.grindrplus.manager.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.ui.draw.clip
import androidx.core.content.ContextCompat.getSystemService
import com.grindrplus.manager.utils.uploadAndShare
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


@Composable
fun ConsoleOutput(
    logEntries: List<LogEntry>,
    modifier: Modifier = Modifier,
    onClear: (() -> Unit)? = null,
) {
    val listState = rememberLazyListState()
    val context = LocalContext.current
    var showCopiedToast by remember { mutableStateOf(false) }
    val logs = logEntries.joinToString("\n") { it.message }
    var uploadingLogs by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(logEntries.size) {
        if (logEntries.isNotEmpty()) {
            listState.animateScrollToItem(logEntries.size - 1)
        }
    }

    LaunchedEffect(showCopiedToast) {
        if (showCopiedToast) {
            delay(2000)
            showCopiedToast = false
        }
    }

    Column(modifier = modifier) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Logs (${logEntries.size} entries)",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.weight(1f))

                IconButton(
                    onClick = {
                        val clipboard = getSystemService(context, ClipboardManager::class.java)
                        val clip = ClipData.newPlainText("Console Logs", logs)
                        clipboard?.setPrimaryClip(clip)
                        showCopiedToast = true
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy logs",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = {
                        if (uploadingLogs) return@IconButton
                        uploadingLogs = true
                        scope.launch {
                            Toast.makeText(context, "Uploading logs...", Toast.LENGTH_SHORT).show()
                            uploadAndShare(logs, context)
                            delay(1000) // Prevent from spamming upload button
                        }.invokeOnCompletion {
                            uploadingLogs = false // should be always invoked
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share logs",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (onClear != null) {
                    IconButton(onClick = onClear) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear logs",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    shape = RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp)
                )
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                state = listState
            ) {
                items(logEntries) { entry ->
                    LogEntryItem(entry)
                }
            }

            if (showCopiedToast) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "Copied to clipboard",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}

@Composable
internal fun LogEntryItem(entry: LogEntry) {
    val logColor = when (entry.type) {
        // TODO: Move to Theme.kt
        LogType.SUCCESS -> Color(0xFF4CAF50)
        LogType.WARNING -> Color(0xFFFFC107)
        LogType.ERROR -> Color(0xFFE91E63)
        LogType.DEBUG -> Color(0xFF9C27B0)
        LogType.VERBOSE -> Color(0xFF757575)
        LogType.INFO -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Column {
        val progressBar =
            "<progressBar:([0-9.]+):>".toRegex().find(entry.message)?.groupValues?.get(1)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = entry.message.replace("<progressBar:$progressBar:>", ""),
                color = logColor,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.weight(1f)
            )
        }

        val progress = progressBar?.toFloatOrNull()
        if (progress != null) {
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = logColor,
                trackColor = logColor.copy(alpha = 0.3f),
            )
        }
    }
}

object ConsoleLogger {
    fun log(message: String, type: LogType = LogType.INFO): LogEntry {
        return LogEntry(
            timestamp = System.currentTimeMillis().toString(),
            message = message,
            type = type
        )
    }

    fun parseLogType(message: String): LogType {
        return when {
            message.startsWith("ERROR:") || message.contains("error", ignoreCase = true) ||
                    message.contains("failed", ignoreCase = true) || message.contains(
                "exception",
                ignoreCase = true
            ) ->
                LogType.ERROR

            message.startsWith("WARNING:") || message.contains("warning", ignoreCase = true) ||
                    message.contains("stalled", ignoreCase = true) ->
                LogType.WARNING

            message.startsWith("DEBUG:") || message.startsWith("LSPOSED D") ->
                LogType.DEBUG

            message.contains("complete", ignoreCase = true) || message.contains(
                "success",
                ignoreCase = true
            ) ||
                    message.contains("finished", ignoreCase = true) || message.contains("100%") ->
                LogType.SUCCESS

            else ->
                LogType.INFO
        }
    }
}