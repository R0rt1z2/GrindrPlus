package com.grindrplus.bridge

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.os.Process
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.grindrplus.core.LogSource
import com.grindrplus.core.Logger
import okhttp3.internal.notify
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

@SuppressLint("MissingPermission")
class BridgeService : Service() {
    private val configFile by lazy { File(getExternalFilesDir(null), "grindrplus.json") }
    private val logFile by lazy { File(getExternalFilesDir(null), "grindrplus.log") }
    private val blockEventsFile by lazy { File(getExternalFilesDir(null), "block_events.json") }
    private val blockEventsLock = ReentrantLock()
    private val ioExecutor = Executors.newSingleThreadExecutor()
    private val logLock = ReentrantLock()
    private val MAX_LOG_SIZE = 5 * 1024 * 1024

    override fun onCreate() {
        super.onCreate()
        Logger.i("BridgeService created", LogSource.BRIDGE)

        ioExecutor.execute {
            if (!configFile.exists()) {
                try {
                    configFile.createNewFile()
                    configFile.writeText("{}")
                } catch (e: Exception) {
                    Logger.e("Failed to create config file", LogSource.BRIDGE)
                    Logger.writeRaw(e.stackTraceToString())
                }
            }

            if (!logFile.exists()) {
                try {
                    logFile.createNewFile()
                } catch (e: Exception) {
                    Logger.e("Failed to create log file", LogSource.BRIDGE)
                    Logger.writeRaw(e.stackTraceToString())
                }
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder {
        Logger.i("BridgeService bound", LogSource.BRIDGE)
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Logger.i("BridgeService started", LogSource.BRIDGE)
        Process.setThreadPriority(Process.THREAD_PRIORITY_FOREGROUND)
        return START_STICKY
    }

    private val binder = object : IBridgeService.Stub() {
        override fun getConfig(): String {
            Logger.d("getConfig() called")
            return try {
                if (!configFile.exists()) {
                    configFile.createNewFile()
                    "{}"
                } else {
                    configFile.readText().ifBlank { "{}" }
                }
            } catch (e: Exception) {
                Logger.e("Error reading config file", LogSource.BRIDGE)
                Logger.writeRaw(e.stackTraceToString())
                "{}"
            }
        }

        override fun setConfig(config: String?) {
            Logger.d("setConfig() called")
            try {
                if (!configFile.exists()) {
                    configFile.createNewFile()
                }

                configFile.writeText(config ?: "{}")
            } catch (e: Exception) {
                Logger.e("Error writing to config file", LogSource.BRIDGE)
                Logger.writeRaw(e.stackTraceToString())
            }
        }

        override fun log(level: String, source: String, message: String, hookName: String?) {
            ioExecutor.execute {
                try {
                    checkAndManageLogSize()
                    val formattedLog = formatLogEntry(level, source, message, hookName)
                    appendToLog(formattedLog)
                } catch (e: Exception) {
                    Logger.e("Error writing log entry", LogSource.BRIDGE)
                    Logger.writeRaw(e.stackTraceToString())
                }
            }
        }

        override fun writeRawLog(content: String) {
            ioExecutor.execute {
                try {
                    checkAndManageLogSize()
                    appendToLog(content + (if (!content.endsWith("\n")) "\n" else ""))
                } catch (e: Exception) {
                    Logger.e("Error writing raw log entry", LogSource.BRIDGE)
                    Logger.writeRaw(e.stackTraceToString())
                }
            }
        }

        override fun sendNotification(
            title: String,
            message: String,
            notificationId: Int,
            channelId: String,
            channelName: String,
            channelDescription: String
        ) {
            Logger.d("sendNotification() called")
            try {
                createNotificationChannel(channelId, channelName, channelDescription)

                val notificationBuilder = NotificationCompat.Builder(applicationContext, channelId)
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentTitle(title)
                    .setContentText(message)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setAutoCancel(true)

                with(NotificationManagerCompat.from(applicationContext)) {
                    notify(notificationId, notificationBuilder.build())
                }
            } catch (e: Exception) {
                Logger.e("Error sending notification", LogSource.BRIDGE)
                Logger.writeRaw(e.stackTraceToString())
            }
        }

        override fun sendNotificationWithActions(
            title: String,
            message: String,
            notificationId: Int,
            channelId: String,
            channelName: String,
            channelDescription: String,
            actionLabels: Array<String>,
            actionIntents: Array<String>,
            actionData: Array<String>
        ) {
            Logger.d("sendNotificationWithActions() called")
            try {
                createNotificationChannel(channelId, channelName, channelDescription)

                val notificationBuilder = NotificationCompat.Builder(applicationContext, channelId)
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentTitle(title)
                    .setContentText(message)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setAutoCancel(true)

                for (i in actionLabels.indices) {
                    if (i >= actionIntents.size || i >= actionData.size) break

                    val intent = createActionIntent(actionIntents[i], actionData[i])
                    val pendingIntent = PendingIntent.getBroadcast(
                        applicationContext,
                        notificationId + i,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )

                    notificationBuilder.addAction(
                        android.R.drawable.ic_menu_send,
                        actionLabels[i],
                        pendingIntent
                    )
                }

                with(NotificationManagerCompat.from(applicationContext)) {
                    notify(notificationId, notificationBuilder.build())
                }
            } catch (e: Exception) {
                Logger.e("Error sending notification with actions", LogSource.BRIDGE)
                Logger.writeRaw(e.stackTraceToString())
            }
        }

        override fun logBlockEvent(profileId: String, displayName: String, isBlock: Boolean, packageName: String) {
            ioExecutor.execute {
                try {
                    blockEventsLock.withLock {
                        if (!blockEventsFile.exists()) {
                            blockEventsFile.createNewFile()
                            blockEventsFile.writeText("[]")
                        }

                        val eventsArray = JSONArray(blockEventsFile.readText().ifBlank { "[]" })
                        val event = JSONObject().apply {
                            put("profileId", profileId)
                            put("displayName", displayName)
                            put("eventType", if (isBlock) "block" else "unblock")
                            put("timestamp", System.currentTimeMillis())
                            put("packageName", packageName)
                        }
                        eventsArray.put(event)
                        blockEventsFile.writeText(eventsArray.toString(4))
                        Logger.d("Logged ${if (isBlock) "block" else "unblock"} event " +
                                "for profile ${profileId.take(profileId.length - 4) + "****"}", LogSource.BRIDGE)
                    }
                } catch (e: Exception) {
                    Timber.tag(TAG).e(e, "Error logging block event")
                }
            }
        }

        override fun getBlockEvents(): String {
            return try {
                if (!blockEventsFile.exists()) {
                    blockEventsFile.createNewFile()
                    "[]"
                } else {
                    blockEventsFile.readText().ifBlank { "[]" }
                }
            } catch (e: Exception) {
                Logger.e("Error reading block events file", LogSource.BRIDGE)
                Logger.writeRaw(e.stackTraceToString())
                "[]"
            }
        }

        override fun clearBlockEvents() {
            blockEventsLock.withLock {
                try {
                    if (blockEventsFile.exists()) {
                        blockEventsFile.delete()
                        blockEventsFile.createNewFile()
                        blockEventsFile.writeText("[]")
                    }
                } catch (e: Exception) {
                    Logger.e("Error clearing block events file", LogSource.BRIDGE)
                    Logger.writeRaw(e.stackTraceToString())
                }
            }
        }
    }

    private fun createNotificationChannel(channelId: String, channelName: String, channelDescription: String) {
        val notificationManager: NotificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (notificationManager.getNotificationChannel(channelId) == null) {
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(channelId, channelName, importance).apply {
                description = channelDescription
            }
            notificationManager.createNotificationChannel(channel)
            Logger.d("Notification channel created: $channelId", LogSource.BRIDGE)
        }
    }

    private fun createActionIntent(actionType: String, actionData: String): Intent {
        val intent = when (actionType) {
            "COPY" -> Intent("com.grindrplus.COPY_ACTION").apply {
                putExtra("data", actionData)
                setPackage(applicationContext.packageName)
                setClassName(applicationContext.packageName,
                    "${applicationContext.packageName}.bridge.NotificationActionReceiver")
            }
            "VIEW_PROFILE" -> Intent("com.grindrplus.VIEW_PROFILE_ACTION").apply {
                putExtra("profileId", actionData)
                setPackage(applicationContext.packageName)
                setClassName(applicationContext.packageName,
                    "${applicationContext.packageName}.bridge.NotificationActionReceiver")
            }
            "CUSTOM" -> Intent("com.grindrplus.CUSTOM_ACTION").apply {
                putExtra("data", actionData)
                setPackage(applicationContext.packageName)
                setClassName(applicationContext.packageName,
                    "${applicationContext.packageName}.bridge.NotificationActionReceiver")
            }
            else -> Intent("com.grindrplus.DEFAULT_ACTION").apply {
                putExtra("data", actionData)
                setPackage(applicationContext.packageName)
                setClassName(applicationContext.packageName,
                    "${applicationContext.packageName}.bridge.NotificationActionReceiver")
            }
        }

        Logger.d("Action intent created: $actionType with data: $actionData", LogSource.BRIDGE)
        return intent
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
        Logger.i("BridgeService destroyed", LogSource.BRIDGE)
        ioExecutor.shutdown()
    }

    companion object {
        private const val TAG = "BridgeService"
        const val CHANNEL_BLOCKS = "grindr_plus_blocks"
        const val CHANNEL_UNBLOCKS = "grindr_plus_unblocks"
        const val CHANNEL_GENERAL = "grindr_plus_general"
    }
}