package com.grindrplus.bridge

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.Process
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import com.grindrplus.core.LogSource
import com.grindrplus.core.Logger
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
    private val periodicTasksExecutor = Executors.newSingleThreadScheduledExecutor()

    private var isForegroundStarted = false

    override fun onCreate() {
        super.onCreate()
        Logger.i("BridgeService created", LogSource.BRIDGE)

        Process.setThreadPriority(Process.THREAD_PRIORITY_FOREGROUND)
        initializeFiles()
    }

    override fun onBind(intent: Intent?): IBinder {
        Logger.i("BridgeService bound", LogSource.BRIDGE)
        startForegroundSafely()
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Logger.i("BridgeService started", LogSource.BRIDGE)

        startForegroundSafely()

        return START_STICKY
    }

    private fun startForegroundSafely() {
        if (isForegroundStarted) {
            return
        }

        try {
            val channelId = "bridge_service_channel"
            createNotificationChannel(channelId, "GrindrPlus Background Service", "Keeps GrindrPlus running in background")

            val notification = NotificationCompat.Builder(this, channelId)
                .setContentTitle("GrindrPlus")
                .setContentText("Background service active")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .setVisibility(NotificationCompat.VISIBILITY_SECRET)
                .setOngoing(true)
                .setShowWhen(false)
                .build()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceCompat.startForeground(
                    this,
                    1001,
                    notification,
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                    } else {
                        0
                    }
                )
            } else {
                startForeground(1001, notification)
            }

            isForegroundStarted = true
            Logger.i("Foreground service started successfully", LogSource.BRIDGE)

        } catch (e: Exception) {
            Logger.w("Failed to start foreground service: ${e.message}", LogSource.BRIDGE)
            Logger.writeRaw(e.stackTraceToString())

            // If we can't start as foreground, continue as normal service
            // The service will still work, just won't be protected from being killed
            isForegroundStarted = false
        }
    }

    private fun initializeFiles() {
        ioExecutor.execute {
            try {
                if (!configFile.exists()) {
                    configFile.createNewFile()
                    configFile.writeText("{}")
                }

                if (!logFile.exists()) {
                    logFile.createNewFile()
                }

                if (!blockEventsFile.exists()) {
                    blockEventsFile.createNewFile()
                    blockEventsFile.writeText("[]")
                }
            } catch (e: Exception) {
                Logger.e("Failed to initialize files: ${e.message}", LogSource.BRIDGE)
                Logger.writeRaw(e.stackTraceToString())
            }
        }
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

        override fun clearLogs() {
            Logger.d("clearLogs() called")
            try {
                logLock.withLock {
                    if (logFile.exists()) {
                        logFile.delete()
                        logFile.createNewFile()
                    }
                }
            } catch (e: Exception) {
                Logger.e("Error clearing log file", LogSource.BRIDGE)
                Logger.writeRaw(e.stackTraceToString())
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

        override fun logBlockEvent(
            profileId: String,
            displayName: String,
            isBlock: Boolean,
            packageName: String
        ) {
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
                        Logger.d(
                            "Logged ${if (isBlock) "block" else "unblock"} event " +
                                    "for profile ${profileId.take(profileId.length - 4) + "****"}",
                            LogSource.BRIDGE
                        )
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

        override fun shouldRegenAndroidId(packageName: String): Boolean {
            val regenFile = File(getExternalFilesDir(null), "$packageName.android_id_regen")
            return regenFile.exists().also { exists ->
                if (exists) {
                    regenFile.delete()
                }
            }
        }

        override fun getForcedLocation(packageName: String): String {
            val coordinatesFile = File(getExternalFilesDir(null), "$packageName.location")
            return if (coordinatesFile.exists()) {
                coordinatesFile.readText().trim().ifBlank { "" }
            } else {
                ""
            }
        }

        override fun deleteForcedLocation(packageName: String) {
            val coordinatesFile = File(getExternalFilesDir(null), "$packageName.location")
            if (coordinatesFile.exists()) {
                coordinatesFile.delete()
            }
        }

        override fun isRooted(): Boolean {
            return com.grindrplus.manager.utils.isRooted(applicationContext)
        }

        override fun isLSPosed(): Boolean {
            return com.grindrplus.manager.utils.isLSPosed()
        }
    }

    private fun createNotificationChannel(
        channelId: String,
        channelName: String,
        channelDescription: String
    ) {
        val notificationManager: NotificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (notificationManager.getNotificationChannel(channelId) == null) {
            val importance = if (channelId == "bridge_service_channel") {
                NotificationManager.IMPORTANCE_MIN
            } else {
                NotificationManager.IMPORTANCE_DEFAULT
            }

            val channel = NotificationChannel(channelId, channelName, importance).apply {
                description = channelDescription
                if (channelId == "bridge_service_channel") {
                    setShowBadge(false)
                    setSound(null, null)
                    enableLights(false)
                    enableVibration(false)
                }
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
                setClassName(
                    applicationContext.packageName,
                    "${applicationContext.packageName}.bridge.NotificationActionReceiver"
                )
            }

            "VIEW_PROFILE" -> Intent("com.grindrplus.VIEW_PROFILE_ACTION").apply {
                putExtra("profileId", actionData)
                setPackage(applicationContext.packageName)
                setClassName(
                    applicationContext.packageName,
                    "${applicationContext.packageName}.bridge.NotificationActionReceiver"
                )
            }

            "CUSTOM" -> Intent("com.grindrplus.CUSTOM_ACTION").apply {
                putExtra("data", actionData)
                setPackage(applicationContext.packageName)
                setClassName(
                    applicationContext.packageName,
                    "${applicationContext.packageName}.bridge.NotificationActionReceiver"
                )
            }

            else -> Intent("com.grindrplus.DEFAULT_ACTION").apply {
                putExtra("data", actionData)
                setPackage(applicationContext.packageName)
                setClassName(
                    applicationContext.packageName,
                    "${applicationContext.packageName}.bridge.NotificationActionReceiver"
                )
            }
        }

        Logger.d("Action intent created: $actionType with data: $actionData", LogSource.BRIDGE)
        return intent
    }

    private fun formatLogEntry(
        level: String,
        source: String,
        message: String,
        hookName: String?
    ): String {
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

                val rotationMessage = "I/${
                    SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                        .format(Date())
                }/system: Log file rotated due to size limit\n"
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
        periodicTasksExecutor.shutdown()
    }

    companion object {
        private const val TAG = "BridgeService"
        const val CHANNEL_BLOCKS = "grindr_plus_blocks"
        const val CHANNEL_UNBLOCKS = "grindr_plus_unblocks"
        const val CHANNEL_GENERAL = "grindr_plus_general"
    }
}