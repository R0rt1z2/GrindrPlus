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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.grindrplus.BuildConfig
import com.grindrplus.core.Config
import com.grindrplus.core.Constants.GRINDR_PACKAGE_NAME
import com.grindrplus.manager.DATA_URL
import com.grindrplus.manager.MainActivity
import com.grindrplus.manager.TAG
import com.grindrplus.manager.installation.Installation
import com.grindrplus.manager.ui.components.BannerType
import com.grindrplus.manager.ui.components.FileDialog
import com.grindrplus.manager.ui.components.MessageBanner
import com.grindrplus.manager.ui.components.NewPackageInfoDialog
import com.grindrplus.manager.ui.components.PackageSelector
import com.grindrplus.manager.ui.components.VersionSelector
import com.grindrplus.manager.ui.components.rememberLauncherFilePicker
import com.grindrplus.manager.utils.AppCloneUtils
import com.grindrplus.manager.utils.ErrorHandler
import com.grindrplus.manager.utils.isLSPosed
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.security.MessageDigest

@Composable
fun InstallPage(context: Activity, innerPadding: PaddingValues, viewModel: InstallScreenViewModel = viewModel()) {
    // 1. State from ViewModel
    val status by viewModel.status.collectAsState()
    val loadingText by viewModel.loadingText.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val logEntries = viewModel.logEntries

    // 2. UI-Specific State
    val warningBannerDismissed = Config.get("install_warning_banner_dismissed", false) as Boolean
    var warningBannerVisible by remember { mutableStateOf(!warningBannerDismissed) }
    var showLSPosedEnableDialog by remember { mutableStateOf(false) }
    var showNewPackageInfoDialog by remember { mutableStateOf(false) }

    var sourceFileSet by remember { mutableStateOf<SourceFileSet?>(null) }
    var selectedPackageName by remember { mutableStateOf(Config.getCurrentPackage()) }

    val isNewClone by viewModel.isNewClone.collectAsState()
    val uiEnabled = when (status) {
        InstallStatus.INSTALLING, InstallStatus.UNINSTALLING -> false
        else -> true
    }

    // 3. Side Effects
    val manifestUrl = (Config.get("custom_manifest", DATA_URL) as String).ifBlank { null }

    LaunchedEffect(Unit) {
        viewModel.loadVersionData(manifestUrl.toString())
    }

    val uninstallLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        // Add a small delay to ensure the OS has updated the package list
        (context as androidx.lifecycle.LifecycleOwner).lifecycleScope.launch {
            delay(500)
            AppCloneUtils.refresh(context)
            val isStillInstalled = AppCloneUtils.findApp(selectedPackageName)?.isInstalled ?: false
            viewModel.uninstallCompleted(selectedPackageName, isStillInstalled)
            if (!isStillInstalled) {
                selectedPackageName = GRINDR_PACKAGE_NAME
                Config.setCurrentPackage(GRINDR_PACKAGE_NAME)
            }
        }
    }

    LaunchedEffect(status) {
        if (status == InstallStatus.SUCCESS && isNewClone && isLSPosed()) {
            showLSPosedEnableDialog = true
        }
    }


    fun performInstallation() {
        val currentFileSet = sourceFileSet ?: run {
            Toast.makeText(context, "Please select version or custom files", Toast.LENGTH_SHORT).show()
            return
        }

        viewModel.install(
            context = context,
            packageName = selectedPackageName,
            sourceFileSet = currentFileSet,
            onSuccess = {
                Toast.makeText(context, "Installation complete!", Toast.LENGTH_SHORT).show()
            },
            onError = { e ->
                handleInstallationError(e, context, viewModel)
            }
        )
    }

    if (showNewPackageInfoDialog) {
        NewPackageInfoDialog(
            context = context,
            onDismiss = { showNewPackageInfoDialog = false },
            onConfirm = { appInfo ->
                showNewPackageInfoDialog = false
                selectedPackageName = appInfo.packageName
                Config.setCurrentPackage(appInfo.packageName)
                viewModel.addLog("Created new clone: ${appInfo.appName} (${appInfo.packageName})", LogType.INFO)
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
        if (status == InstallStatus.LOADING_VERSIONS) {
            LoadingScreen(loadingText)
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
                        enabled = uiEnabled,
                        onPackageSelected = { packageName ->
                            selectedPackageName = packageName
                            viewModel.resetStatus()
                            viewModel.addLog("Selected package: $packageName", LogType.INFO)
                        }
                    )
                }
            }

            MessageBanner(
                text = "• Don't close the app while installation is in progress\n• Grindr WILL crash on first launch after installation",
                isVisible = warningBannerVisible,
                isPulsating = status == InstallStatus.INSTALLING,
                modifier = Modifier.fillMaxWidth(),
                type = BannerType.WARNING,
                onDismiss = {
                    warningBannerVisible = false
                    Config.put("install_warning_banner_dismissed", true)
                }
            )

            // clone management buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { showNewPackageInfoDialog = true },
                    modifier = Modifier.weight(1f),
                    enabled = uiEnabled
                ) {
                    Text("New Clone")
                }

                if (selectedPackageName != GRINDR_PACKAGE_NAME) {
                    val isInstalled = AppCloneUtils.getExistingClones(context).any { it.packageName == selectedPackageName }
                    Button(
                        onClick = {
                            if (isInstalled) {
                                viewModel.uninstallStarted()
                                val intent = Intent(Intent.ACTION_DELETE)
                                intent.data = "package:$selectedPackageName".toUri()
                                uninstallLauncher.launch(intent)
                            } else {
                                Config.removePackage(selectedPackageName)
                                selectedPackageName = GRINDR_PACKAGE_NAME
                                Config.setCurrentPackage(GRINDR_PACKAGE_NAME)
                                viewModel.addLog("Removed clone from settings.", LogType.INFO)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = uiEnabled,
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
                NormalSourceFileSelector(context, viewModel, uiEnabled) { sourceFileSet = it }
            else
                LSPosedSourceFileSelector(viewModel, uiEnabled) { sourceFileSet = it }


            Spacer(modifier = Modifier.height(16.dp))

            ConsoleOutput(
                logEntries = logEntries,
                modifier = Modifier.weight(0.5f),
                onClear = {
                    viewModel.clearLogs()
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
                        viewModel.cleanup(context, sourceFileSet?.versionName)
                    },
                    enabled = uiEnabled,
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

                when (status) {
                    InstallStatus.SUCCESS ->
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

                    InstallStatus.INSTALLING ->
                        Button(
                            onClick = {},
                            enabled = false,
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
                                text = "Installing...",
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }

                    else -> {
                        val isInstalled = remember(selectedPackageName) {
                            AppCloneUtils.findApp(selectedPackageName)?.isInstalled ?: false
                        }

                        Button(
                            onClick = {
                                performInstallation()
                            },
                            enabled = uiEnabled && sourceFileSet != null,
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
                                text = if (isInstalled) "Update" else "Install",
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LSPosedSourceFileSelector(
    viewModel: InstallScreenViewModel,
    enabled: Boolean,
    onFilesSelected: (SourceFileSet) -> Unit
) {
    var useCustomFile by remember { mutableStateOf(false) }
    var versionName by remember { mutableStateOf<String?>(null) }
    val versionData = viewModel.versionData

     LaunchedEffect(versionData.size) {
        if (versionData.isEmpty()) // not loaded yet
            return@LaunchedEffect

        // if we find grindr apk with supported version, use it,
        // else let the user select manually downloaded file
        val supportedVersions = BuildConfig.TARGET_GRINDR_VERSION_NAMES
        val match = versionData.firstOrNull { it.modVer.substringAfterLast("-") in supportedVersions }
        if (match == null) {
            useCustomFile = true
            viewModel.addLog("No matching version found for LSPosed, requiring to select Grindr bundle.", LogType.WARNING)
            return@LaunchedEffect
        }

        versionName = match.modVer.substringAfterLast("-")
        onFilesSelected(SourceFileSet(
            Installation.SourceFiles.Download(match.grindrUrl, null),
            versionName!!
        ))

        viewModel.addLog("Auto-selected matching version for LSPosed: ${versionName}", LogType.INFO)
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
            viewModel.addLog("Grindr bundle selected: ${uri.lastPathSegment}", LogType.INFO)
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
                enabled = enabled
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
    viewModel: InstallScreenViewModel,
    enabled: Boolean,
    onFilesSelected: (SourceFileSet) -> Unit
) {
    var showCustomFileDialog by remember { mutableStateOf(false) }
    var selectedVersion by remember { mutableStateOf<ModVersion?>(null) }
    var customModVersion by remember { mutableStateOf<ModVersion?>(null) }
    val versionData = viewModel.versionData

    // Creates a background task to auto-select the latest version
    LaunchedEffect(versionData.size) {
        if (selectedVersion == null && versionData.isNotEmpty()) {
            selectedVersion = versionData.first()
            viewModel.addLog("Auto-selected latest version: ${selectedVersion?.modVer}", LogType.INFO)
        }
    }

    fun onVersionSelected(selected: ModVersion) {
        selectedVersion = selected

        if (selected.isCustom) {
            onFilesSelected(SourceFileSet(
                Installation.SourceFiles.Local(selected.grindrUrl.toUri(), selected.modUrl.toUri()),
                selected.modVer
            ))
        } else {
            onFilesSelected(SourceFileSet(
                Installation.SourceFiles.Download(selected.grindrUrl, selected.modUrl),
                selected.modVer
            ))
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        VersionSelector(
            versions = versionData + listOfNotNull(customModVersion),
            selectedVersion = selectedVersion,
            onVersionSelected = { selected ->
                onVersionSelected(selected)
                viewModel.addLog("Selected version ${selected.modVer}", LogType.INFO)
            },
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
            customOption = "Select Custom Files...",
            onCustomOptionSelected = {
                showCustomFileDialog = true
            }
        )
    }

    if (showCustomFileDialog) {
        FileDialog(
            context = context,
            onDismiss = { showCustomFileDialog = false },
            onSelect = { versionName, bundleUri, modUri ->
                customModVersion = ModVersion(
                    versionName,
                    bundleUri.toString(),
                    modUri.toString(),
                    isCustom = true
                )
                onVersionSelected(customModVersion!!)
                showCustomFileDialog = false
                viewModel.addLog("Custom files selected. Version: $versionName", LogType.INFO)
                viewModel.addLog("Bundle: ${bundleUri.lastPathSegment}, Mod: ${modUri.lastPathSegment}", LogType.INFO)
            }
        )
    }
}

@Composable
fun LoadingScreen(text: String) {
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
                text = text,
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

private fun handleInstallationError(e: Exception, context: Context, viewModel: InstallScreenViewModel) {
    val errorMessage = "ERROR: ${e.localizedMessage ?: "Unknown error"}"
    viewModel.addLog(errorMessage, LogType.ERROR)

    if (errorMessage.contains("INCOMPATIBLE") || e.message?.contains("INCOMPATIBLE") == true) {
        if (context is MainActivity) {
            context.runOnUiThread { MainActivity.showUninstallDialog.value = true }
        } else {
            Toast.makeText(context, "Installation failed: Signature mismatch. Please uninstall Grindr first.", Toast.LENGTH_SHORT).show()
        }
    } else {
        Toast.makeText(context, "Installation failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
    }

    ErrorHandler.logError(
        context,
        TAG,
        "Installation failed",
        e
    )
}

private fun launchApp(context: Context, packageName: String) {
    try {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
        if (launchIntent != null) {
            context.startActivity(launchIntent)
        } else {
            Toast.makeText(context, "Could not launch app. App may need to be opened manually.", Toast.LENGTH_SHORT).show()
        }
    } catch (e: Exception) {
        Toast.makeText(context, "Error launching app: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
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