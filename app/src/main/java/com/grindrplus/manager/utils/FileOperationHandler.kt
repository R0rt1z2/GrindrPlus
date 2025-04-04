package com.grindrplus.manager.utils

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import com.grindrplus.core.Constants.GRINDR_PACKAGE_NAME
import com.grindrplus.core.Logger
import com.scottyab.rootbeer.RootBeer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object FileOperationHandler {
    lateinit var activity: ComponentActivity

    private lateinit var importFileLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var exportFileLauncher: ActivityResultLauncher<String>
    private lateinit var exportZipLauncher: ActivityResultLauncher<String>

    fun init(activity: ComponentActivity) {
        this.activity = activity
        this.importFileLauncher =
            FileOperationHandler.activity.registerForActivityResult(
                ActivityResultContracts.OpenDocument()
            ) { uri ->
                uri?.let { readFromFile(it) }
            }

        this.exportFileLauncher =
            FileOperationHandler.activity.registerForActivityResult(
                ActivityResultContracts.CreateDocument("application/json")
            ) { uri ->
                uri?.let { writeToFile(it) }
            }

        this.exportZipLauncher =
            FileOperationHandler.activity.registerForActivityResult(
                ActivityResultContracts.CreateDocument("application/zip")
            ) { uri ->
                uri?.let { writeZipToFile(it) }
            }
    }

    private var pendingExportContent: String? = null
    private var pendingExportZipFile: File? = null
    private var onImportComplete: ((String) -> Unit)? = null

    fun exportFile(filename: String, content: String) {
        pendingExportContent = content
        exportFileLauncher.launch(filename)
    }

    fun exportZipFile(filename: String, zipFile: File) {
        pendingExportZipFile = zipFile
        exportZipLauncher.launch(filename)
    }

    fun importFile(mimeTypes: Array<String>, onComplete: (String) -> Unit) {
        onImportComplete = onComplete
        importFileLauncher.launch(mimeTypes)
    }

    private fun writeToFile(uri: android.net.Uri) {
        try {
            val content = pendingExportContent ?: return
            activity.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(content.toByteArray())
            }
        } catch (e: Exception) {
            Logger.apply {
                e("Failed to write file: ${e.message}")
                writeRaw(e.stackTraceToString())
            }
        } finally {
            pendingExportContent = null
        }
    }

    private fun writeZipToFile(uri: android.net.Uri) {
        try {
            val zipFile = pendingExportZipFile ?: return
            activity.contentResolver.openOutputStream(uri)?.use { outputStream ->
                zipFile.inputStream().use { input ->
                    input.copyTo(outputStream)
                }
            }
            zipFile.delete()
        } catch (e: Exception) {
            Logger.apply {
                e("Failed to write zip file: ${e.message}")
                writeRaw(e.stackTraceToString())
            }
        } finally {
            pendingExportZipFile = null
        }
    }

    private fun readFromFile(uri: android.net.Uri) {
        try {
            activity.contentResolver.openInputStream(uri)?.use { inputStream ->
                val content = inputStream.bufferedReader().use { it.readText() }
                onImportComplete?.invoke(content)
            }
        } catch (e: Exception) {
            Logger.apply {
                e("Failed to read file: ${e.message}")
                writeRaw(e.stackTraceToString())
            }
        }
    }

    suspend fun createLogsZip(context: Context): File? = withContext(Dispatchers.IO) {
        try {
            val tempDir = File(context.cacheDir, "log_export")
            tempDir.mkdirs()

            val zipFile = File(context.cacheDir, "grindrplus_logs.zip")
            if (zipFile.exists()) zipFile.delete()

            val deviceInfoFile = File(tempDir, "device_info.json")

            val mainJson = JSONObject()

            val deviceSection = JSONObject().apply {
                put("manufacturer", android.os.Build.MANUFACTURER)
                put("model", android.os.Build.MODEL)
                put("device", android.os.Build.DEVICE)
                put("brand", android.os.Build.BRAND)
                put("hardware", android.os.Build.HARDWARE)
                put("fingerprint", android.os.Build.FINGERPRINT)
            }
            mainJson.put("device", deviceSection)

            val androidSection = JSONObject().apply {
                put("version", android.os.Build.VERSION.RELEASE)
                put("sdk_level", android.os.Build.VERSION.SDK_INT)
                put("security_patch", android.os.Build.VERSION.SECURITY_PATCH)
                put("is_rooted", RootBeer(context).isRooted)
            }
            mainJson.put("android", androidSection)

            val appsSection = JSONObject().apply {
                put("grindrplus_version", context.packageManager.getPackageInfo(context.packageName, 0).versionName)
                put("grindr_version", context.packageManager.getPackageInfo(GRINDR_PACKAGE_NAME, 0).versionName)
                put("clones", AppCloneUtils.getExistingClones(context).size)
            }
            mainJson.put("apps", appsSection)

            val systemSection = JSONObject().apply {
                put("timestamp", System.currentTimeMillis())
                put("locale", java.util.Locale.getDefault().toString())
                put("timezone", java.util.TimeZone.getDefault().id)
                val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
                val memoryInfo = android.app.ActivityManager.MemoryInfo()
                activityManager.getMemoryInfo(memoryInfo)
                put("total_memory_mb", memoryInfo.totalMem / (1024 * 1024))
                put("available_memory_mb", memoryInfo.availMem / (1024 * 1024))
            }
            mainJson.put("system", systemSection)

            deviceInfoFile.writeText(mainJson.toString(4))
            val logFiles = mutableListOf<File>()

            val mainLogFile = File(context.getExternalFilesDir(null), "grindrplus.log")
            if (mainLogFile.exists()) {
                val logCopy = File(tempDir, "grindrplus.log")
                mainLogFile.copyTo(logCopy, overwrite = true)
                logFiles.add(logCopy)
            }

            val backupLogFile = File("${mainLogFile.absolutePath}.bak")
            if (backupLogFile.exists()) {
                val backupCopy = File(tempDir, "grindrplus.log.bak")
                backupLogFile.copyTo(backupCopy, overwrite = true)
                logFiles.add(backupCopy)
            }

            logFiles.add(deviceInfoFile)
            ZipOutputStream(FileOutputStream(zipFile)).use { zipOut ->
                logFiles.forEach { file ->
                    val entry = ZipEntry(file.name)
                    zipOut.putNextEntry(entry)

                    file.inputStream().use { input ->
                        input.copyTo(zipOut)
                    }

                    zipOut.closeEntry()
                }
            }

            tempDir.deleteRecursively()
            return@withContext zipFile
        } catch (e: Exception) {
            Logger.apply {
                e("Failed to create logs zip: ${e.message}")
                writeRaw(e.stackTraceToString())
            }
            return@withContext null
        }
    }
}