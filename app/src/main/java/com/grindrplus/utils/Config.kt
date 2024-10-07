package com.grindrplus.utils

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.newSingleThreadContext
import org.json.JSONObject
import java.io.File
import java.io.IOException

data class HookConfig(val name: String, val description: String, var enabled: Boolean = false)

object Config {
    private const val TAG = "Config"
    private lateinit var configFile: File
    private lateinit var config: JSONObject

    @OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
    private val scope = CoroutineScope(newSingleThreadContext("Config"))

    // The following list holds all the configurations for each hook.
    private val hooks = mutableListOf<HookConfig>()

    init {
        // Initialize all the hook configurations
        hooks.add(HookConfig("Enable unlimited", "Enable Grindr Unlimited features", true))
        hooks.add(HookConfig("Allow screenshots", "Allow screenshots everywhere in the app", true))
        hooks.add(HookConfig("Chat indicators", "Don't show chat markers / indicators to others", true))
        hooks.add(HookConfig("Chat terminal", "Create a chat terminal to execute commands", true))
        hooks.add(HookConfig("Disable boosting", "Get rid of all upsells related to boosting", true))
        hooks.add(HookConfig("Disable updates", "Disable forced updates", true))
        hooks.add(HookConfig("Disable analytics", "Disable Grindr analytics (data collection)", true))
        hooks.add(HookConfig("Expiring photos", "Allow unlimited photo viewing", true))
        hooks.add(HookConfig("Favorites", "Customize layout for the favorites tab", true))
        hooks.add(HookConfig("Feature granting", "Grant all Grindr features", true))
        hooks.add(HookConfig("Local saved phrases", "Save unlimited phrases locally", true))
        hooks.add(HookConfig("Location spoofer", "Spoof your location", true))
        hooks.add(HookConfig("Mod settings", "GrindrPlus settings", true))
        hooks.add(HookConfig("Online indicator", "Customize online indicator duration", true))
        hooks.add(HookConfig("Unlimited profiles", "Allow unlimited profiles", true))
        hooks.add(HookConfig("Unlimited albums", "Allow unlimited albums", true))
        hooks.add(HookConfig("Profile details", "Add extra fields and details to profiles", true))
        hooks.add(HookConfig("Profile views", "Don't let others know you viewed their profile", true))
        hooks.add(HookConfig("Signature Spoofer", "Spoof your device signature", true))
    }

    fun initialize(context: Context) {
        Log.i(TAG, "Initializing GrindrPlus config")
        configFile = File(context.filesDir, "grindrplus.json")
        if (!configFile.exists()) {
            try {
                configFile.createNewFile()
                val initialConfig = JSONObject().put("hooks", JSONObject())
                writeConfig(initialConfig)
            } catch (e: IOException) {
                Log.e(TAG, "Failed to create config file", e)
            }
        }
        config = readConfig(configFile)
        populateHooks() // Populate missing hooks.
    }

    private fun readConfig(file: File): JSONObject {
        return try {
            JSONObject(file.readText())
        } catch (e: Exception) {
            Log.e(TAG, "Error reading config file", e)
            JSONObject().put("hooks", JSONObject())
        }
    }

    private fun writeConfig(json: JSONObject) {
        try {
            configFile.writeText(json.toString(4))
        } catch (e: IOException) {
            Log.e(TAG, "Failed to write config file", e)
        }
    }

    private fun populateHooks() {
        val hooksObject = config.getJSONObject("hooks")

        hooks.forEach { hook ->
            if (!hooksObject.has(hook.name)) {
                val hookDetails = JSONObject().apply {
                    put("description", hook.description)
                    put("enabled", hook.enabled)
                }
                hooksObject.put(hook.name, hookDetails)
            }
        }
        writeConfig(config)
    }

    fun setHookState(name: String, enabled: Boolean) {
        try {
            val hookObject = config.getJSONObject("hooks").getJSONObject(name)
            hookObject.put("enabled", enabled)
            writeConfig(config)
            hooks.find { it.name == name }?.enabled = enabled
            Log.i(TAG, "Hook '$name' updated to enabled: $enabled")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update hook '$name'", e)
        }
    }

    fun addHook(name: String, description: String, enabled: Boolean): Boolean {
        try {
            val hookObject = JSONObject().apply {
                put("description", description)
                put("enabled", enabled)
            }

            config.getJSONObject("hooks").put(name, hookObject)
            writeConfig(config)
            hooks.add(HookConfig(name, description, enabled))
            Log.i(TAG, "Hook '$name' added successfully!")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add hook '$name'", e)
            return false
        }
    }

    fun isHookEnabled(name: String): Boolean {
        return config.getJSONObject("hooks").getJSONObject(name).getBoolean("enabled")
    }

    fun getConfig(): JSONObject {
        return config
    }
}
