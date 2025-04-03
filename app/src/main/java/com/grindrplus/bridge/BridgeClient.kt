package com.grindrplus.bridge

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.IBinder
import com.grindrplus.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import timber.log.Timber
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

    private fun safeLog(message: String) {
        try {
            Timber.tag(TAG).d(message)
        } catch (e: Exception) {
            println("BridgeClient: $message")
        }
    }

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            bridgeService = IBridgeService.Stub.asInterface(binder)
            isBound.set(true)
            isConnecting.set(false)
            connectionLatch.countDown()
            safeLog("Connected to bridge service")
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            bridgeService = null
            isBound.set(false)
            safeLog("Disconnected from bridge service")
        }
    }

    fun connect(onConnected: (() -> Unit)? = null) {
        if (isBound.get()) {
            onConnected?.invoke()
            return
        }

        if (isConnecting.getAndSet(true)) {
            safeLog("Connection already in progress")
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
            safeLog("Error binding service: ${e.message}")
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
                safeLog("Connection timeout in async mode")
                isConnecting.set(false)
            }
        }
    }

    fun connectBlocking(timeoutMs: Long = 10000): Boolean {
        if (isBound.get()) {
            return true
        }

        safeLog("Attempting to connect to bridge service (blocking)")

        if (isConnecting.getAndSet(true)) {
            safeLog("Connection already in progress, waiting...")
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
            safeLog("Error binding service: ${e.message}")
            isConnecting.set(false)
            return false
        }

        val result = connectionLatch.await(timeoutMs, TimeUnit.MILLISECONDS)

        if (!result) {
            safeLog("Connection timeout in blocking mode")
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
            safeLog("Failed to start service directly: ${e.message}")
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
            safeLog("Failed to start ForceStartActivity: ${e.message}")
        }

        try {
            val broadcastIntent = Intent("com.grindrplus.START_BRIDGE_SERVICE").apply {
                setPackage(BuildConfig.APPLICATION_ID)
            }
            context.sendBroadcast(broadcastIntent)
        } catch (e: Exception) {
            safeLog("Failed to broadcast service start intent: ${e.message}")
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
                safeLog("Error unbinding service: ${e.message}")
            }
        }
    }

    fun getConfig(): JSONObject {
        if (!isBound.get()) {
            safeLog("Cannot get config, service not bound")
            return JSONObject()
        }

        return try {
            bridgeService?.config?.let { JSONObject(it) } ?: JSONObject()
        } catch (e: Exception) {
            safeLog("Error getting config: ${e.message}")
            JSONObject()
        }
    }

    fun setConfig(config: JSONObject) {
        if (!isBound.get()) {
            safeLog("Cannot set config, service not bound")
            return
        }

        try {
            bridgeService?.setConfig(config.toString(4))
        } catch (e: Exception) {
            safeLog("Error setting config: ${e.message}")
        }
    }
}