package com.grindrplus.hooks

import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.grindrplus.GrindrPlus
import com.grindrplus.core.Config
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


// supported version: 25.20.0
class DisableUpdates : Hook(
    "Disable updates",
    "Disable forced updates"
) {
    private val versionInfoEndpoint =
        "https://raw.githubusercontent.com/R0rt1z2/GrindrPlus/master/version.json"
    private val appUpdateInfo = "com.google.android.play.core.appupdate.AppUpdateInfo"
    private val appUpdateZzm = "com.google.android.play.core.appupdate.zzm" // search for 'requestUpdateInfo(%s)'
	private val appUpgradeManager = "jf.n" // search for 'Uri.parse("market://details?id=com.grindrapp.android");'
    private val appConfiguration = "com.grindrapp.android.platform.config.AppConfiguration"

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
			// search for '.setMessage(R.string.deprecation_message);'
            .hook("b", HookStage.BEFORE) { param ->
                param.setResult(null)
            }

        findClass(appUpdateZzm) // requestUpdateInfo()
            .hook("zza", HookStage.BEFORE) { param ->
                param.setResult(null)
            }

		updateVersionInfo()

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
                    val versionCode = json.getInt("versionCode")
                    val versionName = json.getString("versionName")
                    logd("Successfully fetched version info: $versionName ($versionCode)")

					versionFetchCallback(versionName, versionCode)
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

	private fun versionFetchCallback(versionName: String, versionCode: Int) {
		val (versionNamePrevious, _) = getVersionInfoPatched()

		Config.put("disable_updates_version_name", versionName)
		Config.put("disable_updates_version_code", versionCode)

		if (versionName != versionNamePrevious)
			restartApp()
	}

	private fun restartApp() {
		val context = GrindrPlus.context
		Handler(Looper.getMainLooper()).post {
			logd("Restarting the app")
			Toast.makeText(context, "Restarting the app to disable forced updates", Toast.LENGTH_SHORT).show()

			val intent: Intent? = context.packageManager.getLaunchIntentForPackage(context.packageName)
			if (intent == null) {
				loge("unable to get app launch intent")
				return@post
			}

			val mainIntent = Intent.makeRestartActivityTask(intent.component)
			mainIntent.setPackage(context.packageName)
			context.startActivity(mainIntent)
			Runtime.getRuntime().exit(0)
		}
	}

	private fun getVersionInfo(): Pair<String, Int> {
		val packageInfo = GrindrPlus.context.packageManager.getPackageInfo(
			GrindrPlus.context.packageName,
			0
		)
		val currentVersionName = packageInfo.versionName.toString()
		val currentVersionCode = packageInfo.longVersionCode.toInt()

		return Pair(currentVersionName, currentVersionCode)
	}

	private fun getVersionInfoPatched(): Pair<String, Int> {
		val (currentVersionName, currentVersionCode) = getVersionInfo()

		val versionName = Config.get("disable_updates_version_name", currentVersionName).toString()
		val versionCode = Config.get("disable_updates_version_code", currentVersionCode).toString().toInt()

		return Pair(versionName, versionCode)
	}

    private fun updateVersionInfo() {
		val (currentVersionName, currentVersionCode) = getVersionInfo()
		val (versionName, versionCode) = getVersionInfoPatched()

		if (compareVersions(versionName, currentVersionName) > 0) {
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