package com.grindrplus.hooks

import com.grindrplus.GrindrPlus
import com.grindrplus.utils.Hook
import com.grindrplus.utils.HookStage
import com.grindrplus.utils.hook
import com.grindrplus.utils.hookConstructor
import de.robv.android.xposed.XposedHelpers.setObjectField
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject


class DisableUpdates : Hook(
    "Disable updates",
    "Disable forced updates"
) {
    private val versionInfoEndpoint =
        "https://raw.githubusercontent.com/R0rt1z2/GrindrPlus/master/version.json"
    private val appUpdateInfo = "com.google.android.play.core.appupdate.AppUpdateInfo"
    private val appUpdateZzm = "com.google.android.play.core.appupdate.zzm"
    private val appUpgradeManager = "c5.j"
    private val appConfiguration = "com.grindrapp.android.base.config.AppConfiguration"
    private var versionCode: Int = 0
    private var versionName: String = ""

    override fun init() {
        findClass(appUpdateInfo)
            .hook("updateAvailability", HookStage.BEFORE) { param ->
                param.setResult(1)
            }

        findClass(appUpdateInfo)
            .hook("isUpdateTypeAllowed", HookStage.BEFORE) { param ->
                param.setResult(false)
            }

        findClass(appUpgradeManager) // showDeprecatedVersionDialog()
            .hook("e", HookStage.BEFORE) { param ->
                param.setResult(null)
            }

        findClass(appUpdateZzm) // requestUpdateInfo()
            .hook("zza", HookStage.BEFORE) { param ->
                param.setResult(null)
            }

        Thread {
            fetchLatestVersionInfo()
        }.start()
    }

    private fun fetchLatestVersionInfo() {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url(versionInfoEndpoint).build()

        try {
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val jsonData = response.body?.string()
                if (jsonData != null) {
                    val json = JSONObject(jsonData)
                    versionCode = json.getInt("versionCode")
                    versionName = json.getString("versionName")
                    GrindrPlus.logger.log("Fetched version info: $versionName ($versionCode)")
                    updateVersionInfo()
                }
            } else {
                GrindrPlus.logger.log("Error fetching version info: ${response.message}")
            }
        } catch (e: Exception) {
            GrindrPlus.logger.log("Error fetching version info: ${e.message}")
        }
    }

    private fun updateVersionInfo() {
        if (versionName < GrindrPlus.context.packageManager.getPackageInfo(
                GrindrPlus.context.packageName,
                0
            ).versionName
        ) {
            findClass(appConfiguration).hookConstructor(HookStage.AFTER) { param ->
                setObjectField(param.thisObject(), "b", versionName)
                setObjectField(param.thisObject(), "c", versionCode)
                setObjectField(param.thisObject(), "z", "$versionName.$versionCode")
            }
        }
    }
}