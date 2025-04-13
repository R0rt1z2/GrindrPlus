package com.grindrplus.bridge

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import com.grindrplus.BuildConfig
import com.grindrplus.core.Logger
import com.grindrplus.core.LogSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class BridgeClient(private val context: Context) {
    private var bridgeService: IBridgeService? = null
    private val isConnecting = AtomicBoolean(false)
    private val isBound = AtomicBoolean(false)
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private var connectionLatch = CountDownLatch(1)
    private val serviceWatchdog = Handler(Looper.getMainLooper())
    private var lastConnectionAttempt = 0L

    init {
        Logger.initialize(context, this, false)
    }

    fun getService(): IBridgeService? = bridgeService

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            bridgeService = IBridgeService.Stub.asInterface(binder)
            isBound.set(true)
            isConnecting.set(false)
            connectionLatch.countDown()
            Logger.i("Connected to bridge service", LogSource.BRIDGE)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            bridgeService = null
            isBound.set(false)
            Logger.i("Disconnected from bridge service", LogSource.BRIDGE)

            if (!isConnecting.get()) {
                Handler(Looper.getMainLooper()).postDelayed({
                    if (!isBound.get() && !isConnecting.get()) {
                        Logger.d("Auto-reconnecting after service disconnection", LogSource.BRIDGE)
                        connect()
                    }
                }, 2000)
            }
        }
    }

    private val watchdogRunnable = object : Runnable {
        override fun run() {
            if (!isBound.get() && !isConnecting.get()) {
                val now = System.currentTimeMillis()
                if (now - lastConnectionAttempt > 5000) {
                    Logger.w("Service watchdog detected disconnection, reconnecting...", LogSource.BRIDGE)
                    connectWithRetry()
                }
            }
            serviceWatchdog.postDelayed(this, 30000)
        }
    }

    fun startWatchdog() {
        serviceWatchdog.removeCallbacks(watchdogRunnable)
        serviceWatchdog.postDelayed(watchdogRunnable, 30000)
        Logger.d("Started service watchdog", LogSource.BRIDGE)
    }

    fun stopWatchdog() {
        serviceWatchdog.removeCallbacks(watchdogRunnable)
        Logger.d("Stopped service watchdog", LogSource.BRIDGE)
    }

    fun connectWithRetry(maxRetries: Int = 3, retryDelay: Long = 1000, onConnected: (() -> Unit)? = null) {
        var attempts = 0

        fun tryConnect() {
            attempts++
            Logger.d("Connection attempt $attempts/$maxRetries", LogSource.BRIDGE)

            connect {
                Logger.i("Successfully connected on attempt $attempts", LogSource.BRIDGE)
                onConnected?.invoke()
            }

            coroutineScope.launch {
                delay(retryDelay)
                if (!isBound.get() && attempts < maxRetries && !isConnecting.get()) {
                    tryConnect()
                } else if (!isBound.get() && attempts >= maxRetries) {
                    Logger.w("Failed to connect after $maxRetries attempts", LogSource.BRIDGE)
                }
            }
        }

        tryConnect()
    }

    fun connect(onConnected: (() -> Unit)? = null) {
        if (isBound.get()) {
            onConnected?.invoke()
            return
        }

        if (isConnecting.getAndSet(true)) {
            Logger.d("Connection already in progress", LogSource.BRIDGE)
            return
        }

        lastConnectionAttempt = System.currentTimeMillis()
        connectionLatch = CountDownLatch(1)

        startService()

        val intent = Intent().apply {
            setClassName(
                BuildConfig.APPLICATION_ID,
                "${BuildConfig.APPLICATION_ID}.bridge.BridgeService"
            )
        }

        if (!bindServiceProperly(intent)) {
            return
        }

        coroutineScope.launch {
            val connected = connectionLatch.await(5000, TimeUnit.MILLISECONDS)

            if (connected) {
                withContext(Dispatchers.Main) {
                    onConnected?.invoke()
                }
            } else {
                Logger.w("Connection timeout in async mode", LogSource.BRIDGE)

                try {
                    context.unbindService(connection)
                } catch (e: Exception) {
                    Logger.e("Error unbinding service after timeout: ${e.message}", LogSource.BRIDGE)
                }

                isConnecting.set(false)
            }
        }
    }

    fun connectBlocking(timeoutMs: Long = 10000): Boolean {
        if (isBound.get()) {
            return true
        }

        Logger.d("Attempting to connect to bridge service (blocking)", LogSource.BRIDGE)

        if (isConnecting.getAndSet(true)) {
            Logger.d("Connection already in progress, waiting...", LogSource.BRIDGE)
            return connectionLatch.await(timeoutMs, TimeUnit.MILLISECONDS)
        }

        lastConnectionAttempt = System.currentTimeMillis()
        connectionLatch = CountDownLatch(1)

        startService()

        val intent = Intent().apply {
            setClassName(
                BuildConfig.APPLICATION_ID,
                "${BuildConfig.APPLICATION_ID}.bridge.BridgeService"
            )
        }

        if (!bindServiceProperly(intent)) {
            return false
        }

        val result = connectionLatch.await(timeoutMs, TimeUnit.MILLISECONDS)

        if (!result) {
            Logger.w("Connection timeout in blocking mode", LogSource.BRIDGE)

            try {
                context.unbindService(connection)
            } catch (e: Exception) {
                Logger.e("Error unbinding service after timeout: ${e.message}", LogSource.BRIDGE)
            }

            isConnecting.set(false)
        }

        return result
    }

    private fun startService() {
        try {
            val serviceIntent = Intent().apply {
                setClassName(
                    BuildConfig.APPLICATION_ID,
                    "${BuildConfig.APPLICATION_ID}.bridge.BridgeService"
                )
            }
            context.startService(serviceIntent)
            Logger.d("Service start attempt via startService", LogSource.BRIDGE)
        } catch (e: Exception) {
            Logger.w("Failed to start service directly: ${e.message}", LogSource.BRIDGE)

            try {
                val forceStartIntent = Intent().apply {
                    setClassName(
                        BuildConfig.APPLICATION_ID,
                        "${BuildConfig.APPLICATION_ID}.bridge.ForceStartActivity"
                    )
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(forceStartIntent)
                Logger.d("Service start attempt via ForceStartActivity", LogSource.BRIDGE)
            } catch (e: Exception) {
                Logger.w("Failed to start ForceStartActivity: ${e.message}", LogSource.BRIDGE)
            }
        }
    }

    private fun bindServiceProperly(intent: Intent): Boolean {
        try {
            val bindResult = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                context.bindService(
                    intent,
                    Context.BIND_AUTO_CREATE,
                    Executors.newSingleThreadExecutor(),
                    connection
                )
            } else {
                context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
            }

            if (!bindResult) {
                Logger.w("bindService returned false", LogSource.BRIDGE)
                isConnecting.set(false)
                return false
            }

            return true
        } catch (e: Exception) {
            Logger.e("Error binding service: ${e.message}", LogSource.BRIDGE)
            isConnecting.set(false)
            return false
        }
    }

    fun unbind() {
        if (isBound.getAndSet(false)) {
            try {
                context.unbindService(connection)
                bridgeService = null
            } catch (e: Exception) {
                Logger.e("Error unbinding service: ${e.message}", LogSource.BRIDGE)
            }
        }
    }

    fun getConfig(): JSONObject {
        if (!isBound.get()) {
            if (connectBlocking(3000)) {
                Logger.d("Connected to service on-demand for getConfig", LogSource.BRIDGE)
            } else {
                Logger.w("Cannot get config, service not bound", LogSource.BRIDGE)
                return JSONObject()
            }
        }

        return try {
            bridgeService?.config?.let { JSONObject(it) } ?: JSONObject()
        } catch (e: Exception) {
            Logger.e("Error getting config: ${e.message}", LogSource.BRIDGE)
            JSONObject()
        }
    }

    fun setConfig(config: JSONObject) {
        if (!isBound.get()) {
            if (connectBlocking(3000)) {
                Logger.d("Connected to service on-demand for setConfig", LogSource.BRIDGE)
            } else {
                Logger.w("Cannot set config, service not bound", LogSource.BRIDGE)
                return
            }
        }

        try {
            bridgeService?.setConfig(config.toString(4))
        } catch (e: Exception) {
            Logger.e("Error setting config: ${e.message}", LogSource.BRIDGE)
        }
    }

    fun logBlockEvent(profileId: String, displayName: String, isBlock: Boolean, packageName: String) {
        if (!isBound.get()) {
            if (connectBlocking(3000)) {
                Logger.d("Connected to service on-demand for logBlockEvent", LogSource.BRIDGE)
            } else {
                Logger.w("Cannot log block event, service not bound", LogSource.BRIDGE)
                return
            }
        }

        try {
            bridgeService?.logBlockEvent(profileId, displayName, isBlock, packageName)
        } catch (e: Exception) {
            Logger.e("Error logging block event: ${e.message}", LogSource.BRIDGE)
        }
    }

    fun getBlockEvents(): JSONArray {
        if (!isBound.get()) {
            if (connectBlocking(3000)) {
                Logger.d("Connected to service on-demand for getBlockEvents", LogSource.BRIDGE)
            } else {
                Logger.w("Cannot get block events, service not bound", LogSource.BRIDGE)
                return JSONArray()
            }
        }

        return try {
            bridgeService?.blockEvents?.let { JSONArray(it) } ?: JSONArray()
        } catch (e: Exception) {
            Logger.e("Error getting block events: ${e.message}", LogSource.BRIDGE)
            JSONArray()
        }
    }

    fun clearBlockEvents() {
        if (!isBound.get()) {
            if (connectBlocking(3000)) {
                Logger.d("Connected to service on-demand for clearBlockEvents", LogSource.BRIDGE)
            } else {
                Logger.w("Cannot clear block events, service not bound", LogSource.BRIDGE)
                return
            }
        }

        try {
            bridgeService?.clearBlockEvents()
        } catch (e: Exception) {
            Logger.e("Error clearing block events: ${e.message}", LogSource.BRIDGE)
        }
    }

    fun sendNotification(
        title: String,
        message: String,
        notificationId: Int,
        channelId: String = "default_channel_id",
        channelName: String = "Default Channel",
        channelDescription: String = "Default notifications"
    ) {
        if (!isBound.get()) {
            if (connectBlocking(3000)) {
                Logger.d("Connected to service on-demand for sendNotification", LogSource.BRIDGE)
            } else {
                Logger.w("Cannot send notification, service not bound", LogSource.BRIDGE)
                return
            }
        }

        try {
            bridgeService?.sendNotification(
                title,
                message,
                notificationId,
                channelId,
                channelName,
                channelDescription
            )
        } catch (e: Exception) {
            Logger.e("Error sending notification: ${e.message}", LogSource.BRIDGE)
        }
    }

    fun sendNotificationWithMultipleActions(
        title: String,
        message: String,
        notificationId: Int,
        actionLabels: List<String>,
        actionTypes: List<String>,
        actionData: List<String>,
        channelId: String = "default_channel_id",
        channelName: String = "Default Channel",
        channelDescription: String = "Default notifications"
    ) {
        if (!isBound.get()) {
            if (connectBlocking(3000)) {
                Logger.d("Connected to service on-demand for sendNotificationWithActions", LogSource.BRIDGE)
            } else {
                Logger.w("Cannot send notification, service not bound", LogSource.BRIDGE)
                return
            }
        }

        try {
            bridgeService?.sendNotificationWithActions(
                title,
                message,
                notificationId,
                channelId,
                channelName,
                channelDescription,
                actionLabels.toTypedArray(),
                actionTypes.toTypedArray(),
                actionData.toTypedArray()
            )
        } catch (e: Exception) {
            Logger.e("Error sending notification with multiple actions: ${e.message}", LogSource.BRIDGE)
        }
    }
}