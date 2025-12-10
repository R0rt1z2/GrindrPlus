package com.grindrplus.bridge

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.Looper
import com.grindrplus.BuildConfig
import com.grindrplus.core.LogSource
import com.grindrplus.core.Logger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume

class BridgeClient(private val context: Context) {
    private var bridgeService: IBridgeService? = null
    private val isConnecting = AtomicBoolean(false)
    private val isBound = AtomicBoolean(false)
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val mainHandler = Handler(Looper.getMainLooper())
    private val serviceWatchdog = Handler(Looper.getMainLooper())
    private var lastConnectionAttempt = 0L
    private var connectionDeferreds = mutableMapOf<String, CompletableDeferred<Boolean>>()
    private val bindingExecutor = Executors.newSingleThreadExecutor()

    companion object {
        const val CONNECTION_TIMEOUT_MS = 5000L
        private const val WATCHDOG_CHECK_INTERVAL_MS = 30000L
        private const val RECONNECT_DELAY_MS = 2000L
    }

    init {
        Logger.initialize(context, this, false)
    }

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            bridgeService = IBridgeService.Stub.asInterface(binder)
            isBound.set(true)
            isConnecting.set(false)

            connectionDeferreds.forEach { (_, deferred) ->
                if (!deferred.isCompleted) deferred.complete(true)
            }
            connectionDeferreds.clear()

            Logger.i("Connected to bridge service", LogSource.BRIDGE)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            bridgeService = null
            isBound.set(false)
            Logger.i("Disconnected from bridge service", LogSource.BRIDGE)

            if (!isConnecting.get()) {
                mainHandler.postDelayed({
                    if (!isBound.get() && !isConnecting.get()) {
                        Logger.d("Auto-reconnecting after service disconnection", LogSource.BRIDGE)
                        coroutineScope.launch {
                            connect()
                        }
                    }
                }, RECONNECT_DELAY_MS)
            }
        }
    }

    private val watchdogRunnable = object : Runnable {
        override fun run() {
            if (!isBound.get() && !isConnecting.get()) {
                val now = System.currentTimeMillis()
                if (now - lastConnectionAttempt > 5000) {
                    Logger.w("Service watchdog detected disconnection, reconnecting...", LogSource.BRIDGE)
                    coroutineScope.launch {
                        connectWithRetry()
                    }
                }
            }
            serviceWatchdog.postDelayed(this, WATCHDOG_CHECK_INTERVAL_MS)
        }
    }

    fun startWatchdog() {
        serviceWatchdog.removeCallbacks(watchdogRunnable)
        serviceWatchdog.postDelayed(watchdogRunnable, WATCHDOG_CHECK_INTERVAL_MS)
        Logger.d("Started service watchdog", LogSource.BRIDGE)
    }

    fun stopWatchdog() {
        serviceWatchdog.removeCallbacks(watchdogRunnable)
        Logger.d("Stopped service watchdog", LogSource.BRIDGE)
    }

    fun isConnected(): Boolean {
        return isBound.get()
    }

    fun getService(): IBridgeService? = bridgeService

    suspend fun connectWithRetry(maxRetries: Int = 3, retryDelay: Long = 1000): Boolean {
        var attempts = 0
        var connected = false

        while (!connected && attempts < maxRetries) {
            attempts++
            Logger.d("Connection attempt $attempts/$maxRetries", LogSource.BRIDGE)

            connected = connect()

            if (connected) {
                Logger.i("Successfully connected on attempt $attempts", LogSource.BRIDGE)
                return true
            }

            if (attempts < maxRetries) {
                delay(retryDelay)
            }
        }

        if (!connected) {
            Logger.w("Failed to connect after $maxRetries attempts", LogSource.BRIDGE)
        }

        return connected
    }

    fun connectAsync(onConnected: ((Boolean) -> Unit)? = null) {
        coroutineScope.launch {
            val result = connect()
            withContext(Dispatchers.Main) {
                onConnected?.invoke(result)
            }
        }
    }

    suspend fun connect(): Boolean {
        if (isBound.get()) {
            return true
        }

        if (isConnecting.getAndSet(true)) {
            Logger.d("Connection already in progress, waiting...", LogSource.BRIDGE)
            val connectionKey = "connect-${System.currentTimeMillis()}"
            val deferred = CompletableDeferred<Boolean>()
            connectionDeferreds[connectionKey] = deferred

            try {
                return withTimeout(CONNECTION_TIMEOUT_MS) {
                    deferred.await()
                }
            } catch (e: Exception) {
                Logger.w("Timeout waiting for existing connection", LogSource.BRIDGE)
                connectionDeferreds.remove(connectionKey)
                return false
            }
        }

        lastConnectionAttempt = System.currentTimeMillis()

        try {
            startService()
        } catch (e: Exception) {
            Logger.e("Failed to start service: ${e.message}", LogSource.BRIDGE)
            isConnecting.set(false)
            return false
        }

        val intent = Intent().apply {
            setClassName(
                BuildConfig.APPLICATION_ID,
                BridgeService::class.java.name
            )
        }

        return suspendCancellableCoroutine { continuation ->
            val bindResult = try {
                bindServiceSafely(intent)
            } catch (e: Exception) {
                Logger.e("Error binding service: ${e.message}", LogSource.BRIDGE)
                isConnecting.set(false)
                continuation.resume(false)
                return@suspendCancellableCoroutine
            }

            if (!bindResult) {
                Logger.w("bindService returned false", LogSource.BRIDGE)
                isConnecting.set(false)
                continuation.resume(false)
                return@suspendCancellableCoroutine
            }

            val timeoutHandler = Handler(Looper.getMainLooper())
            val timeoutOccurred = AtomicBoolean(false)

            val timeoutRunnable = Runnable {
                if (continuation.isActive && !timeoutOccurred.getAndSet(true)) {
                    Logger.w("Connection timeout", LogSource.BRIDGE)
                    try {
                        context.unbindService(connection)
                    } catch (e: Exception) {
                        Logger.e("Error unbinding service after timeout: ${e.message}", LogSource.BRIDGE)
                    }
                    isConnecting.set(false)
                    continuation.resume(false)
                }
            }

            timeoutHandler.postDelayed(timeoutRunnable, CONNECTION_TIMEOUT_MS)

            continuation.invokeOnCancellation {
                timeoutHandler.removeCallbacks(timeoutRunnable)
                try {
                    context.unbindService(connection)
                } catch (e: Exception) {
                    Logger.e("Error unbinding service on cancellation: ${e.message}", LogSource.BRIDGE)
                }
                isConnecting.set(false)
            }

            val connectionKey = "connect-${continuation.hashCode()}"
            val deferred = CompletableDeferred<Boolean>()
            connectionDeferreds[connectionKey] = deferred

            coroutineScope.launch {
                val result = try {
                    withTimeout(CONNECTION_TIMEOUT_MS) {
                        deferred.await()
                    }
                } catch (_: Exception) {
                    false
                }

                connectionDeferreds.remove(connectionKey)
                timeoutHandler.removeCallbacks(timeoutRunnable)

                if (continuation.isActive && !timeoutOccurred.get()) {
                    continuation.resume(result)
                }
            }
        }
    }

    fun connectBlocking(timeoutMs: Long = CONNECTION_TIMEOUT_MS): Boolean {
        if (isBound.get()) {
            return true
        }

        Logger.d("Attempting to connect to bridge service (blocking)", LogSource.BRIDGE)

        val result = runBlocking {
            try {
                withTimeout(timeoutMs) {
                    connect()
                }
            } catch (e: Exception) {
                Logger.w("Connection timeout in blocking mode", LogSource.BRIDGE)
                false
            }
        }

        return result
    }

    private fun bindServiceSafely(intent: Intent): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            context.bindService(
                intent,
                Context.BIND_AUTO_CREATE,
                bindingExecutor,
                connection
            )
        } else {
            try {
                val handlerThread = HandlerThread("BridgeClientThread")
                handlerThread.start()
                val handler = Handler(handlerThread.looper)

                val bindResult = try {
                    val userHandle = Process::class.java.getMethod("myUserHandle").invoke(null)
                    context.javaClass.getMethod(
                        "bindServiceAsUser",
                        Intent::class.java,
                        ServiceConnection::class.java,
                        Int::class.javaPrimitiveType,
                        Handler::class.java,
                        userHandle.javaClass
                    ).invoke(
                        context,
                        intent,
                        connection,
                        Context.BIND_AUTO_CREATE,
                        handler,
                        userHandle
                    ) as Boolean
                } catch (e: Exception) {
                    Logger.w("bindServiceAsUser failed, falling back to bindService: ${e.message}", LogSource.BRIDGE)
                    context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
                }

                bindResult
            } catch (e: Exception) {
                Logger.e("Failed to bind service with any method: ${e.message}", LogSource.BRIDGE)
                context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
            }
        }
    }

    private fun startService() {
        try {
            val serviceIntent = Intent().apply {
                setClassName(
                    BuildConfig.APPLICATION_ID,
                    "${BuildConfig.APPLICATION_ID}.bridge.BridgeService"
                )
            }

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                    Logger.d("Service start attempt via startForegroundService", LogSource.BRIDGE)
                } else {
                    context.startService(serviceIntent)
                    Logger.d("Service start attempt via startService", LogSource.BRIDGE)
                }
                Thread.sleep(100)
            } catch (e: Exception) {
                Logger.w("Failed to start service directly: ${e.message}", LogSource.BRIDGE)

                try {
                    val forceStartIntent = ForceStartActivity.createIntent(context)
                    context.startActivity(forceStartIntent)
                    Logger.d("Service start attempt via ForceStartActivity (fallback)", LogSource.BRIDGE)
                    Thread.sleep(50)
                } catch (e2: Exception) {
                    Logger.e("All service start methods failed: ${e2.message}", LogSource.BRIDGE)
                }
            }
        } catch (e: Exception) {
            Logger.e("Failed to start service: ${e.message}", LogSource.BRIDGE)
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

    fun shouldRegenAndroidId(packageName: String): Boolean {
        if (!isBound.get()) {
            if (connectBlocking(3000)) {
                Logger.d(
                    "Connected to service on-demand for shouldRegenAndroidId",
                    LogSource.BRIDGE
                )
            } else {
                Logger.w(
                    "Cannot check Android ID regeneration, service not bound",
                    LogSource.BRIDGE
                )
                return false
            }
        }

        return try {
            bridgeService?.shouldRegenAndroidId(packageName) ?: false
        } catch (e: Exception) {
            Logger.e("Error checking Android ID regeneration: ${e.message}", LogSource.BRIDGE)
            false
        }
    }

    fun getForcedLocation(packageName: String): String {
        if (!isBound.get()) {
            if (connectBlocking(3000)) {
                Logger.d("Connected to service on-demand for getForcedLocation", LogSource.BRIDGE)
            } else {
                Logger.w("Cannot get forced location, service not bound", LogSource.BRIDGE)
                return ""
            }
        }

        return try {
            bridgeService?.getForcedLocation(packageName) ?: ""
        } catch (e: Exception) {
            Logger.e("Error getting forced location: ${e.message}", LogSource.BRIDGE)
            ""
        }
    }

    fun deleteForcedLocation(packageName: String) {
        if (!isBound.get()) {
            if (connectBlocking(3000)) {
                Logger.d("Connected to service on-demand for deleteForcedLocation", LogSource.BRIDGE)
            } else {
                Logger.w("Cannot delete forced location, service not bound", LogSource.BRIDGE)
                return
            }
        }

        try {
            bridgeService?.deleteForcedLocation(packageName)
        } catch (e: Exception) {
            Logger.e("Error deleting forced location: ${e.message}", LogSource.BRIDGE)
        }
    }

    fun isRooted(): Boolean {
        if (!isBound.get()) {
            if (connectBlocking(3000)) {
                Logger.d("Connected to service on-demand for isRooted", LogSource.BRIDGE)
            } else {
                Logger.w("Cannot check root status, service not bound", LogSource.BRIDGE)
                return false
            }
        }

        return try {
            bridgeService?.isRooted() ?: false
        } catch (e: Exception) {
            Logger.e("Error checking root status: ${e.message}", LogSource.BRIDGE)
            false
        }
    }

    fun isLSPosed(): Boolean {
        if (!isBound.get()) {
            if (connectBlocking(3000)) {
                Logger.d("Connected to service on-demand for isLSPosed", LogSource.BRIDGE)
            } else {
                Logger.w("Cannot check LSPosed status, service not bound", LogSource.BRIDGE)
                return false
            }
        }

        return try {
            bridgeService?.isLSPosed() ?: false
        } catch (e: Exception) {
            Logger.e("Error checking LSPosed status: ${e.message}", LogSource.BRIDGE)
            false
        }
    }
}

private fun <T> runBlocking(block: suspend () -> T): T {
    return java.util.concurrent.CompletableFuture<T>().let { future ->
        CoroutineScope(Dispatchers.IO).launch {
            try {
                future.complete(block())
            } catch (e: Exception) {
                future.completeExceptionally(e)
            }
        }
        try {
            future.get(BridgeClient.CONNECTION_TIMEOUT_MS + 1000, TimeUnit.MILLISECONDS)
        } catch (e: Exception) {
            throw e.cause ?: e
        }
    }
}