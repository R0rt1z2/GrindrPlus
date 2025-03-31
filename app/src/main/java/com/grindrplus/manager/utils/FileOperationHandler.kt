package com.grindrplus.manager.utils

import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts

object FileOperationHandler {
    lateinit var activity: ComponentActivity

    private lateinit var importFileLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var exportFileLauncher: ActivityResultLauncher<String>

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
    }

    private var pendingExportContent: String? = null
    private var onImportComplete: ((String) -> Unit)? = null

    fun exportFile(filename: String, content: String) {
        pendingExportContent = content
        exportFileLauncher.launch(filename)
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
            // Handle exception
        } finally {
            pendingExportContent = null
        }
    }

    private fun readFromFile(uri: android.net.Uri) {
        try {
            activity.contentResolver.openInputStream(uri)?.use { inputStream ->
                val content = inputStream.bufferedReader().use { it.readText() }
                onImportComplete?.invoke(content)
            }
        } catch (e: Exception) {
            // Handle exception
        }
    }
}