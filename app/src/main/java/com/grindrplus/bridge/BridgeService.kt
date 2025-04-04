package com.grindrplus.bridge

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.Process
import timber.log.Timber
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class BridgeService : Service() {
    private val configFile by lazy { File(getExternalFilesDir(null), "grindrplus.json") }
    private val logFile by lazy { File(getExternalFilesDir(null), "grindrplus.log") }
    private val ioExecutor = Executors.newSingleThreadExecutor()
    private val logLock = ReentrantLock()
    private val MAX_LOG_SIZE = 5 * 1024 * 1024

    override fun onCreate() {
        super.onCreate()
        Timber.tag(TAG).d("BridgeService created")

        ioExecutor.execute {
            if (!configFile.exists()) {
                try {
                    configFile.createNewFile()
                    configFile.writeText("{}")
                } catch (e: Exception) {
                    Timber.tag(TAG).e(e, "Failed to create config file")
                }
            }

            if (!logFile.exists()) {
                try {
                    logFile.createNewFile()
                } catch (e: Exception) {
                    Timber.tag(TAG).e(e, "Failed to create log file")
                }
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder {
        Timber.tag(TAG).d("Client binding to BridgeService")
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Timber.tag(TAG).d("onStartCommand")
        Process.setThreadPriority(Process.THREAD_PRIORITY_FOREGROUND)
        return START_STICKY
    }

    private val binder = object : IBridgeService.Stub() {
        override fun getConfig(): String {
            Timber.tag(TAG).d("getConfig() called")
            return try {
                if (!configFile.exists()) {
                    configFile.createNewFile()
                    "{}"
                } else {
                    configFile.readText().ifBlank { "{}" }
                }
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Error reading config file")
                "{}"
            }
        }

        override fun setConfig(config: String?) {
            Timber.tag(TAG).d("setConfig() called")
            try {
                if (!configFile.exists()) {
                    configFile.createNewFile()
                }

                configFile.writeText(config ?: "{}")
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Error writing config file")
            }
        }

        override fun log(level: String, source: String, message: String, hookName: String?) {
            ioExecutor.execute {
                try {
                    checkAndManageLogSize()
                    val formattedLog = formatLogEntry(level, source, message, hookName)
                    appendToLog(formattedLog)
                } catch (e: Exception) {
                    Timber.tag(TAG).e(e, "Error writing to log file")
                }
            }
        }

        override fun writeRawLog(content: String) {
            ioExecutor.execute {
                try {
                    checkAndManageLogSize()
                    appendToLog(content + (if (!content.endsWith("\n")) "\n" else ""))
                } catch (e: Exception) {
                    Timber.tag(TAG).e(e, "Error writing raw content to log file")
                }
            }
        }
    }

    private fun formatLogEntry(level: String, source: String, message: String, hookName: String?): String {
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
            .format(Date())

        return if (hookName != null) {
            "[$timestamp][$source][$level][$hookName] $message\n"
        } else {
            "[$timestamp][$source][$level] $message\n"
        }
    }

    private fun checkAndManageLogSize() {
        logLock.withLock {
            if (logFile.exists() && logFile.length() > MAX_LOG_SIZE) {
                val backupFile = File("${logFile.absolutePath}.bak")
                backupFile.takeIf { it.exists() }?.delete()

                logFile.renameTo(backupFile)
                logFile.createNewFile()

                val rotationMessage = "I/${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                    .format(Date())}/system: Log file rotated due to size limit\n"
                logFile.appendText(rotationMessage)
            }
        }
    }

    private fun appendToLog(content: String) {
        logLock.withLock {
            if (!logFile.exists()) {
                logFile.createNewFile()
            }
            logFile.appendText(content)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.tag(TAG).d("BridgeService destroyed")
        ioExecutor.shutdown()
    }

    companion object {
        private const val TAG = "BridgeService"
    }
}