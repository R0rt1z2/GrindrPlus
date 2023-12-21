
package com.grindrplus

import com.grindrplus.Hooks.hookUpdateInfo
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.File
import java.io.IOException

import java.text.SimpleDateFormat
import java.util.*

object Utils {
    fun toReadableDate(timestamp: Long): String = SimpleDateFormat.getDateTimeInstance().format(Date(timestamp))

    /**
     * Calculates the BMI and returns a description of the BMI.
     *
     * @param weight in grams
     * @param height in cm
     * @return BMI in kg/m^2 and its description
     *
     * @see <a href="https://en.wikipedia.org/wiki/Body_mass_index">BMI - Wikipedia</a>
     * @see <a href="https://www.who.int/europe/news-room/fact-sheets/item/a-healthy-lifestyle---who-recommendations">WHO recommendations</a>
     */
    fun getBmiDescription(weight: Double, height: Double): String {
        val originalBmi = 10 * (weight / (height * height)) //Multiply by 10 to get kg/m^2
        val bmi = Math.round(originalBmi * 100.0) / 100.0 //Round to 2 decimal places
        return when {
            bmi < 18.5 -> "$bmi (Underweight)"
            bmi < 25.0 -> "$bmi (Normal weight)"
            bmi < 30.0 -> "$bmi (Overweight)"
            bmi < 35.0 -> "$bmi (Obesity I)"
            bmi < 40.0 -> "$bmi (Obesity II)"
            else -> "$bmi (Obesity III)"
        }
    }

    /**
     * Gets the fixed location.
     */
    fun getFixedLocationParam(param: XC_MethodHook.MethodHookParam, latOrLon: Boolean): Any {
        val regex = Regex("([0-9]+\\.[0-9]+),([0-9]+\\.[0-9]+)")
        val locationFile = File(Hooker.appContext.filesDir, "location.txt")
        if (!locationFile.exists()) {
            locationFile.createNewFile()
        }
        val content = locationFile.readText()
        return regex.find(content)?.groups?.get(if (latOrLon) 1 else 2)?.value?.toDouble()
            ?: XposedBridge.invokeOriginalMethod(param.method, param.thisObject, param.args)
    }

    /**
     * Fetches the latest version of Grindr from APKPure.
     * It then spoofs the app version, internally.
     */
    fun fetchVersionAndUpdate() {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url("https://apkpure.com/grindr-gay-chat-for-android/com.grindrapp.android/download")
            .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:121.0) Gecko/20100101 Firefox/121.0")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Logger.xLog("Fetch failed: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!response.isSuccessful)
                        return Logger.xLog("Received unexpected response code: ${response.code}")

                    val responseBody = response.body?.string() ?:
                    return Logger.xLog("Unable to get body from response!")
                    val versionName = """"versionName":"(.*?)",""".toRegex()
                        .find(responseBody)?.groups?.get(1)?.value
                    val versionCode = """"versionCode":(\d+),""".toRegex()
                        .find(responseBody)?.groups?.get(1)?.value

                    // If both are valid, spoof the app version to prevent the update dialog
                    // from showing up. This should be enough to disable forced updates.
                    if (versionName != null && versionCode != null) {
                        hookUpdateInfo(versionName, versionCode.toInt())
                    }
                }
            }
        })
    }
}