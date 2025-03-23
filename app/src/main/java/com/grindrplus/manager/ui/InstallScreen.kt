package com.grindrplus.manager.ui

import android.app.Activity
import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.grindrplus.manager.DATA_URL
import com.grindrplus.manager.Installation
import com.grindrplus.manager.MainActivity
import com.grindrplus.manager.TAG
import com.grindrplus.manager.activityScope
import com.grindrplus.manager.utils.ConsoleLogger
import com.grindrplus.manager.utils.ConsoleOutput
import com.grindrplus.manager.utils.ErrorHandler
import com.grindrplus.manager.utils.LogEntry
import com.grindrplus.manager.utils.LogType
import com.grindrplus.manager.utils.StorageUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

private val logEntries = mutableStateListOf<LogEntry>()
private var progress by mutableFloatStateOf(0f)

@Composable
fun InstallPage(context: Activity, innerPadding: PaddingValues) {
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val versionData = remember { mutableStateListOf<Data>() }
    var selectedVersion by remember { mutableStateOf<Data?>(null) }
    var isInstalling by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            StorageUtils.cleanupOldInstallationFiles(context, true, null)
        }

        addLog("Welcome to Grindr Plus Manager")
        addLog("Loading available versions...", LogType.INFO)

        loadVersionData(
            onSuccess = { data ->
                versionData.clear()
                versionData.addAll(data)
                isLoading = false
                addLog("Found ${data.size} available versions", LogType.SUCCESS)
            },
            onError = { error ->
                errorMessage = error
                isLoading = false
                addLog("Failed to load version data: $error", LogType.ERROR)
            }
        )
    }
    Column(
        modifier = Modifier
            .padding(innerPadding)
            .padding(16.dp)
            .fillMaxSize()
    ) {
        if (isLoading) {
            LoadingScreen()
        } else if (errorMessage != null) {
            ErrorScreen(errorMessage!!) {
                // Retry loading
                isLoading = true
                errorMessage = null
                activityScope.launch {
                    addLog("Retrying version data load...", LogType.INFO)
                    loadVersionData(
                        onSuccess = { data ->
                            versionData.clear()
                            versionData.addAll(data)
                            isLoading = false
                            addLog(
                                "Found ${data.size} available versions",
                                LogType.SUCCESS
                            )
                        },
                        onError = { error ->
                            errorMessage = error
                            isLoading = false
                            addLog("Failed to load version data: $error", LogType.ERROR)
                        }
                    )
                }
            }
        } else {
            // Warning message
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "⚠️ Do not close the app while installation is in progress ⚠️",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Version selector dropdown
            VersionSelector(
                versions = versionData,
                selectedVersion = selectedVersion,
                onVersionSelected = {
                    selectedVersion = it
                    addLog("Selected version ${it.modVer}", LogType.INFO)
                },
                isEnabled = !isInstalling,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            ConsoleOutput(
                logEntries = logEntries,
                modifier = Modifier.weight(0.5f),
            )

            Spacer(modifier = Modifier.height(16.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(16.dp))


            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                OutlinedButton(
                    onClick = {
                        activityScope.launch {
                            try {
                                withContext(Dispatchers.IO) {
                                    StorageUtils.cleanupOldInstallationFiles(
                                        context, true, selectedVersion?.modVer
                                    )
                                }
                                addLog(
                                    "Cleaned up old installation files",
                                    LogType.SUCCESS
                                )
                            } catch (e: Exception) {
                                addLog(
                                    "Failed to clean up: ${e.localizedMessage}",
                                    LogType.ERROR
                                )
                            }
                        }
                    },
                    enabled = !isInstalling,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary,
                        disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(
                            alpha = 0.38f
                        )
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        "Clean Up",
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Button(
                    onClick = {
                        if (selectedVersion == null) {
                            showToast(context, "Please select a version first")
                            return@Button
                        }

                        startInstallation(
                            selectedVersion!!,
                            onStarted = { isInstalling = true },
                            onCompleted = { isInstalling = false },
                            context
                        )
                    },
                    enabled = selectedVersion != null && !isInstalling,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(
                            alpha = 0.12f
                        ),
                        disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(
                            alpha = 0.38f
                        )
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = if (isInstalling) "Installing..." else "Install",
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun LoadingScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Loading available versions...",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}

@Composable
fun ErrorScreen(errorMessage: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Error: $errorMessage",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Text("Retry")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VersionSelector(
    modifier: Modifier = Modifier,
    versions: List<Data>,
    selectedVersion: Data?,
    onVersionSelected: (Data) -> Unit,
    isEnabled: Boolean = true,
) {
    var expanded by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxWidth()
    ) {
        // Dropdown anchor
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = {
                if (isEnabled) {
                    expanded = !expanded
                }
            }
        ) {
            OutlinedTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(MenuAnchorType.PrimaryEditable, true),
                readOnly = true,
                enabled = isEnabled,
                value = selectedVersion?.modVer ?: "",
                onValueChange = { },
                label = { Text("Select a GrindrPlus version") },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    disabledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                )
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.fillMaxWidth()
            ) {
                if (versions.isEmpty()) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                "No versions available",
                                color = MaterialTheme.colorScheme.error
                            )
                        },
                        onClick = { expanded = false },
                        enabled = false
                    )
                } else {
                    versions.forEach { version ->
                        DropdownMenuItem(
                            text = {
                                Text("Version ${version.modVer}")
                            },
                            onClick = {
                                onVersionSelected(version)
                                expanded = false
                            },
                            contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                        )
                    }
                }
            }
        }
    }
}

private fun loadVersionData(
    onSuccess: (List<Data>) -> Unit,
    onError: (String) -> Unit,
) {
    activityScope.launch(Dispatchers.IO) {
        try {
            val client = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build()

            val request = Request.Builder()
                .url(DATA_URL)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("Failed to load data: ${response.code}")
                }

                val responseBody = response.body?.string()
                    ?: throw IOException("Empty response body")

                val data = parseVersionData(responseBody)
                withContext(Dispatchers.Main) {
                    onSuccess(data)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading version data", e)
            withContext(Dispatchers.Main) {
                onError(e.localizedMessage ?: "Unknown error")
            }
        }
    }
}

private fun parseVersionData(jsonData: String): List<Data> {
    try {
        val result = mutableListOf<Data>()
        val jsonObject = JSONObject(jsonData)
        val keys = jsonObject.keys()

        while (keys.hasNext()) {
            val key = keys.next()
            val jsonArray = jsonObject.getJSONArray(key)

            if (jsonArray.length() >= 2) {
                result.add(
                    Data(
                        modVer = key,
                        grindrUrl = jsonArray.getString(0),
                        modUrl = jsonArray.getString(1)
                    )
                )
            }
        }

        return result.sortedByDescending { it.modVer }
    } catch (e: JSONException) {
        Log.e(TAG, "Error parsing JSON", e)
        throw IOException("Invalid data format: ${e.localizedMessage}")
    }
}

private fun startInstallation(
    version: Data,
    onStarted: () -> Unit,
    onCompleted: () -> Unit,
    context: Activity,
) {
    onStarted()

    addLog("Starting installation for version ${version.modVer}...", LogType.INFO)

    activityScope.launch {
        try {
            val installation = Installation(
                context,
                version.modVer,
                version.modUrl,
                version.grindrUrl
            )

            withContext(Dispatchers.IO) {
                installation.install(
                    print = { output ->
                        Log.d(TAG, output)
                        val logType = ConsoleLogger.parseLogType(output)
                        context.runOnUiThread {
                            addLog(output, logType)
                        }
                    },

                    progress = {
                        context.runOnUiThread {
                            progress = it
                        }
                    }
                )
            }

            addLog("Installation completed successfully!", LogType.SUCCESS)
            showToast(context, "Installation complete!")
        } catch (e: Exception) {
            Log.e(TAG, "Installation failed", e)
            val errorMessage = "ERROR: ${e.localizedMessage ?: "Unknown error"}"
            addLog(errorMessage, LogType.ERROR)
            showToast(context, "Installation failed: ${e.localizedMessage}")

            ErrorHandler.logError(
                context,
                TAG,
                "Installation failed for version ${version.modVer}",
                e
            )
        } finally {
            onCompleted()
        }
    }
}

private fun addLog(message: String, type: LogType = LogType.INFO) {
    if (message.contains("<>:")) {
        val prefix = message.split("<>:")[0]

        logEntries.find { it.message.startsWith(prefix) }?.let {
            logEntries.remove(it)
        }
    }

    val logEntry = ConsoleLogger.log(message.replace("<>:", ":"), type)
    logEntries.add(logEntry)
}

private fun showToast(context: Context, message: String) {
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
}

data class Data(
    val modVer: String,
    val grindrUrl: String,
    val modUrl: String,
)