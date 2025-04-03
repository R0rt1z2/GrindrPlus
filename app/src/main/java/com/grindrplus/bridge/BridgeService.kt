package com.grindrplus.bridge

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.Process
import timber.log.Timber
import java.io.File
import java.util.concurrent.Executors

class BridgeService : Service() {
    private val configFile by lazy { File(getExternalFilesDir(null), "grindrplus.json") }
    private val ioExecutor = Executors.newSingleThreadExecutor()

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