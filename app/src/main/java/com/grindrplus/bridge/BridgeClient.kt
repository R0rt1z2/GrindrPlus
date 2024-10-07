package com.grindrplus.bridge

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.util.Log
import com.grindrplus.BuildConfig
import com.grindrplus.GrindrPlus
import de.robv.android.xposed.XposedHelpers
import org.json.JSONObject
import java.util.concurrent.Executors

class BridgeClient(private val context: Context) : ServiceConnection {
    private val TAG = "GrindrPlus"
    private var isBound = false
    private var bridgeService: IBridgeService? = null
    private var onConnectedCallback: (() -> Unit)? = null

    override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
        this.bridgeService = IBridgeService.Stub.asInterface(binder)
        isBound = true
        Log.e(TAG,"Successfully connected to the bridge service!")
        this.onConnectedCallback?.invoke()
    }

    override fun onServiceDisconnected(name: ComponentName?) {
        bridgeService = null
        isBound = false
        Log.e(TAG,"Disconnected from the bridge service!")
    }

    fun connect(onConnected: () -> Unit) {
        runCatching {
            val intent = Intent().setClassName(
                BuildConfig.APPLICATION_ID,
                BuildConfig.APPLICATION_ID + ".bridge.BridgeService"
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                context.bindService(
                    intent,
                    this,
                    Context.BIND_AUTO_CREATE
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
            Log.e(TAG,"Failed to bind to the bridge service: ${it.message}")
        }.onSuccess {
            Log.i(TAG,"Successfully bound to the bridge service!")
            onConnectedCallback = onConnected
        }
    }

    fun unbindService() {
        if (isBound) {
            context.unbindService(this)
            isBound = false
            Log.i(TAG,"Unbound from the bridge service!")
        }
    }

    /**
     * Get translations for the specified locale.
     * @param locale The locale to get translations for.
     * @return The translations for the specified locale (or null if not found).
     */
    fun getTranslation(locale: String): JSONObject? {
        if (!isBound) {
            Log.e(TAG,"Cannot get translation, service is not bound!")
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
            Log.e(TAG,"Cannot get available translations, service is not bound!")
            return emptyList()
        }

        return bridgeService?.getAvailableTranslations() ?: emptyList()
    }

    /**
     * Gets the state of the specified hook.
     * @param name The name of the hook to get the state of.
     * @param description The description of the hook.
     * @return True if the hook is enabled, false otherwise.
     */
    fun isHookEnabled(name: String, description: String): Boolean {
        Log.i(TAG,"Checking if hook '$name' is enabled...")
        if (!isBound) {
            Log.e(TAG,"Cannot get hook state, service is not bound!")
            return false
        }

        try {
            return bridgeService?.isHookEnabled(name) ?: false
        } catch (e: org.json.JSONException) {
            Log.e(TAG,"Hook '$name' not found in configuration, adding it...")
            return addHook(name, description, false)
        }
    }

    /**
     * Sets the state of the specified hook.
     * @param name The name of the hook to set the state of.
     * @param enabled True to enable the hook, false to disable it.
     */
    fun setHookState(name: String, enabled: Boolean) {
        if (!isBound) {
            Log.e(TAG, "Cannot set hook state, service is not bound!")
            return
        }

        bridgeService?.setHookState(name, enabled)
    }

    /**
     * Add hook to the configuration.
     * @param name The name of the hook to add.
     * @param description The description of the hook.
     * @param enabled True if the hook is enabled, false otherwise.
     * @return True if the hook was added successfully, false otherwise.
     */
    fun addHook(name: String, description: String, enabled: Boolean): Boolean {
        if (!isBound) {
            Log.e(TAG, "Cannot add hook, service is not bound!")
            return false
        }

        try {
            val hookObject = JSONObject().apply {
                put("description", description)
                put("enabled", enabled)
            }

            val result = bridgeService?.addHook(name, hookObject.toString(), true) ?: false
            if (result) {
                Log.i(TAG, "Hook '$name' added successfully!")
            } else {
                Log.e(TAG, "Failed to add hook '$name'")
            }

            return result
        } catch (e: org.json.JSONException) {
            Log.e(TAG, "Failed to add hook '$name'", e)
            return false
        }
    }
}
