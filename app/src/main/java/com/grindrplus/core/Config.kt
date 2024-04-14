package com.grindrplus.core

import org.json.JSONObject
import java.io.File

class Config(private val configFilePath: String) {

    private val defaultConfig = JSONObject().apply {
        put("profile_redesign", true)
        put("dont_record_views", true)
        put("teleport_enabled", false)
        put("show_profile_details", true)
    }

    private fun getConfigFile(): File {
        val configFile = File(configFilePath)
        if (!configFile.exists() || configFile.readText().isEmpty()) {
            configFile.createNewFile()
            configFile.writeText(defaultConfig.toString())
        }
        return configFile
    }

    private fun readConfig(): JSONObject {
        return JSONObject(getConfigFile().readText())
    }

    fun readString(key: String, defaultValue: String): String {
        return readConfig().optString(key, defaultValue)
    }

    fun readBoolean(key: String, defaultValue: Boolean): Boolean {
        return readConfig().optBoolean(key, defaultValue)
    }

    fun readInt(key: String, defaultValue: Int): Int {
        return readConfig().optInt(key, defaultValue)
    }

    fun writeConfig(key: String, value: Any) {
        try {
            val jsonObject = readConfig()
            jsonObject.put(key, value)
            getConfigFile().writeText(
                jsonObject.toString())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun readMap(key: String): JSONObject {
        return readConfig().optJSONObject(key) ?: JSONObject()
    }

    fun writeMap(key: String, map: JSONObject) {
        writeConfig(key, map)
    }

    fun updateMapEntry(mapKey: String, entryKey: String,
                       value: JSONObject) {
        val map = readMap(mapKey)
        map.put(entryKey, value)
        writeMap(mapKey, map)
    }

    fun removeMapEntry(mapKey: String, entryKey: String) {
        val map = readMap(mapKey)
        map.remove(entryKey)
        writeMap(mapKey, map)
    }

    fun addToMap(name: String, key: String, value: Any) {
        try {
            val configFile = getConfigFile()
            val jsonObject = readConfig()
            val targetMap = jsonObject
                .optJSONObject(name) ?: JSONObject()
            targetMap.put(key, value)
            jsonObject.put(name, targetMap)
            configFile.writeText(jsonObject.toString())
        }
        catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
