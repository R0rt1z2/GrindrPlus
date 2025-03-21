package com.grindrplus.bridge

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.RemoteException
import android.util.Log
import java.io.File
import java.io.InputStream

class BridgeService : Service() {
    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    private val binder = object : IBridgeService.Stub() {
        @Throws(RemoteException::class)
        override fun getTranslation(locale: String): String {
            val assetPath = "translations/$locale.json"
            return try {
                val inputStream: InputStream = assets.open(assetPath)
                val size: Int = inputStream.available()
                val buffer = ByteArray(size)
                inputStream.read(buffer)
                inputStream.close()

                String(buffer)
            } catch (e: Exception) {
                Log.e("BridgeService", "Error loading translation file: $assetPath", e)
                "{\"error\": \"Translation file not found or failed to load\"}"
            }
        }

        @Throws(RemoteException::class)
        override fun getAvailableTranslations(): List<String> {
            return try {
                assets.list("translations")?.map { it.removeSuffix(".json") } ?: emptyList()
            } catch (e: Exception) {
                Log.e("BridgeService", "Error listing translation files", e)
                emptyList()
            }
        }

        override fun getConfig(): String {
            println("Called getConfig on ${getExternalFilesDir(null)}")
            val configFile = File(getExternalFilesDir(null), "grindrplus.json");
            return try {
                configFile.readText()
            } catch (e: Exception) {
                Log.e("BridgeService", "Error reading config file", e)
                "{}"
            }
        }

        override fun setConfig(config: String?) {
            println("Called setConfig on ${getExternalFilesDir(null)}")
            val configFile = File(getExternalFilesDir(null), "grindrplus.json");
            try {
                configFile.writeText(config ?: "{}")
            } catch (e: Exception) {
                Log.e("BridgeService", "Error writing config file", e)
            }
        }
    }
}
