package com.grindrplus.manager.ui

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.grindrplus.core.Config
import com.grindrplus.core.Constants.GRINDR_PACKAGE_NAME
import com.grindrplus.core.Logger
import com.grindrplus.manager.DATA_URL
import com.grindrplus.manager.MainActivity
import com.grindrplus.manager.TAG
import com.grindrplus.manager.activityScope
import com.grindrplus.manager.installation.Installation
import com.grindrplus.manager.installation.Print
import com.grindrplus.manager.ui.components.BannerType
import com.grindrplus.manager.ui.components.CloneDialog
import com.grindrplus.manager.ui.components.MessageBanner
import com.grindrplus.manager.ui.components.VersionSelector
import com.grindrplus.manager.utils.ErrorHandler
import com.grindrplus.manager.utils.StorageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import com.grindrplus.manager.ui.components.FileDialog
import com.grindrplus.manager.utils.isLSPosed

private val logEntries = mutableStateListOf<LogEntry>()

@Stable
private class InstallPageState(
    val context: Activity,
    val isLSPosed: Boolean,
) {
    var selectedVersion by mutableStateOf<Data?>(null)
    var isInstalling by mutableStateOf(false)
    var isCloning by mutableStateOf(false)
    var installationSuccessful by mutableStateOf(false)
    var showCloneDialog by mutableStateOf(false)
    var installation by mutableStateOf<Installation?>(null)
    var warningBannerVisible by mutableStateOf(true)
    var lsposedBannerVisible by mutableStateOf(isLSPosed)
    var showCustomFileDialog by mutableStateOf(false)
    var useCustomFiles by mutableStateOf(false)
    var customVersionName by mutableStateOf("custom")
    var customBundleUri by mutableStateOf<Uri?>(null)
    var customModUri by mutableStateOf<Uri?>(null)

    val print: Print = { output ->
        val logType = ConsoleLogger.parseLogType(output)
        context.runOnUiThread { addLog(output, logType) }
    }

    fun selectVersion(selected: Data) {
        when {
            // Re-selecting the active custom version: keep current state.
            selected.modVer == customVersionName && useCustomFiles -> Unit
            selected.modVer == "custom" -> showCustomFileDialog = true
            else -> {
                selectedVersion = selected
                useCustomFiles = false
                addLog("Selected version ${selected.modVer}", LogType.INFO)
            }
        }
    }

    fun applyCustomFiles(versionName: String, bundleUri: Uri, modUri: Uri) {
        customVersionName = versionName
        customBundleUri = bundleUri
        customModUri = modUri
        useCustomFiles = true
        showCustomFileDialog = false
        addLog("Custom files selected. Version: $versionName", LogType.INFO)
        addLog("Bundle: ${bundleUri.lastPathSegment}, Mod: ${modUri.lastPathSegment}", LogType.INFO)
    }

    fun cleanUp() {
        activityScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    StorageUtils.cleanupOldInstallationFiles(
                        context, true, selectedVersion?.modVer
                    )
                }
                addLog("Cleaned up old installation files", LogType.SUCCESS)
            } catch (e: Exception) {
                addLog("Failed to clean up: ${e.localizedMessage}", LogType.ERROR)
            }
        }
    }

    fun requestCloneDialog() { showCloneDialog = true }
    fun dismissCloneDialog() { showCloneDialog = false }
    fun dismissCustomFileDialog() { showCustomFileDialog = false }
    fun dismissWarningBanner() { warningBannerVisible = false }
    fun dismissLSPosedBanner() { lsposedBannerVisible = false }

    fun performClone(packageName: String, appName: String, debuggable: Boolean, embedLSPatch: Boolean) {
        showCloneDialog = false
        isCloning = true
        activityScope.launch {
            addLog("Starting Grindr cloning process...", LogType.INFO)
            addLog("Target package: $packageName", LogType.INFO)
            addLog("Target app name: $appName", LogType.INFO)

            val success = try {
                installation!!.cloneGrindr(packageName, appName, debuggable, embedLSPatch, print)
                true
            } catch (e: Exception) {
                Logger.i("Cloning failed: ${e.localizedMessage}")
                addLog("Cloning failed: ${e.localizedMessage}", LogType.ERROR)
                false
            }
            if (success) addLog("Grindr clone created successfully!", LogType.SUCCESS)
            else addLog("Failed to clone Grindr", LogType.ERROR)
            isCloning = false
        }
    }

    fun installOrLaunch() {
        when {
            installationSuccessful -> launchGrindr(context)
            useCustomFiles && customBundleUri != null && customModUri != null -> startCustomInstallation()
            selectedVersion == null -> showToast(context, "Please select a version first")
            else -> startInstallation(selectedVersion!!)
        }
    }

    private fun startInstallation(version: Data) {
        isInstalling = true
        addLog("Starting installation for version ${version.modVer}...", LogType.INFO)

        activityScope.launch {
            try {
                val mapsApiKey = (Config.get("maps_api_key", "") as String).ifBlank { null }
                val install = Installation(
                    context, version.modVer, version.modUrl, version.grindrUrl, mapsApiKey
                )
                withContext(Dispatchers.IO) { install.install(print = print) }
                addLog("Installation completed successfully!", LogType.SUCCESS)
                showToast(context, "Installation complete!")
                installationSuccessful = true
            } catch (e: Exception) {
                handleInstallationError(e, context)
                installationSuccessful = false
            } finally {
                isInstalling = false
            }
        }
    }

    private fun startCustomInstallation() {
        val bundleUri = customBundleUri
        val modUri = customModUri
        if (bundleUri == null || modUri == null) {
            showToast(context, "Please select both bundle and mod files")
            return
        }

        isInstalling = true
        addLog("Starting custom installation with version name: $customVersionName...", LogType.INFO)

        activityScope.launch {
            try {
                val bundleFile = createTempFileFromUri(context, bundleUri, "grindr-$customVersionName.zip")
                val modFile = createTempFileFromUri(context, modUri, "mod-$customVersionName.zip")
                val mapsApiKey = (Config.get("maps_api_key", "") as String).ifBlank { null }
                val customInstall = Installation(
                    context, customVersionName, modFile.absolutePath, bundleFile.absolutePath, mapsApiKey
                )
                withContext(Dispatchers.IO) { customInstall.installCustom(bundleFile, modFile, print) }
                addLog("Custom installation completed successfully!", LogType.SUCCESS)
                showToast(context, "Installation complete!")
                installationSuccessful = true
            } catch (e: Exception) {
                handleInstallationError(e, context)
            } finally {
                isInstalling = false
            }
        }
    }
}

@Composable
private fun rememberInstallPageState(context: Activity): InstallPageState {
    val isLSPosedNow = remember { isLSPosed() }
    return remember(context) { InstallPageState(context, isLSPosedNow) }
}

@Composable
fun InstallPage(context: Activity, innerPadding: PaddingValues, viewModel: InstallScreenViewModel = viewModel()) {
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val versionData = viewModel.versionData
    val state = rememberInstallPageState(context)
    val manifestUrl = (Config.get("custom_manifest", DATA_URL) as String).ifBlank { null }

    LaunchedEffect(Unit) {
        viewModel.loadVersionData(manifestUrl.toString())
    }

    LaunchedEffect(versionData.size) {
        if (state.selectedVersion == null && versionData.isNotEmpty()) {
            state.selectedVersion = versionData.first()
            addLog("Auto-selected latest version: ${state.selectedVersion?.modVer}", LogType.INFO)
        }
    }

    LaunchedEffect(state.selectedVersion) {
        val version = state.selectedVersion ?: return@LaunchedEffect
        val mapsApiKey = (Config.get("maps_api_key", "") as String).ifBlank { null }
        state.installation = Installation(
            context, version.modVer, version.modUrl, version.grindrUrl, mapsApiKey
        )
    }

    if (state.showCloneDialog) {
        CloneDialog(
            context = context,
            onDismiss = state::dismissCloneDialog,
            onStartCloning = state::performClone,
        )
    }

    if (state.showCustomFileDialog) {
        FileDialog(
            context = context,
            onDismiss = state::dismissCustomFileDialog,
            onSelect = state::applyCustomFiles,
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
                viewModel.loadVersionData(manifestUrl.toString())
            }
        } else {
            MessageBanner(
                text = "• Don't close the app while installation is in progress\n• Grindr WILL crash on first launch after installation",
                isVisible = state.warningBannerVisible,
                isPulsating = state.isInstalling || state.isCloning,
                modifier = Modifier.fillMaxWidth(),
                type = BannerType.WARNING,
                onDismiss = state::dismissWarningBanner
            )

            if (state.isLSPosed) {
                MessageBanner(
                    text = "We detected that you are using LSPosed. Only use this screen to create clones, not to install the modded Grindr.",
                    isVisible = state.lsposedBannerVisible,
                    isPulsating = true,
                    modifier = Modifier.fillMaxWidth(),
                    type = BannerType.ERROR,
                    onDismiss = state::dismissLSPosedBanner
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                VersionSelector(
                    versions = if (state.useCustomFiles)
                        listOf(Data(state.customVersionName, "", "")) + versionData
                    else
                        versionData,
                    selectedVersion = if (state.useCustomFiles && state.customBundleUri != null)
                        Data(state.customVersionName, "", "")
                    else
                        state.selectedVersion,
                    onVersionSelected = state::selectVersion,
                    isEnabled = !state.isInstalling && !state.isCloning,
                    modifier = Modifier.fillMaxWidth(),
                    customOption = "Use Custom Files..."
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            ConsoleOutput(
                logEntries = logEntries,
                modifier = Modifier.weight(0.5f),
                onClear = {
                    logEntries.clear()
                    addLog("Successfully cleared logs!", LogType.SUCCESS)
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                OutlinedButton(
                    onClick = state::cleanUp,
                    enabled = !state.isInstalling && !state.isCloning,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary,
                        disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Clean Up", modifier = Modifier.padding(vertical = 8.dp))
                }

                Spacer(modifier = Modifier.width(16.dp))

                Button(
                    onClick = state::installOrLaunch,
                    enabled = ((state.selectedVersion != null || (state.useCustomFiles && state.customBundleUri != null && state.customModUri != null)) ||
                            state.installationSuccessful) && !state.isInstalling && !state.isCloning,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                        disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = when {
                            state.isInstalling -> "Installing..."
                            state.installationSuccessful -> "Open Grindr"
                            else -> "Install"
                        },
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }

            if (isGrindrInstalled(context)) {
                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = state::requestCloneDialog,
                    enabled = !state.isInstalling && !state.isCloning,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Clone",
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        text = if (state.isCloning) "Cloning..." else "Clone Grindr",
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
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 24.dp)
        ) {
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
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "If loading appears stuck, please force close the app and try again.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
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

private suspend fun createTempFileFromUri(context: Context, uri: Uri, filename: String): File {
    return withContext(Dispatchers.IO) {
        val tempFile = File(context.filesDir, filename)
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            tempFile.outputStream().use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        } ?: throw IOException("Failed to open input stream for URI: $uri")
        tempFile
    }
}

private fun handleInstallationError(e: Exception, context: Context) {
    val errorMessage = "ERROR: ${e.localizedMessage ?: "Unknown error"}"
    addLog(errorMessage, LogType.ERROR)

    if (errorMessage.contains("INCOMPATIBLE") || e.message?.contains("INCOMPATIBLE") == true) {
        if (context is MainActivity) {
            context.runOnUiThread { MainActivity.showUninstallDialog.value = true }
        } else {
            showToast(context, "Installation failed: Signature mismatch. Please uninstall Grindr first.")
        }
    } else {
        showToast(context, "Installation failed: ${e.localizedMessage}")
    }

    ErrorHandler.logError(
        context,
        TAG,
        "Installation failed",
        e
    )
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

private fun launchGrindr(context: Context) {
    try {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(GRINDR_PACKAGE_NAME)
        if (launchIntent != null) {
            context.startActivity(launchIntent)
        } else {
            showToast(context, "Could not launch Grindr. App may need to be opened manually.")
        }
    } catch (e: Exception) {
        showToast(context, "Error launching Grindr: ${e.localizedMessage}")
    }
}

private fun isGrindrInstalled(context: Context): Boolean {
    return try {
        context.packageManager.getPackageInfo(GRINDR_PACKAGE_NAME, 0)
        true
    } catch (_: PackageManager.NameNotFoundException) {
        false
    }
}

data class Data(
    val modVer: String,
    val grindrUrl: String,
    val modUrl: String,
)