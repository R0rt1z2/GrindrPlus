package com.grindrplus.core

import android.content.Context
import android.util.Log
import com.grindrplus.GrindrPlus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.newSingleThreadContext
import org.json.JSONObject
import java.io.File
import java.io.IOException

object Config {
    @OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
    private val scope = CoroutineScope(newSingleThreadContext("Config"))
    private lateinit var configFile: File
    private var localConfig = JSONObject()

    suspend fun initialize(context: Context?) {
        if (context != null) {
            configFile = File(context.filesDir, "grindrplus.json")
            if (configFile.exists()) {
                File(
                    context.filesDir,
                    "pre-migration-config-backup-should-be-empty.json"
                ).writeText(readRemoteConfig().toString())
                writeRemoteConfig(JSONObject(configFile.readText()))
                configFile.delete()
            }
        }

        localConfig = readRemoteConfig()
    }

    private fun readRemoteConfig(): JSONObject {
        return try {
            GrindrPlus.bridgeClient.getConfig() ?: JSONObject().put("hooks", JSONObject())
        } catch (e: Exception) {
            Log.e("GrindrPlus", "Error reading config file", e)
            JSONObject().put("hooks", JSONObject())
        }
    }

    private fun writeRemoteConfig(json: JSONObject) {
        try {
            GrindrPlus.bridgeClient.setConfig(json)
        } catch (e: IOException) {
            Log.e("GrindrPlus", "Failed to write config file", e)
        }
    }

    fun put(name: String, value: Any) {
        localConfig.put(name, value)
        writeRemoteConfig(localConfig)
    }

    fun get(name: String, default: Any): Any {
        return readRemoteConfig().opt(name) ?: default.also { put(name, default) }
    }

    fun setHookEnabled(hookName: String, enabled: Boolean) {
        val hooks = localConfig.optJSONObject("hooks") ?: JSONObject().also { localConfig.put("hooks", it) }
        hooks.optJSONObject(hookName)?.put("enabled", enabled)
         writeRemoteConfig(localConfig)
    }

    fun isHookEnabled(hookName: String): Boolean {
        val hooks = readRemoteConfig().optJSONObject("hooks") ?: return false
        return hooks.optJSONObject(hookName)?.getBoolean("enabled") ?: false
    }

    suspend fun initHookSettings(name: String, description: String, state: Boolean) {
        if (localConfig.optJSONObject("hooks")?.optJSONObject(name) == null) {
            val hooks =
                localConfig.optJSONObject("hooks") ?: JSONObject().also { localConfig.put("hooks", it) }
            hooks.put(name, JSONObject().apply {
                put("description", description)
                put("enabled", state)
            })

            writeRemoteConfig(localConfig)
        }
    }

    fun getHooksSettings(): Map<String, Pair<String, Boolean>> {
        val hooks = readRemoteConfig().optJSONObject("hooks") ?: return emptyMap()
        val map = mutableMapOf<String, Pair<String, Boolean>>()

        val keys = hooks.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val obj = hooks.getJSONObject(key)
            map[key] = Pair(obj.getString("description"), obj.getBoolean("enabled"))
        }

        return map
    }
}
