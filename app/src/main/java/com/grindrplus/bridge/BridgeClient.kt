
package com.grindrplus.bridge

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.IBinder
import com.grindrplus.BuildConfig
import com.grindrplus.core.Logger
import com.grindrplus.core.LogSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class BridgeClient(private val context: Context) {
    private val TAG = "BridgeClient"
    private var bridgeService: IBridgeService? = null
    private val isConnecting = AtomicBoolean(false)
    private val isBound = AtomicBoolean(false)
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private val connectionLatch = CountDownLatch(1)

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
        }
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

        startServiceMultipleWays()

        val intent = Intent().apply {
            setClassName(
                BuildConfig.APPLICATION_ID,
                "${BuildConfig.APPLICATION_ID}.bridge.BridgeService"
            )
        }

        try {
            bindServiceProperly(intent)
        } catch (e: Exception) {
            Logger.e("Error binding service: ${e.message}", LogSource.BRIDGE)
            isConnecting.set(false)
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

        startServiceMultipleWays()

        val intent = Intent().apply {
            setClassName(
                BuildConfig.APPLICATION_ID,
                "${BuildConfig.APPLICATION_ID}.bridge.BridgeService"
            )
        }

        try {
            bindServiceProperly(intent)
        } catch (e: Exception) {
            Logger.e("Error binding service: ${e.message}", LogSource.BRIDGE)
            isConnecting.set(false)
            return false
        }

        val result = connectionLatch.await(timeoutMs, TimeUnit.MILLISECONDS)

        if (!result) {
            Logger.w("Connection timeout in blocking mode", LogSource.BRIDGE)
            isConnecting.set(false)
        }

        return result
    }

    private fun startServiceMultipleWays() {
        try {
            val serviceIntent = Intent().apply {
                setClassName(
                    BuildConfig.APPLICATION_ID,
                    "${BuildConfig.APPLICATION_ID}.bridge.BridgeService"
                )
            }
            context.startService(serviceIntent)
        } catch (e: Exception) {
            Logger.w("Failed to start service directly: ${e.message}", LogSource.BRIDGE)
        }

        try {
            val forceStartIntent = Intent().apply {
                setClassName(
                    BuildConfig.APPLICATION_ID,
                    "${BuildConfig.APPLICATION_ID}.bridge.ForceStartActivity"
                )
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(forceStartIntent)
        } catch (e: Exception) {
            Logger.w("Failed to start ForceStartActivity: ${e.message}", LogSource.BRIDGE)
        }

        try {
            val broadcastIntent = Intent("com.grindrplus.START_BRIDGE_SERVICE").apply {
                setPackage(BuildConfig.APPLICATION_ID)
            }
            context.sendBroadcast(broadcastIntent)
        } catch (e: Exception) {
            Logger.w("Failed to broadcast service start intent: ${e.message}", LogSource.BRIDGE)
        }
    }

    private fun bindServiceProperly(intent: Intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            context.bindService(
                intent,
                Context.BIND_AUTO_CREATE,
                Executors.newSingleThreadExecutor(),
                connection
            )
        } else {
            context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
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
            Logger.w("Cannot get config, service not bound", LogSource.BRIDGE)
            return JSONObject()
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
            Logger.w("Cannot set config, service not bound", LogSource.BRIDGE)
            return
        }

        try {
            bridgeService?.setConfig(config.toString(4))
        } catch (e: Exception) {
            Logger.e("Error setting config: ${e.message}", LogSource.BRIDGE)
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
            Logger.w("Cannot send notification, service not bound", LogSource.BRIDGE)
            return
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
            Logger.w("Cannot send notification, service not bound", LogSource.BRIDGE)
            return
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