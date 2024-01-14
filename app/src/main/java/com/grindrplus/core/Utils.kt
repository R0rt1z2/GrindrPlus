
package com.grindrplus.core

import android.content.Intent
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.grindrplus.Hooker
import com.grindrplus.Hooker.Companion.config
import com.grindrplus.Hooker.Companion.sharedPref
import com.grindrplus.core.Hooks.chatMessageManager
import com.grindrplus.core.Hooks.hookUpdateInfo
import com.grindrplus.core.Hooks.ownProfileId
import com.grindrplus.decorated.persistence.model.ChatMessage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers.callMethod
import de.robv.android.xposed.XposedHelpers.findClass
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.URLEncoder

import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern

object Utils {
    fun toReadableDate(timestamp: Long): String = SimpleDateFormat.getDateTimeInstance().format(Date(timestamp))

    /**
     * Calculates the BMI and returns a description of the BMI.
     *
     * @param weight in kilograms
     * @param height in cm
     * @return BMI in kg/m^2 and its description
     *
     * @see <a href="https://en.wikipedia.org/wiki/Body_mass_index">BMI - Wikipedia</a>
     * @see <a href="https://www.who.int/europe/news-room/fact-sheets/item/a-healthy-lifestyle---who-recommendations">WHO recommendations</a>
     */
    fun getBMIDescription(height: String, weight: String): String {
        val heightInMeters = convertHeightToMeters(height)
        val weightInKg = convertWeightToKg(weight)

        return if (heightInMeters != null && weightInKg != null) {
            val bmi = weightInKg / (heightInMeters * heightInMeters)
            val roundedBmi = Math.round(bmi * 100.0) / 100.0
            when {
                roundedBmi < 18.5 -> "$roundedBmi (Underweight)"
                roundedBmi < 25.0 -> "$roundedBmi (Normal weight)"
                roundedBmi < 30.0 -> "$roundedBmi (Overweight)"
                roundedBmi < 35.0 -> "$roundedBmi (Obesity I)"
                roundedBmi < 40.0 -> "$roundedBmi (Obesity II)"
                else -> "$roundedBmi (Obesity III)"
            }
        } else {
            "BMI not available"
        }
    }

    /**
     * Converts a height string to meters.
     */
    fun convertHeightToMeters(height: String): Double? {
        val feetAndInchesRegex = """(\d+)'(\d+)"""".toRegex()
        val cmRegex = """(\d+) cm""".toRegex()

        return when {
            feetAndInchesRegex.matches(height) -> {
                val (feet, inches) = feetAndInchesRegex.find(height)!!.destructured
                ((feet.toInt() * 12 + inches.toInt()) * 2.54) / 100
            }
            cmRegex.matches(height) -> {
                val (cm) = cmRegex.find(height)!!.destructured
                cm.toInt() / 100.0
            }
            else -> null
        }
    }

    /**
     * Converts a weight string to kilograms.
     */
    fun convertWeightToKg(weight: String): Double? {
        val lbsRegex = """(\d+) lbs""".toRegex()
        val kgRegex = """(\d+) kg""".toRegex()

        return when {
            lbsRegex.matches(weight) -> {
                val (lbs) = lbsRegex.find(weight)!!.destructured
                lbs.toInt() * 0.453592
            }
            kgRegex.matches(weight) -> {
                val (kg) = kgRegex.find(weight)!!.destructured
                kg.toDouble()
            }
            else -> null
        }
    }

    /**
     * Returns the height and weight TextViews from a View.
     */
    fun findHeightAndWeightTextViews(view: View): Pair<TextView?, TextView?> {
        var heightTextView: TextView? = null
        var weightTextView: TextView? = null

        val heightPattern = Pattern.compile("(\\d+'\\d+\")|(\\d+ cm)")
        val weightPattern = Pattern.compile("(\\d+ lbs)|(\\d+ kg)")

        if (view is TextView) {
            if (heightPattern.matcher(view.text).matches()) {
                heightTextView = view
            } else if (weightPattern.matcher(view.text).matches()) {
                weightTextView = view
            }
        } else if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                val (foundHeightTextView, foundWeightTextView) = findHeightAndWeightTextViews(view.getChildAt(i))
                if (foundHeightTextView != null) heightTextView = foundHeightTextView
                if (foundWeightTextView != null) weightTextView = foundWeightTextView
            }
        }

        return Pair(heightTextView, weightTextView)
    }

    fun fetchVersionAndUpdate() {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url("https://raw.githubusercontent.com/R0rt1z2/GrindrPlus/master/version.json")
            .addHeader("User-Agent", "GrindrPlus")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Logger.xLog("Fetch failed: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!response.isSuccessful)
                        return Logger.xLog("Received unexpected response code: ${response.code}")

                    // Convert the response body to a JSON object
                    val jsonObject = JSONObject(response.body?.string() ?: return)
                    val versionName = jsonObject.getString("versionName")
                    val versionCode = jsonObject.getInt("versionCode")

                    // If both are valid, spoof the app version to prevent the update dialog
                    // from showing up. This should be enough to disable forced updates.
                    if (versionName != null && versionCode != null) {
                        hookUpdateInfo(versionName, versionCode)
                    }
                }
            }
        })
    }

    /**
     * Maps the feature flag to the corresponding value.
     */
    fun mapFeatureFlag(
        feature: String,
        param: XC_MethodHook.MethodHookParam
    ): Boolean = when (feature) {
        "profile-redesign-20230214" -> config.readBoolean("profile_redesign", true)
        "notification-action-chat-20230206" -> true
        "gender-updates" -> true
        "gender-filter" -> true
        "gender-exclusion" -> true
        "calendar-ui" -> true
        "vaccine-profile-field" -> true
        "tag-search" -> true
        "approximate-distance" -> true
        "spectrum_solicitation_sex" -> true
        "allow-mock-location" -> true
        "spectrum-solicitation-of-drugs" -> true
        "side-profile-link" -> true
        "canceled-screen" -> true
        "takemehome-button" -> true
        "download-my-data" -> true
        "face-auth-android" -> true
        else ->
            XposedBridge.invokeOriginalMethod(
                param.method,
                param.thisObject,
                param.args
            ) as Boolean
    }

    /**
     * Open a profile by its ID.
     * Based on yukkerike's work.
     *
     * @param id The profile ID.
     */
    fun openProfile(id: String) {
        val generalDeepLinksClass = findClass("com.grindrapp.android.deeplink.GeneralDeepLinks", Hooker.pkgParam.classLoader)
        val profilesActivityClass = findClass("com.grindrapp.android.ui.profileV2.ProfilesActivity", Hooker.pkgParam.classLoader)
        val profilesActivityInstance = profilesActivityClass.getField("u0").get(null)
        val referrerTypeClass = findClass("com.grindrapp.android.base.model.profile.ReferrerType", Hooker.pkgParam.classLoader)
        val referrerType = referrerTypeClass.getField("NOTIFICATION").get(null)
        val profilesActivityInnerClass = findClass("com.grindrapp.android.ui.profileV2.ProfilesActivity\$a", Hooker.pkgParam.classLoader)

        var intent: Intent? = null
        for (method in profilesActivityInnerClass.declaredMethods) {
            if (method.parameterTypes.size == 3 && method.parameterTypes[2] == referrerTypeClass) {
                intent = method.invoke(
                    profilesActivityInstance,
                    Hooker.appContext, id, referrerType
                ) as Intent
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                break
            }
        }

        if (intent != null) {
            for (method in generalDeepLinksClass.declaredMethods) {
                if (method.name == "safedk_Context_startActivity_97cb3195734cf5c9cc3418feeafa6dd6") {
                    method.invoke(null, Hooker.appContext, intent)
                    return
                }
            }
        }
    }

    /**
     * Logs a chat message.
     *
     * @param text The message text.
     * @param from The profile ID of the sender.
     * @param sender The profile ID of the sender. If null, the own profile ID is used.
     */
    fun logChatMessage(text: String, from: String, sender: String? = null) {
        val chatMessage = ChatMessage()
        chatMessage.messageId = UUID.randomUUID().toString()
        chatMessage.sender = sender ?: ownProfileId ?: return
        chatMessage.recipient = from
        chatMessage.stanzaId = from
        chatMessage.conversationId = from
        chatMessage.timestamp = System.currentTimeMillis()
        chatMessage.type = "text"
        chatMessage.body = text

        callMethod(
            chatMessageManager,
            Obfuscation.GApp.xmpp.ChatMessageManager_.handleIncomingChatMessage,
            chatMessage.instance,
            false,
            false
        )
    }

    /**
     * Returns the boolean preference value.
     */
    fun getBooleanPreference(key: String, defaultValue: Boolean = false): Boolean {
        return sharedPref.getString(key, defaultValue.toString())?.toBoolean() ?: defaultValue
    }

    /**
     * Sets the boolean preference value.
     */
    fun setBooleanPreference(key: String, value: Boolean) {
        sharedPref.edit().putString(key, value.toString()).apply()
    }

    /**
     * Sets the mocked location in the preferences.
     */
    fun setLocationPreference(key: String, latitude: Double, longitude: Double) {
        val locationString = "$latitude,$longitude"
        sharedPref.edit().putString(key, locationString).apply()
    }

    /**
     * Returns the mocked location from the preferences.
     */
    fun getLocationPreference(key: String): Pair<Double, Double>? {
        val locationString = sharedPref.getString(key, null)
        return locationString?.split(',')?.let {
            if (it.size == 2) {
                val latitude = it[0].toDoubleOrNull()
                val longitude = it[1].toDoubleOrNull()
                if (latitude != null && longitude != null) {
                    Pair(latitude, longitude)
                } else null
            } else null
        }
    }

    /**
     * Get latitude and longitude from name.
     */
    fun getLatLngFromLocationName(locationName: String): Pair<Double, Double>? {
        try {
            val client = OkHttpClient()
            val encodedLocation = URLEncoder.encode(locationName, "UTF-8")
            val url = "https://nominatim.openstreetmap.org/search?format=json&q=$encodedLocation"

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "GrindrPlus")
                .build()

            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string()

                responseBody?.let {
                    val jsonArray = JSONArray(it)
                    if (jsonArray.length() > 0) {
                        val jsonObject = jsonArray.getJSONObject(0)
                        val latitude = jsonObject.getDouble("lat")
                        val longitude = jsonObject.getDouble("lon")
                        return Pair(latitude, longitude)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    /**
     * Toggle specific setting or feature on/off.
     */
    public fun toggleSetting(setting: String, description: String): String {
        val newState = !config.readBoolean(setting, true)
        config.writeConfig(setting, newState)
        return "$description ${if (newState) "enabled" else "disabled"}."
    }
}