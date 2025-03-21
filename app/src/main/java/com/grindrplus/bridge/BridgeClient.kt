package com.grindrplus.bridge

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import com.grindrplus.BuildConfig
import com.grindrplus.GrindrPlus
import de.robv.android.xposed.XposedHelpers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject
import java.util.concurrent.Executors

class BridgeClient(private val context: Context) : ServiceConnection {
    private var isBound = false
    private var bridgeService: IBridgeService? = null
    private var onConnectedCallback: (() -> Unit)? = null

    override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
        this.bridgeService = IBridgeService.Stub.asInterface(binder)
        isBound = true
        println("Successfully connected to the bridge service!")
        this.onConnectedCallback?.invoke()
    }

    override fun onServiceDisconnected(name: ComponentName?) {
        bridgeService = null
        isBound = false
        GrindrPlus.logger.log("Disconnected from the bridge service!")
    }

    fun connect() {
        runCatching {
            val intent = Intent().setClassName(
                BuildConfig.APPLICATION_ID,
                BuildConfig.APPLICATION_ID + ".bridge.BridgeService"
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                context.bindService(
                    intent,
                    Context.BIND_AUTO_CREATE,
                    Executors.newSingleThreadExecutor(),
                    this
                )
            } else {
                XposedHelpers.callMethod(
                    context,
                    "bindServiceAsUser",
                    intent,
                    this,
                    Context.BIND_AUTO_CREATE,
                    Handler(HandlerThread("BridgeClient").apply { start() }.looper),
                    android.os.Process.myUserHandle()
                )
            }
        }.onFailure {
            println("Failed to bind to the bridge service: ${it.message}")
            throw it
        }
    }

    fun unbindService() {
        if (isBound) {
            context.unbindService(this)
            isBound = false
            println("Unbound from the bridge service!")
        }
    }

    /**
     * Get translations for the specified locale.
     * @param locale The locale to get translations for.
     * @return The translations for the specified locale (or null if not found).
     */
    fun getTranslation(locale: String): JSONObject? {
        if (!isBound) {
            println("Cannot get translation, service is not bound!")
            return null
        }

        return bridgeService?.getTranslation(locale)?.let {
            JSONObject(it)
        }
    }

    /**
     * Get list of available translations ("en_US", "es_ES", etc).
     * @return List of available translations.
     */
    fun getAvailableTranslations(): List<String> {
        if (!isBound) {
            println("Cannot get available translations, service is not bound!")
            return emptyList()
        }

        return bridgeService?.getAvailableTranslations() ?: emptyList()
    }

    fun getConfig(): JSONObject? {
        if (!isBound) {
            println("Cannot get config, service is not bound!")
            return null
        }

        return bridgeService?.getConfig()?.let {
            JSONObject(it)
        }

    }

    fun setConfig(config: JSONObject) {
        if (!isBound) {
            println("Cannot set config, service is not bound!")
            return
        }

        bridgeService?.setConfig(config.toString(4))
    }

}
