package com.grindrplus.manager.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.grindrplus.manager.utils.FileOperationHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.file.FileSystems

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugLogsScreen(
    onBack: () -> Unit,
) {
    var logs by remember { mutableStateOf(emptyList<LogEntry>()) }
    val context = LocalContext.current

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Debug Logs") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Back",
                            modifier = Modifier.rotate(180f)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            ConsoleOutput(
                logEntries = logs,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                onClear = {
                    logs = emptyList()
                },
            )

            Spacer(modifier = Modifier.height(16.dp))

            val scope = rememberCoroutineScope()

            Button(
                onClick = {
                    scope.launch {
                        try {
                            val zipFile = FileOperationHandler.createLogsZip(context)
                            if (zipFile != null) {
                                FileOperationHandler.exportZipFile(
                                    "grindrplus_logs.zip",
                                    zipFile
                                )
                                // snackbarHostState.showSnackbar("Logs packaged, choose a location to save")
                            } else {
                                // snackbarHostState.showSnackbar("Failed to create logs package")
                            }
                        } catch (e: Exception) {
                            // snackbarHostState.showSnackbar("Error exporting logs: ${e.message}")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth().weight(1f),
            ) {
                Text(
                    modifier = Modifier.padding(8.dp),
                    text = "Export Logs"
                )
            }
        }
    }
}