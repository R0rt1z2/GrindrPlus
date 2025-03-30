package com.grindrplus.manager.utils

import android.content.Context
import android.os.Build
import android.util.Log
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// TODO: Sync with module once everything's connected
object ErrorHandler {
    private const val TAG = "ErrorHandler"
    private const val LOG_FILE_PREFIX = "grindrplus_log_"

    fun logError(context: Context, tag: String, message: String, error: Throwable?) {
        Timber.tag(tag).e(error, message)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val logDir = File(context.getExternalFilesDir(null), "logs")
                if (!logDir.exists()) {
                    logDir.mkdirs()
                }

                cleanupOldLogs(logDir, 5)

                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                val logFile = File(logDir, "$LOG_FILE_PREFIX${timestamp}.txt")

                FileOutputStream(logFile, true).use { output ->
                    val writer = PrintWriter(output)

                    writer.println("---- ERROR LOG $timestamp ----")
                    writer.println("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
                    writer.println("Android: ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})")
                    writer.println("Message: $message")

                    if (error != null) {
                        writer.println("Exception: ${error.javaClass.name}: ${error.message}")
                        val sw = StringWriter()
                        val pw = PrintWriter(sw)
                        error.printStackTrace(pw)
                        writer.println(sw.toString())
                    }

                    writer.println("----------------------")
                    writer.println()
                    writer.flush()
                }
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Failed to write to error log")
            }
        }
    }

    fun showToast(context: Context, message: String, length: Int = Toast.LENGTH_SHORT) {
        CoroutineScope(Dispatchers.Main).launch {
            Toast.makeText(context, message, length).show()
        }
    }

    @Suppress("SameParameterValue")
    private fun cleanupOldLogs(logDir: File, keepCount: Int) {
        try {
            val logFiles = logDir.listFiles { file ->
                file.isFile && file.name.startsWith(LOG_FILE_PREFIX)
            }

            if (logFiles != null && logFiles.size > keepCount) {
                val sortedFiles = logFiles.sortedBy { it.lastModified() }

                for (i in 0 until sortedFiles.size - keepCount) {
                    sortedFiles[i].delete()
                }
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Failed to cleanup old logs")
        }
    }
}