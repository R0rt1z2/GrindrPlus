package com.grindrplus.hooks

import android.app.AlertDialog
import android.content.Context
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.grindrplus.BuildConfig
import com.grindrplus.GrindrPlus
import com.grindrplus.GrindrPlus.restartGrindr
import com.grindrplus.core.Config
import com.grindrplus.core.loge
import com.grindrplus.core.logi
import com.grindrplus.core.logw
import com.grindrplus.utils.Hook
import com.grindrplus.utils.HookStage
import com.grindrplus.utils.hookConstructor
import java.io.File

class StatusDialog : Hook(
    "Status Dialog",
    "Check whether GrindrPlus is alive or not"
) {
    private val tabView = "com.google.android.material.tabs.TabLayout\$TabView"

    override fun init() {
        findClass(tabView).hookConstructor(HookStage.AFTER) { param ->
            val tabView = param.thisObject() as View

            tabView.post {
                val parent = tabView.parent as? ViewGroup
                val position = parent?.indexOfChild(tabView) ?: -1

                if (position == 0) {
                    tabView.setOnLongClickListener { v ->
                        showGrindrPlusDialog(v.context)
                        false
                    }
                }
            }
        }
    }

    private fun showGrindrPlusDialog(context: Context) {
        GrindrPlus.currentActivity?.runOnUiThread {
            try {
                val packageManager = context.packageManager
                val packageInfo = packageManager.getPackageInfo(context.packageName, 0)

                val appVersionName = packageInfo.versionName
                val appVersionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    packageInfo.longVersionCode
                } else {
                    @Suppress("DEPRECATION")
                    packageInfo.versionCode.toLong()
                }

                val packageName = context.packageName
                val deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}"
                val androidVersion = "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"
                val moduleVersion = try {
                    BuildConfig.VERSION_NAME
                } catch (e: Exception) {
                    "Unknown"
                }

                val bridgeStatus = if (GrindrPlus.bridgeClient.isConnected()) {
                    "Connected"
                } else {
                    "Disconnected"
                }

                val androidDeviceIdStatus = (Config.get("android_device_id", "") as String)
                    .let { id -> if (id.isNotEmpty()) "Spoofing ($id)" else "Not Spoofing (stock)" }

                if (GrindrPlus.bridgeClient.isConnected()) {
                    val isLSPosed = GrindrPlus.bridgeClient.isLSPosed()
                    val isRooted = GrindrPlus.bridgeClient.isRooted()
                }

                val message = buildString {
                    appendLine("GrindrPlus is active and running")
                    appendLine()
                    appendLine("App Information:")
                    appendLine("• Version: $appVersionName ($appVersionCode)")
                    appendLine("• Package: $packageName")
                    appendLine("• Android ID: $androidDeviceIdStatus")
                    appendLine()
                    appendLine("Module Information:")
                    appendLine("• GrindrPlus: $moduleVersion")
                    appendLine("• Bridge Status: $bridgeStatus")
                    if (GrindrPlus.bridgeClient.isConnected()) {
                        appendLine("• LSPosed: ${GrindrPlus.bridgeClient.isLSPosed()}")
                        appendLine("• Rooted: ${GrindrPlus.bridgeClient.isRooted()}")
                    }
                    appendLine()
                    appendLine("Device Information:")
                    appendLine("• Device: $deviceModel")
                    appendLine("• Android: $androidVersion")
                    appendLine()
                    appendLine("Long press this tab to show this dialog")
                }

                AlertDialog.Builder(context)
                    .setTitle("GrindrPlus")
                    .setMessage(message)
                    .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
                    .setNegativeButton("Restart") { dialog, _ ->
                        dialog.dismiss()
                        performCacheClearOperation(context)
                    }
                    .setIcon(android.R.drawable.ic_dialog_info)
                    .show()

            } catch (e: Exception) {
                AlertDialog.Builder(context)
                    .setTitle("GrindrPlus")
                    .setMessage("GrindrPlus is active and running\n\nError retrieving details: ${e.message}")
                    .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
                    .show()
            }
        }
    }

    private fun performCacheClearOperation(context: Context) {
        GrindrPlus.executeAsync {
            val operationResult = CacheClearResult()

            try {
                val cacheDirs = getCacheDirectories(context)

                cacheDirs.forEach { cacheDir ->
                    if (cacheDir.exists() && cacheDir.canWrite()) {
                        val clearResult = clearDirectoryContents(cacheDir)
                        operationResult.addResult(clearResult)
                    }
                }

                clearTemporarySharedPreferences(context, operationResult)
                clearWebViewCache(context, operationResult)

                val totalSizeMB = operationResult.totalSize / (1024 * 1024)
                logi("Cache operation completed: ${operationResult.totalFiles} files removed, ${totalSizeMB}MB freed")
                restartGrindr(100, "Restarting Grindr... (${totalSizeMB}MB freed)")
            } catch (e: Exception) {
                loge("Cache clear operation failed: ${e.message}")
                GrindrPlus.runOnMainThread {
                    GrindrPlus.showToast(Toast.LENGTH_LONG, "Cache clear operation failed: ${e.localizedMessage}")
                }
            }
        }
    }

    private fun getCacheDirectories(context: Context): List<File> {
        val directories = mutableListOf<File>()

        context.cacheDir?.let { directories.add(it) }
        context.externalCacheDir?.let { directories.add(it) }

        return directories
    }

    private fun clearDirectoryContents(directory: File): ClearOperationResult {
        val result = ClearOperationResult()

        if (!directory.exists() || !directory.isDirectory) {
            return result
        }

        try {
            directory.listFiles()?.forEach { file ->
                val fileSize = if (file.isFile) file.length() else 0L

                val deleted = if (file.isDirectory) {
                    val subResult = clearDirectoryContents(file)
                    result.addSubResult(subResult)
                    file.delete()
                } else {
                    file.delete()
                }

                if (deleted) {
                    result.filesCleared++
                    result.bytesCleared += fileSize
                }
            }
        } catch (e: SecurityException) {
            logw("Permission denied accessing directory: ${directory.absolutePath}")
        } catch (e: Exception) {
            logw("Error processing directory: ${directory.absolutePath} - ${e.message}")
        }

        return result
    }

    private fun clearWebViewCache(context: Context, operationResult: CacheClearResult) {
        try {
            val webViewCacheDir = File(context.filesDir.parent, "app_webview")
            if (webViewCacheDir.exists()) {
                val clearResult = clearDirectoryContents(webViewCacheDir)
                operationResult.addResult(clearResult)
            }
        } catch (e: Exception) {
            logw("WebView cache clear failed: ${e.message}")
        }
    }

    private fun clearTemporarySharedPreferences(context: Context, operationResult: CacheClearResult) {
        try {
            val sharedPrefsDir = File(context.filesDir.parent, "shared_prefs")
            if (sharedPrefsDir.exists()) {
                sharedPrefsDir.listFiles()
                    ?.filter { it.name.contains("cache", ignoreCase = true) || it.name.contains("temp", ignoreCase = true) }
                    ?.forEach { file ->
                        val size = file.length()
                        if (file.delete()) {
                            operationResult.totalFiles++
                            operationResult.totalSize += size
                        }
                    }
            }
        } catch (e: Exception) {
            logw("Temporary shared preferences clear failed: ${e.message}")
        }
    }

    private data class ClearOperationResult(
        var filesCleared: Int = 0,
        var bytesCleared: Long = 0L
    ) {
        fun addSubResult(other: ClearOperationResult) {
            filesCleared += other.filesCleared
            bytesCleared += other.bytesCleared
        }
    }

    private data class CacheClearResult(
        var totalFiles: Int = 0,
        var totalSize: Long = 0L
    ) {
        fun addResult(result: ClearOperationResult) {
            totalFiles += result.filesCleared
            totalSize += result.bytesCleared
        }
    }
}