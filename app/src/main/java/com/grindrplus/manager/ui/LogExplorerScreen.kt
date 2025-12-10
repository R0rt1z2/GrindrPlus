package com.grindrplus.manager.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.core.net.toUri
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.grindrplus.BuildConfig
import com.grindrplus.core.Config
import com.grindrplus.core.Logger
import com.grindrplus.manager.utils.FileOperationHandler
import com.grindrplus.manager.utils.uploadAndShare
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.file.FileSystems

enum class LogType {
    INFO, WARNING, ERROR, DEBUG, VERBOSE, SUCCESS
}

data class LogEntry(
    val timestamp: String?,
    val message: String,
    val type: LogType
)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugLogsScreen(
    onBack: () -> Unit,
) {
    var logs by remember { mutableStateOf(emptyList<LogEntry>()) }
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showExportDialog by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }

    var debugModeEnabled by remember {
        mutableStateOf(Config.get("debug_mode", false) as Boolean)
    }

    val isDebugBuild = BuildConfig.DEBUG

    BackHandler(onBack = onBack)

    LaunchedEffect(Unit) {
        launch {
            withContext(Dispatchers.IO) {
                val log = File(context.getExternalFilesDir(null), "grindrplus.log")

                fun parseLogs(logs: List<String>) =
                    logs.map {
                        LogEntry(
                            timestamp = null,
                            message = it,
                            type = when {
                                it.startsWith("I") -> LogType.INFO
                                it.startsWith("W") -> LogType.WARNING
                                it.startsWith("E") -> LogType.ERROR
                                it.startsWith("D") -> LogType.DEBUG
                                it.startsWith("V") -> LogType.VERBOSE
                                else -> LogType.INFO
                            }
                        )
                    }

                logs = parseLogs(log.readLines())

                val watchService = FileSystems.getDefault().newWatchService()

                log.toPath().parent.register(
                    watchService,
                    java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY
                )

                while (true) {
                    val key = watchService.take()

                    for (event in key.pollEvents()) {
                        if (event.context() == log.name) {
                            val newLog = log.readLines()
                            logs = parseLogs(newLog)
                        }
                    }

                    key.reset()
                }
            }
        }
    }

    if (showExportDialog) {
        ExportLogsDialog(
            onDismissRequest = { showExportDialog = false },
            onZipExport = {
                scope.launch {
                    try {
                        val zipFile = FileOperationHandler.createLogsZip(context)
                        if (zipFile != null) {
                            FileOperationHandler.exportZipFile(
                                "grindrplus_logs.zip",
                                zipFile
                            )
                        } else {
                            snackbarHostState.showSnackbar("Failed to create logs package")
                        }
                    } catch (e: Exception) {
                        snackbarHostState.showSnackbar("Error exporting logs: ${e.message}")
                    }
                    showExportDialog = false
                }
            },
            onUrlExport = {
                showExportDialog = false

                scope.launch(Dispatchers.IO) {
                    try {
                        withContext(Dispatchers.Main) {
                            snackbarHostState.showSnackbar("Generating URL... This may take a while.")
                        }

                        val log = File(context.getExternalFilesDir(null), "grindrplus.log")
                        val logContent = log.readText()

                        uploadAndShare(logContent, context)
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            snackbarHostState.showSnackbar("Error: ${e.message ?: "Unknown error"}")
                        }
                    }
                }
            }
        )
    }

    if (showReportDialog) {
        ReportIssueDialog(
            onDismiss = { showReportDialog = false },
            onOpenGitHub = {
                val intent = Intent(Intent.ACTION_VIEW, "https://github.com/R0rt1z2/GrindrPlus/issues".toUri())
                context.startActivity(intent)
                showReportDialog = false
            }
        )
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.padding(bottom = 80.dp)
            )
        },
        topBar = {
            TopAppBar(
                title = { Text("Logs") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Back",
                            modifier = Modifier.rotate(180f)
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            if (!isDebugBuild) {
                                val newState = !debugModeEnabled
                                debugModeEnabled = newState
                                Config.put("debug_mode", newState)
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        if (newState) "Verbose logging enabled" else "Verbose logging disabled"
                                    )
                                }
                            }
                        },
                        enabled = !isDebugBuild
                    ) {
                        Box {
                            Icon(
                                imageVector = Icons.Default.Code,
                                contentDescription = "Toggle Verbose Logging",
                                tint = if (isDebugBuild || debugModeEnabled)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            if (isDebugBuild) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                                        .align(Alignment.TopEnd)
                                )
                            }
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            LogsViewer(
                logs = logs,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(
                    onClick = { showExportDialog = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Export Logs")
                }

                Spacer(modifier = Modifier.padding(horizontal = 8.dp))

                Button(
                    onClick = {
                        logs = emptyList()
                        Logger.clearLogs()
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                ) {
                    Text("Clear Logs")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = { showReportDialog = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            ) {
                Text("Report an Issue")
            }

            Spacer(modifier = Modifier.height(90.dp))
        }
    }
}

@Composable
fun LogsViewer(
    logs: List<LogEntry>,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            listState.scrollToItem(logs.size - 1)
        }
    }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            if (logs.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No logs available",
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    state = listState
                ) {
                    items(logs) { logEntry ->
                        LogEntryItem(logEntry)

                        if (logEntry != logs.last()) {
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ExportLogsDialog(
    onDismissRequest: () -> Unit,
    onZipExport: () -> Unit,
    onUrlExport: () -> Unit
) {
    Dialog(onDismissRequest = onDismissRequest) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(16.dp),
            shape = MaterialTheme.shapes.large
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Export Logs",
                    style = MaterialTheme.typography.headlineSmall
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Choose how you would like to export the logs:",
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(24.dp))

                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = onZipExport,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text(
                            "Generate ZIP",
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Button(
                        onClick = onUrlExport,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    ) {
                        Text(
                            "Generate URL",
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Button(
                        onClick = onDismissRequest,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Text("Cancel")
                    }
                }
            }
        }
    }
}

@Composable
fun ReportIssueDialog(
    onDismiss: () -> Unit,
    onOpenGitHub: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(16.dp),
            shape = MaterialTheme.shapes.large
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close dialog",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Report an Issue",
                        style = MaterialTheme.typography.headlineSmall
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "To help the developer fix issues, you can report it on GitHub by using the \"New Issue\" button and then using the \"Bug Report\" template.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = onOpenGitHub,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text("Open GitHub")
                    }
                }
            }
        }
    }
}