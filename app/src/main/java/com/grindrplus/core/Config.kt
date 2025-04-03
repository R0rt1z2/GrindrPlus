package com.grindrplus.core

import android.content.Context
import com.grindrplus.GrindrPlus
import com.grindrplus.manager.utils.AppCloneUtils
import org.json.JSONObject
import timber.log.Timber
import java.io.File
import java.io.IOException

object Config {
    private lateinit var configFile: File
    private var localConfig = JSONObject()

    private var currentPackageName = Constants.GRINDR_PACKAGE_NAME

    fun initialize(context: Context?, packageName: String? = null) {
        println("Called initialize for package: $packageName")
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

        if (packageName != null) {
            currentPackageName = packageName
        }

        migrateToMultiCloneFormat()
    }


    /**
     * Migrate existing config to multi-clone format if needed
     */
    private fun migrateToMultiCloneFormat() {
        if (!localConfig.has("clones")) {
            val cloneSettings = JSONObject()

            if (localConfig.has("hooks")) {
                val defaultPackageConfig = JSONObject()
                defaultPackageConfig.put("hooks", localConfig.get("hooks"))
                cloneSettings.put(Constants.GRINDR_PACKAGE_NAME, defaultPackageConfig)

                val keysToMove = mutableListOf<String>()
                val keys = localConfig.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    if (key != "hooks" && key != "analytics" && key != "discreet_icon" && key != "material_you") {
                        defaultPackageConfig.put(key, localConfig.get(key))
                        keysToMove.add(key)
                    }
                }
                keysToMove.forEach { localConfig.remove(it) }
            } else {
                cloneSettings.put(Constants.GRINDR_PACKAGE_NAME, JSONObject().put("hooks", JSONObject()))
            }

            localConfig.put("clones", cloneSettings)
            writeRemoteConfig(localConfig)
        }

        ensurePackageExists(currentPackageName)
    }

    /**
     * Set current package name for settings
     */
    fun setCurrentPackage(packageName: String) {
        currentPackageName = packageName
        ensurePackageExists(packageName)
    }

    /**
     * Get current package name
     */
    fun getCurrentPackage(): String {
        return currentPackageName
    }

    /**
     * Ensure the package exists in config
     */
    private fun ensurePackageExists(packageName: String) {
        val clones = localConfig.optJSONObject("clones") ?: JSONObject().also {
            localConfig.put("clones", it)
        }

        if (!clones.has(packageName)) {
            clones.put(packageName, JSONObject().put("hooks", JSONObject()))
            writeRemoteConfig(localConfig)
        }
    }

    /**
     * Get all available packages with settings
     */
    fun getAvailablePackages(context: Context): List<String> {
        val installedClones = listOf(Constants.GRINDR_PACKAGE_NAME) + AppCloneUtils.getExistingClones(context)
        val clones = localConfig.optJSONObject("clones") ?: return listOf(Constants.GRINDR_PACKAGE_NAME)

        return installedClones.filter { pkg ->
            clones.has(pkg)
        }
    }

    fun readRemoteConfig(): JSONObject {
        return try {
            val value = GrindrPlus.bridgeClient.getConfig()
            println("Called readRemoteConfig, isNull: ${value == null}")
            value ?: JSONObject().put("clones", JSONObject().put(Constants.GRINDR_PACKAGE_NAME, JSONObject().put("hooks", JSONObject())))
        } catch (e: Exception) {
            Timber.tag("GrindrPlus").e(e, "Error reading config file")
            JSONObject().put("clones", JSONObject().put(Constants.GRINDR_PACKAGE_NAME, JSONObject().put("hooks", JSONObject())))
        }
    }

    fun writeRemoteConfig(json: JSONObject) {
        try {
            println("Called writeRemoteConfig")
            GrindrPlus.bridgeClient.setConfig(json)
        } catch (e: IOException) {
            Timber.tag("GrindrPlus").e(e, "Failed to write config file")
        }
    }

    private fun getCurrentPackageConfig(): JSONObject {
        val clones = localConfig.optJSONObject("clones")
            ?: JSONObject().also { localConfig.put("clones", it) }

        return clones.optJSONObject(currentPackageName)
            ?: JSONObject().also { clones.put(currentPackageName, it) }
    }

    fun put(name: String, value: Any) {
        if (name in listOf("analytics", "discreet_icon", "material_you")) {
            localConfig.put(name, value)
        } else {
            val packageConfig = getCurrentPackageConfig()
            packageConfig.put(name, value)
        }

        writeRemoteConfig(localConfig)
    }

    fun get(name: String, default: Any): Any {
        if (name in listOf("analytics", "discreet_icon", "material_you")) {
            val get = localConfig.opt(name)
            return get ?: default.also { put(name, default) }
        }

        val packageConfig = getCurrentPackageConfig()
        val get = packageConfig.opt(name)
        return get ?: default.also { put(name, default) }
    }

    fun setHookEnabled(hookName: String, enabled: Boolean) {
        val packageConfig = getCurrentPackageConfig()
        val hooks = packageConfig.optJSONObject("hooks")
            ?: JSONObject().also { packageConfig.put("hooks", it) }

        hooks.optJSONObject(hookName)?.put("enabled", enabled)
        writeRemoteConfig(localConfig)
    }

    fun isHookEnabled(hookName: String): Boolean {
        val packageConfig = getCurrentPackageConfig()
        val hooks = packageConfig.optJSONObject("hooks") ?: return false
        return hooks.optJSONObject(hookName)?.getBoolean("enabled") == true
    }

    suspend fun initHookSettings(name: String, description: String, state: Boolean) {
        val packageConfig = getCurrentPackageConfig()
        val hooks = packageConfig.optJSONObject("hooks")
            ?: JSONObject().also { packageConfig.put("hooks", it) }

        if (hooks.optJSONObject(name) == null) {
            hooks.put(name, JSONObject().apply {
                put("description", description)
                put("enabled", state)
            })

            writeRemoteConfig(localConfig)
        }
    }

    fun getHooksSettings(): Map<String, Pair<String, Boolean>> {
        val packageConfig = getCurrentPackageConfig()
        val hooks = packageConfig.optJSONObject("hooks") ?: return emptyMap()
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