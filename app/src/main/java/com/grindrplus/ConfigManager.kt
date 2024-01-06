import org.json.JSONObject
import java.io.File

class ConfigManager(private val configFilePath: String) {

    private fun getConfigFile(): File {
        val configFile = File(configFilePath)
        if (!configFile.exists()) {
            configFile.createNewFile()
            configFile.writeText("{}")
        }
        return configFile
    }

    private fun readConfig(): JSONObject {
        val configFile = getConfigFile()
        val jsonString = configFile.readText()
        return JSONObject(jsonString)
    }

    fun readString(key: String, defaultValue: String): String {
        val jsonObject = readConfig()
        return if (jsonObject.has(key)) jsonObject.getString(key) else defaultValue
    }

    fun readBoolean(key: String, defaultValue: Boolean): Boolean {
        val jsonObject = readConfig()
        return if (jsonObject.has(key)) jsonObject.getBoolean(key) else defaultValue
    }

    fun readInt(key: String, defaultValue: Int): Int {
        val jsonObject = readConfig()
        return if (jsonObject.has(key)) jsonObject.getInt(key) else defaultValue
    }

    fun writeConfig(key: String, value: Any) {
        try {
            val configFile = getConfigFile()
            val jsonObject = readConfig()
            jsonObject.put(key, value)
            configFile.writeText(jsonObject.toString())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
