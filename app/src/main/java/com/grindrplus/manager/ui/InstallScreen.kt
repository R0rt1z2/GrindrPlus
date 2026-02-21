package com.grindrplus.manager.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.grindrplus.core.Config
import com.grindrplus.core.Constants.GRINDR_PACKAGE_NAME
import com.grindrplus.manager.DATA_URL
import com.grindrplus.manager.MainActivity
import com.grindrplus.manager.TAG
import com.grindrplus.manager.activityScope
import com.grindrplus.manager.installation.Installation
import com.grindrplus.manager.installation.Print
import com.grindrplus.manager.ui.components.BannerType
import com.grindrplus.manager.ui.components.FileDialog
import com.grindrplus.manager.ui.components.MessageBanner
import com.grindrplus.manager.utils.AppCloneUtils
import com.grindrplus.manager.utils.ErrorHandler
import com.grindrplus.manager.utils.StorageUtils
import com.grindrplus.manager.utils.isLSPosed
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.core.net.toUri
import com.grindrplus.BuildConfig
import com.grindrplus.manager.ui.components.PackageSelector
import com.grindrplus.manager.ui.components.rememberLauncherFilePicker
import java.security.MessageDigest

private val logEntries = mutableStateListOf<LogEntry>()

@Composable
fun InstallPage(context: Activity, innerPadding: PaddingValues, viewModel: InstallScreenViewModel = viewModel()) {
    // 1. State from ViewModel
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val versionData = viewModel.versionData

    // 2. UI-Specific State
    val warningBannerDismissed = Config.get("install_warning_banner_dismissed", false) as Boolean
    var warningBannerVisible by remember { mutableStateOf(!warningBannerDismissed) }
    var showLSPosedEnableDialog by remember { mutableStateOf(false) }
    var showNewPackageInfoDialog by remember { mutableStateOf(false) }

    var sourceFileSet by remember { mutableStateOf<SourceFileSet?>(null) }
    var selectedPackageName by remember { mutableStateOf(Config.getCurrentPackage()) }

    var isInstalling by remember { mutableStateOf(false) }
    var isUninstalling by remember { mutableStateOf(false) }
    var installationSuccessful by remember { mutableStateOf(false) }
    var isNewCloneInstallation by remember { mutableStateOf(false) }
    var packagePendingUninstall by remember { mutableStateOf<String?>(null) }

    // 3. Side Effects
    val manifestUrl = (Config.get("custom_manifest", DATA_URL) as String).ifBlank { null }

    LaunchedEffect(Unit) {
        viewModel.loadVersionData(manifestUrl.toString())
    }

    val print: Print = { output ->
        val logType = ConsoleLogger.parseLogType(output)
        context.runOnUiThread {
            addLog(output, logType)
        }
    }

    val uninstallLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        if (packagePendingUninstall != null) {
            val isStillInstalled = AppCloneUtils.getExistingClones(context).any { it.packageName == packagePendingUninstall }
            if (!isStillInstalled) {
                Config.removePackage(packagePendingUninstall!!)
                if (selectedPackageName == packagePendingUninstall) {
                    selectedPackageName = GRINDR_PACKAGE_NAME
                    Config.setCurrentPackage(GRINDR_PACKAGE_NAME)
                }
                addLog("Successfully uninstalled and removed clone settings.", LogType.SUCCESS)
            } else {
                addLog("Uninstall cancelled or failed.", LogType.WARNING)
            }
            packagePendingUninstall = null
        }
    }

    LaunchedEffect(installationSuccessful) {
        if (installationSuccessful && isNewCloneInstallation && isLSPosed()) {
            showLSPosedEnableDialog = true
        }
    }

    fun getSourceFileSet(): SourceFileSet? {
        if (sourceFileSet == null)
            showToast(context, "Please select version or custom files")

        return sourceFileSet
    }

    fun getAppInfoOverride(): Installation.AppInfoOverride? {
        val isClone = selectedPackageName != GRINDR_PACKAGE_NAME
        if (!isClone)
            return null

        val cloneName = AppCloneUtils.getKnownClones(context)
            .find { it.packageName == selectedPackageName }?.appName
            ?: AppCloneUtils.formatAppName(selectedPackageName)

        val apiKey = (Config.get("maps_api_key", "") as String).ifBlank { null }

        return Installation.AppInfoOverride(selectedPackageName, cloneName, apiKey)
    }

    fun performInstallation() {
        val sourceFileSet = getSourceFileSet() ?: return

        isInstalling = true
        val isClone = selectedPackageName != GRINDR_PACKAGE_NAME
        isNewCloneInstallation = isClone && AppCloneUtils.getExistingClones(context).none { it.packageName == selectedPackageName }
        val versionName = sourceFileSet.versionName

        addLog("Starting installation with version: $versionName...", LogType.INFO)

        activityScope.launch {
            try {
                isInstalling = true
                val appInfo = getAppInfoOverride()

                val installation = Installation(
                    context,
                    versionName,
                    sourceFiles = sourceFileSet.sourceFiles,
                    appInfo = appInfo,
                    embedLSPatch = !isLSPosed(),
                )

                withContext(Dispatchers.IO) {
                    installation.start(
                        print = print
                    )
                }

                addLog("Custom installation completed successfully!", LogType.SUCCESS)
                AppCloneUtils.refreshCache(context)
                showToast(context, "Installation complete!")
                installationSuccessful = true
            } catch (e: Exception) {
                handleInstallationError(e, context)
            } finally {
                isInstalling = false
            }
        }
    }

    if (showNewPackageInfoDialog) {
        com.grindrplus.manager.ui.components.NewPackageInfoDialog(
            context = context,
            onDismiss = { showNewPackageInfoDialog = false },
            onConfirm = { appInfo ->
                showNewPackageInfoDialog = false
                selectedPackageName = appInfo.packageName
                Config.setCurrentPackage(appInfo.packageName)
                addLog("Created new clone: ${appInfo.appName} (${appInfo.packageName})", LogType.INFO)
            }
        )
    }

    if (showLSPosedEnableDialog) {
        AlertDialog(
            onDismissRequest = { showLSPosedEnableDialog = false },
            title = { Text("LSPosed Action Required") },
            text = { Text("Since this is a new clone and you are using LSPosed, you need to open the LSPosed manager and enable GrindrPlus for this new app before launching it.") },
            confirmButton = {
                Button(onClick = { showLSPosedEnableDialog = false }) {
                    Text("Understood")
                }
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
                viewModel.loadVersionData(manifestUrl.toString())
            }
        } else {
            // package selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    PackageSelector(
                        selectedPackage = selectedPackageName,
                        onPackageSelected = { packageName ->
                            selectedPackageName = packageName
                            addLog("Selected package: $packageName", LogType.INFO)
                        }
                    )
                }
            }

            MessageBanner(
                text = "• Don't close the app while installation is in progress\n• Grindr WILL crash on first launch after installation",
                isVisible = warningBannerVisible,
                isPulsating = isInstalling,
                modifier = Modifier.fillMaxWidth(),
                type = BannerType.WARNING,
                onDismiss = {
                    warningBannerVisible = false
                    Config.put("install_warning_banner_dismissed", true)
                }
            )

            // clone management buttons
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { showNewPackageInfoDialog = true },
                    modifier = Modifier.weight(1f),
                    enabled = !isInstalling && !isUninstalling
                ) {
                    Text("New Clone")
                }

                if (selectedPackageName != GRINDR_PACKAGE_NAME) {
                    val isInstalled = AppCloneUtils.getExistingClones(context).any { it.packageName == selectedPackageName }
                    Button(
                        onClick = {
                            if (isInstalled) {
                                packagePendingUninstall = selectedPackageName
                                val intent = Intent(Intent.ACTION_DELETE)
                                intent.data = "package:$selectedPackageName".toUri()
                                uninstallLauncher.launch(intent)
                            } else {
                                Config.removePackage(selectedPackageName)
                                selectedPackageName = GRINDR_PACKAGE_NAME
                                Config.setCurrentPackage(GRINDR_PACKAGE_NAME)
                                addLog("Removed clone from settings.", LogType.INFO)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !isInstalling && !isUninstalling,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isInstalled) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.errorContainer,
                            contentColor = if (isInstalled) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onErrorContainer
                        )
                    ) {
                        Text(if (isInstalled) "Uninstall" else "Remove")
                    }
                }
            }

            if (!isLSPosed())
                NormalSourceFileSelector(context, versionData) { sourceFileSet = it }
            else
                LSPosedSourceFileSelector(versionData) { sourceFileSet = it }


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

            // bottom row buttons
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
                                        context, true, sourceFileSet?.versionName
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

                if (installationSuccessful) {
                    Button(
                        onClick = { launchApp(context, selectedPackageName) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Open Grindr",
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                } else if (isInstalling) {
                    Button(
                        onClick = {},
                        enabled = false,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                            disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Installing...",
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                } else {
                    val isInstalled = remember(selectedPackageName) {
                        if (selectedPackageName == GRINDR_PACKAGE_NAME) {
                            isGrindrInstalled(context)
                        } else {
                            AppCloneUtils.getExistingClones(context).any { it.packageName == selectedPackageName && it.isInstalled }
                        }
                    }

                    val buttonEnabled = sourceFileSet != null

                    Button(
                        onClick = {
                            performInstallation()
                        },
                        enabled = buttonEnabled,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                            disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = if (isInstalled) "Update" else "Install",
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }
            }

            // Existing Grindr management button removed
        }
    }
}

@Composable
fun LSPosedSourceFileSelector(
    versionData: SnapshotStateList<Data>,
    onFilesSelected: (SourceFileSet) -> Unit
) {
    var useCustomFile by remember { mutableStateOf(false) }
    var versionName by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(versionData.size) {
        if (useCustomFile || versionData.isEmpty()) {
            useCustomFile = true
            addLog("No versions found for LSPosed, requiring to select Grindr bundle.", LogType.WARNING)
            return@LaunchedEffect
        }

        // if we find grindr apk with supported version, use it,
        // else let the user select manually downloaded file
        val supportedVersions = BuildConfig.TARGET_GRINDR_VERSION_NAMES
        val match = versionData.firstOrNull { it.modVer.substringAfterLast("-") in supportedVersions }
        if (match == null) {
            useCustomFile = true
            addLog("No matching version found for LSPosed, requiring to select Grindr bundle.", LogType.WARNING)
            return@LaunchedEffect
        }

        versionName = match.modVer.substringAfterLast("-")
        onFilesSelected(SourceFileSet(
            Installation.SourceFiles.Download(match.grindrUrl, null),
            versionName!!
        ))

        addLog("Auto-selected matching version for LSPosed: ${versionName}", LogType.INFO)
    }

    val bundlePickerLauncher = rememberLauncherFilePicker { uri: Uri? ->
        if (uri != null) {
            val filenameHash = MessageDigest.getInstance("SHA-256")
                .digest(uri.lastPathSegment!!.toByteArray())
                .fold("") { str, byte -> str + "%02x".format(byte) }
            versionName = filenameHash.substring(0,8)

            onFilesSelected(SourceFileSet(
                Installation.SourceFiles.Local(uri, null),
                versionName!!
            ))
            addLog("Grindr bundle selected: ${uri.lastPathSegment}", LogType.INFO)
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (useCustomFile) {
            Text(
                text =
                    if (versionName == null) {
                        val fallbackVer = BuildConfig.TARGET_GRINDR_VERSION_NAMES.first()
                        "Please select Grindr bundle $fallbackVer"
                    } else {
                        "Custom: $versionName"
                    },
                modifier = Modifier.weight(1f)
            )

            OutlinedButton(
                onClick = { bundlePickerLauncher.launch(arrayOf("*/*")) },
            ) {
                Text(if (versionName != null) "Change Bundle" else "Select Bundle")
            }

        } else {
            Text(
                text = "Version: ${versionName ?: "Loading..."}",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun NormalSourceFileSelector(
    context: Context,
    versionData: SnapshotStateList<Data>,
    onFilesSelected: (SourceFileSet) -> Unit
) {
    var showCustomFileDialog by remember { mutableStateOf(false) }

    var selectedVersion by remember { mutableStateOf<Data?>(null) }
    var customVersionName by remember { mutableStateOf<String?>(null) }


    // Creates a background task to auto-select the latest version
    LaunchedEffect(versionData.size) {
        if (selectedVersion == null && versionData.isNotEmpty()) {
            selectedVersion = versionData.first()
            addLog("Auto-selected latest version: ${selectedVersion?.modVer}", LogType.INFO)
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (customVersionName != null) {
            Text(
                text = "Custom: $customVersionName",
                modifier = Modifier.weight(1f)
            )
            OutlinedButton(
                onClick = { showCustomFileDialog = true },
            ) {
                Text("Change Files")
            }
            OutlinedButton(
                onClick = { customVersionName = null },
            ) {
                Text("Reset")
            }
        } else {
            Text(
                text = "Latest Version: ${selectedVersion?.modVer ?: "Loading..."}",
                modifier = Modifier.weight(1f)
            )
            OutlinedButton(
                onClick = { showCustomFileDialog = true },
            ) {
                Text("Custom Installation")
            }
        }
    }

    if (showCustomFileDialog) {
        FileDialog(
            context = context,
            onDismiss = { showCustomFileDialog = false },
            onSelect = { versionName, bundleUri, modUri ->
                onFilesSelected(SourceFileSet(
                    Installation.SourceFiles.Local(bundleUri, modUri),
                    versionName
                ))
                showCustomFileDialog = false
                addLog("Custom files selected. Version: $versionName", LogType.INFO)
                addLog("Bundle: ${bundleUri.lastPathSegment}, Mod: ${modUri.lastPathSegment}", LogType.INFO)
            }
        )
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

private fun launchApp(context: Context, packageName: String) {
    try {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
        if (launchIntent != null) {
            context.startActivity(launchIntent)
        } else {
            showToast(context, "Could not launch app. App may need to be opened manually.")
        }
    } catch (e: Exception) {
        showToast(context, "Error launching app: ${e.localizedMessage}")
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

data class SourceFileSet(
    val sourceFiles: Installation.SourceFiles,
    val versionName: String
)