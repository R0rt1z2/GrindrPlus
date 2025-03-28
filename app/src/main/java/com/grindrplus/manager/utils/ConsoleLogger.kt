package com.grindrplus.manager.utils

import android.content.Intent
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
import kotlinx.coroutines.launch

data class LogEntry(
    val timestamp: Long = System.currentTimeMillis(),
    var message: String,
    val type: LogType = LogType.INFO,
)

enum class LogType {
    INFO, SUCCESS, WARNING, ERROR, DEBUG
}

@Composable
fun ConsoleOutput(
    logEntries: List<LogEntry>,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(logEntries.size) {
        if (logEntries.isNotEmpty()) {
            coroutineScope.launch {
                listState.animateScrollToItem(logEntries.size - 1)
            }
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Logs",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = "${logEntries.size} entries",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant,
                thickness = 1.dp
            )

            if (logEntries.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No log entries yet",
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.wrapContentHeight()
                ) {
                    items(logEntries) { entry ->
                        LogEntryItem(entry)
                    }

                    item {
                        Divider(
                            color = MaterialTheme.colorScheme.outlineVariant,
                            thickness = 1.dp
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(
                                onClick = {
                                    val shareText = logEntries.joinToString("\n") { it.message }
                                    val intent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, shareText)
                                        putExtra(Intent.EXTRA_SUBJECT, "GrindrPlus Logs")
                                    }

                                    context.startActivity(
                                        Intent.createChooser(
                                            intent,
                                            "Share logs"
                                        )
                                    )
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Share logs",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Share")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LogEntryItem(entry: LogEntry) {
    val logColor = when (entry.type) {
        // TODO: Move to Theme.kt
        LogType.SUCCESS -> Color(0xFF4CAF50)
        LogType.WARNING -> Color(0xFFFFC107)
        LogType.ERROR -> Color(0xFFE91E63)
        LogType.DEBUG -> Color(0xFF9C27B0)
        LogType.INFO -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = entry.message,
            color = logColor,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.weight(1f)
        )
    }
}

object ConsoleLogger {
    fun log(message: String, type: LogType = LogType.INFO): LogEntry {
        return LogEntry(
            timestamp = System.currentTimeMillis(),
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