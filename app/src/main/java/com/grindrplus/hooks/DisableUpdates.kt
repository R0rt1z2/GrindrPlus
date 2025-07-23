package com.grindrplus.hooks

import com.grindrplus.GrindrPlus
import com.grindrplus.core.Logger
import com.grindrplus.core.logd
import com.grindrplus.core.loge
import com.grindrplus.utils.Hook
import com.grindrplus.utils.HookStage
import com.grindrplus.utils.hook
import com.grindrplus.utils.hookConstructor
import de.robv.android.xposed.XposedHelpers.setObjectField
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
    private val appUpdateZzm = "com.google.android.play.core.appupdate.zzm" // search for 'requestUpdateInfo(%s)'
    private val appUpgradeManager = "z9.n" // search for 'Uri.parse("market://details?id=com.grindrapp.android");'
    private val appConfiguration = "com.grindrapp.android.platform.config.AppConfiguration"
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
            .hook("a", HookStage.BEFORE) { param ->
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
                    logd("Successfully fetched version info: $versionName ($versionCode)")
                    updateVersionInfo()
                }
            } else {
                Logger.e("Failed to fetch version info: ${response.message}")
            }
        } catch (e: Exception) {
            loge("Error fetching version info: ${e.message}")
            Logger.writeRaw(e.stackTraceToString())
        }
    }

    private fun updateVersionInfo() {
        if (versionName < GrindrPlus.context.packageManager.getPackageInfo(
                GrindrPlus.context.packageName,
                0
            ).versionName.toString()
        ) {
            findClass(appConfiguration).hookConstructor(HookStage.AFTER) { param ->
                setObjectField(param.thisObject(), "b", versionName)
                setObjectField(param.thisObject(), "c", versionCode)
                setObjectField(param.thisObject(), "z", "$versionName.$versionCode")
            }
        }
    }
}