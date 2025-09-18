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
    private val appUpgradeManager = "Ma.s" // search for 'Uri.parse("market://details?id=com.grindrapp.android");'
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
                loge("Failed to fetch version info: ${response.message}")
            }
        } catch (e: Exception) {
            loge("Error fetching version info: ${e.message}")
            Logger.writeRaw(e.stackTraceToString())
        }
    }

    private fun compareVersions(v1: String, v2: String): Int {
        val parts1 = v1.split(".").map { it.toInt() }
        val parts2 = v2.split(".").map { it.toInt() }
        val maxLength = maxOf(parts1.size, parts2.size)

        for (i in 0 until maxLength) {
            val part1 = if (i < parts1.size) parts1[i] else 0
            val part2 = if (i < parts2.size) parts2[i] else 0
            if (part1 != part2) return part1.compareTo(part2)
        }
        return 0
    }

    private fun updateVersionInfo() {
        val currentVersion = GrindrPlus.context.packageManager.getPackageInfo(
            GrindrPlus.context.packageName,
            0
        ).versionName.toString()

        if (compareVersions(versionName, currentVersion) > 0) {
            findClass(appConfiguration).hookConstructor(HookStage.AFTER) { param ->
                setObjectField(param.thisObject(), "d", "$versionName.$versionCode")
            }

            findClass(GrindrPlus.userAgent).hookConstructor(HookStage.AFTER) { param ->
                param.thisObject().javaClass.declaredFields.forEach { field ->
                    field.isAccessible = true
                    val value = field.get(param.thisObject())
                    if (value is String && value.startsWith("grindr3/")) {
                        field.set(param.thisObject(), "grindr3/$versionName.$versionCode;$versionCode;")
                        return@forEach
                    }
                }
            }
        } else {
            logd("Current version is up-to-date: $versionName ($versionCode)")
        }
    }
}