package com.grindrplus.manager.ui

import android.app.Activity
import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grindrplus.core.Config
import com.grindrplus.core.Constants.GRINDR_PACKAGE_NAME
import com.grindrplus.core.Logger
import com.grindrplus.manager.DATA_URL
import com.grindrplus.manager.installation.Installation
import com.grindrplus.manager.utils.AppCloneUtils
import com.grindrplus.manager.utils.StorageUtils
import com.grindrplus.manager.utils.isLSPosed
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONException
import org.json.JSONObject
import timber.log.Timber
import java.io.IOException

enum class LogType {
    INFO, WARNING, ERROR, DEBUG, VERBOSE, SUCCESS
}

data class LogEntry(
    val timestamp: String?,
    val message: String,
    val type: LogType
)

enum class InstallStatus {
    IDLE,
    LOADING_VERSIONS,
    INSTALLING,
    UNINSTALLING,
    SUCCESS
}

class InstallScreenViewModel : ViewModel() {

    val manifestUrl = (Config.get("custom_manifest", DATA_URL) as String)

    private val _status = MutableStateFlow(InstallStatus.IDLE)
    val status = _status.asStateFlow()

    private val _loadingText = MutableStateFlow("Loading available versions...")
    val loadingText = _loadingText.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    val versionData = mutableStateListOf<ModVersion>()
    val logEntries = mutableStateListOf<LogEntry>()

    private val _isNewClone = MutableStateFlow(false)
    val isNewClone = _isNewClone.asStateFlow()

    fun addLog(message: String, type: LogType = LogType.INFO) {
        if (message.contains("<>:")) {
            val prefix = message.split("<>:")[0]
            logEntries.find { it.message.startsWith(prefix) }?.let {
                logEntries.remove(it)
            }
        }

        val logEntry = ConsoleLogger.log(message.replace("<>:", ":"), type)
        logEntries.add(logEntry)

        Timber.d("Install log: $type $message")
    }

    fun clearLogs() {
        logEntries.clear()
    }

    fun loadVersionData(context: Context) {
        if (versionData.isNotEmpty()) return

        _status.value = InstallStatus.LOADING_VERSIONS
        _errorMessage.value = null
        viewModelScope.launch {
            // Update loading text after 10 seconds of waiting
            val textUpdateJob = launch {
                delay(10000)
                _loadingText.value = "Still loading... Check your internet connectivity."
            }

            val result = withContext(Dispatchers.IO) {
                fetchVersions(manifestUrl)
            }

            textUpdateJob.cancel() // Cancel the text update job once loading is done

            if (result.isSuccess) {
                versionData.clear()
                versionData.addAll(result.getOrThrow())
                Logger.d("Found ${versionData.size} available versions")

                AppCloneUtils.setAvailableModVersions(versionData.map { it.modVersion }, context)
            } else {
                _errorMessage.value = result.exceptionOrNull()?.localizedMessage ?: "Unknown error"
                Logger.e("Error loading version data: ${_errorMessage.value}")
            }
            _status.value = InstallStatus.IDLE
        }
    }

    fun install(
        context: Context,
        packageName: String,
        sourceFileSet: SourceFileSet,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        _status.value = InstallStatus.INSTALLING
        val versionName = sourceFileSet.versionName

        addLog("Starting installation for $packageName with version: $versionName...", LogType.INFO)

        viewModelScope.launch {
            try {
                val isClone = packageName != GRINDR_PACKAGE_NAME
                _isNewClone.value = isClone && AppCloneUtils.apps.value
                    .none { it.packageName == packageName && it.isInstalled }

                val appInfo = if (!isClone) null else {
                    val cloneName = AppCloneUtils.apps.value
                        .find { it.packageName == packageName }?.appName
                        ?: AppCloneUtils.formatAppName(packageName)

                    Installation.AppInfoOverride(packageName, cloneName)
                }

                val mapsApiKey = (Config.get("maps_api_key", "") as String).ifBlank { null }

                val installation = Installation(
                    context,
                    versionName,
                    sourceFiles = sourceFileSet.sourceFiles,
                    appInfo = appInfo,
                    mapsApiKey = mapsApiKey,
                    embedLSPatch = !isLSPosed(),
                )

                withContext(Dispatchers.IO) {
                    installation.start { output ->
                        (context as? Activity)?.runOnUiThread {
                            addLog(output, ConsoleLogger.parseLogType(output))
                        }
                    }
                }

                addLog("Installation completed successfully!", LogType.SUCCESS)
                AppCloneUtils.refresh(context)
                onSuccess()
                _status.value = InstallStatus.SUCCESS
            } catch (e: Exception) {
                _status.value = InstallStatus.IDLE
                onError(e)
            }
        }
    }

    fun cleanup(context: Context, versionName: String?) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    StorageUtils.cleanupOldInstallationFiles(context, true, versionName)
                }
                addLog("Cleaned up old installation files", LogType.SUCCESS)
            } catch (e: Exception) {
                addLog("Failed to clean up: ${e.localizedMessage}", LogType.ERROR)
            }
        }
    }

    fun uninstallStarted() {
        _status.value = InstallStatus.UNINSTALLING
    }

    fun uninstallCompleted(packageName: String, isStillInstalled: Boolean) {
        if (!isStillInstalled) {
            addLog("Successfully uninstalled and removed clone settings.", LogType.SUCCESS)
        } else {
            addLog("Uninstall cancelled or failed.", LogType.WARNING)
        }
        _status.value = InstallStatus.IDLE
    }

    fun resetStatus() {
        _status.value = InstallStatus.IDLE
        _errorMessage.value = null
    }

    private fun fetchVersions(manifestUrl: String): Result<List<ModVersion>> {
        val client = HttpClient.instance
        val maxRetries = 3
        var lastException: Exception? = null

        for (attempt in 1..maxRetries) {
            try {
                Logger.d("Loading version data from $manifestUrl (Attempt $attempt/$maxRetries)")
                val request = Request.Builder().url(manifestUrl).build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) throw IOException("Server error: ${response.code}")
                    val body = response.body?.string() ?: throw IOException("Empty response body")
                    return Result.success(parseVersionData(body))
                }
            } catch (e: Exception) {
                lastException = e
                Logger.e("Attempt $attempt failed: ${e.message}")
                if (attempt < maxRetries) Thread.sleep(2000) // Use sleep for non-coroutine delay
            }
        }
        return Result.failure(lastException ?: IOException("Unknown error after $maxRetries retries"))
    }

    private fun parseVersionData(jsonData: String): List<ModVersion> {
        return try {
            val result = mutableListOf<ModVersion>()
            val jsonObject = JSONObject(jsonData)
            jsonObject.keys().forEach { key ->
                val jsonArray = jsonObject.getJSONArray(key)
                if (jsonArray.length() >= 2) {
                    result.add(
                        ModVersion(
                            modVersion = key,
                            grindrUrl = jsonArray.getString(0),
                            modUrl = jsonArray.getString(1)
                        )
                    )
                }
            }
            result.sortedByDescending { it.modVersion }
        } catch (e: JSONException) {
            throw IOException("Invalid data format: ${e.localizedMessage}")
        }
    }
}

data class ModVersion(
    val modVersion: String,
    val grindrUrl: String,
    val modUrl: String,
    val isCustom: Boolean = false
)

data class SourceFileSet(
    val sourceFiles: Installation.SourceFiles,
    val versionName: String
)