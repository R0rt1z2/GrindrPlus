package com.grindrplus.hooks

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.os.Build
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import android.widget.Toast
import com.grindrplus.GrindrPlus
import com.grindrplus.GrindrPlus.httpClient
import com.grindrplus.GrindrPlus.loadClass
import com.grindrplus.GrindrPlus.showToast
import com.grindrplus.core.Config
import com.grindrplus.core.Utils
import com.grindrplus.core.Utils.calculateBMI
import com.grindrplus.core.Utils.h2n
import com.grindrplus.core.Utils.w2n
import com.grindrplus.ui.Utils.copyToClipboard
import com.grindrplus.ui.Utils.formatEpochSeconds
import com.grindrplus.ui.Utils.getId
import com.grindrplus.utils.Hook
import com.grindrplus.utils.HookStage
import com.grindrplus.utils.PCHIP
import com.grindrplus.utils.hook
import de.robv.android.xposed.XposedHelpers.callMethod
import de.robv.android.xposed.XposedHelpers.getObjectField
import de.robv.android.xposed.XposedHelpers.setObjectField
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONArray
import java.io.IOException
import kotlin.math.roundToInt

class ProfileDetails : Hook(
    "Profile details",
    "Add extra fields and details to profiles"
) {
    private var boostedProfilesList = emptyList<String>()
    val distanceUtils = "com.grindrapp.android.utils.DistanceUtils"
    val profileBarView = "com.grindrapp.android.ui.profileV2.ProfileBarView"
    val profileViewState = "com.grindrapp.android.ui.profileV2.model.ProfileViewState"
    private val serverDrivenCascadeCachedState =
        "com.grindrapp.android.persistence.model.serverdrivencascade.ServerDrivenCascadeCacheState"
    private val serverDrivenCascadeCachedProfile =
        "com.grindrapp.android.persistence.model.serverdrivencascade.ServerDrivenCascadeCachedProfile"
    private var spline = PCHIP(
        listOf(
            1238563200L to 0,          // 2009-04-01
            1285027200L to 1000000,    // 2010-09-21
            1462924800L to 35512000,   // 2016-05-11
            1501804800L to 132076000,  // 2017-08-04
            1546547829L to 201948000,  // 2019-01-03
            1618531200L to 351220000,  // 2021-04-16
            1636150385L to 390338000,  // 2021-11-05
            1637963460L to 394800000,  // 2021-11-26
            1680393600L to 505225000,  // 2023-04-02
            1717200000L to 630495000,  // 2024-06-01
            1717372800L to 634942000,  // 2024-06-03
            1729950240L to 699724000,  // 2024-10-26
            1732986600L to 710609000,  // 2024-11-30
            1733349060L to 711676000,  // 2024-12-04
            1735229820L to 718934000,  // 2024-12-26
            1738065780L to 730248000   // 2025-01-29
        )
    )

    private val splineDataEndpoint =
        "https://raw.githubusercontent.com/R0rt1z2/GrindrPlus/refs/heads/master/spline.json"

    @SuppressLint("DefaultLocale")
    override fun init() {
        fetchRemoteData(splineDataEndpoint) { points ->
            spline = PCHIP(points)
            GrindrPlus.logger.log("Updated spline with remote data")
        }

        findClass(serverDrivenCascadeCachedState)
            .hook("getItems", HookStage.AFTER) { param ->
                (param.getResult() as List<*>).filter {
                    (it?.javaClass?.name ?: "") == serverDrivenCascadeCachedProfile
                }.forEach {
                    if (getObjectField(it, "isBoosting") as Boolean) {
                        boostedProfilesList += callMethod(it, "getProfileId") as String
                    }
                }
            }

        findClass(profileBarView)
            .hook("setProfile", HookStage.BEFORE) { param ->
                val profileId = getObjectField(param.arg(0), "profileId") as String
                val accountCreationTime = formatEpochSeconds(spline.invert(profileId.toDouble()).toLong())
                val distance = callMethod(param.arg(0), "getDistance") ?: "Unknown (hidden)"
                setObjectField(param.arg(0), "distance", distance)

                if (profileId in boostedProfilesList) {
                    val lastSeen = callMethod(param.arg(0), "getLastSeenText")
                    setObjectField(param.arg(0), "lastSeenText", "$lastSeen (Boosting)")
                }

                val displayName = callMethod(param.arg(0), "getDisplayName") ?: profileId
                setObjectField(param.arg(0), "displayName", displayName)

                val viewBinding = getObjectField(param.thisObject(), "d")
                val displayNameTextView = getObjectField(viewBinding, "d") as TextView

                displayNameTextView.setOnLongClickListener {
                    GrindrPlus.showToast(Toast.LENGTH_LONG, "Profile ID: $profileId")
                    copyToClipboard("Profile ID", profileId)
                    true
                }

                displayNameTextView.setOnClickListener {
                    val properties = mapOf(
                        "Estimated creation" to accountCreationTime,
                        "Profile ID" to profileId,
                        "Approximate distance" to Utils.safeGetField(
                            param.arg(0),
                            "approximateDistance"
                        ) as? Boolean,
                        "Found via teleport" to Utils.safeGetField(
                            param.arg(0),
                            "foundViaTeleport"
                        ) as? Boolean,
                        "Favorite" to Utils.safeGetField(param.arg(0), "isFavorite") as? Boolean,
                        "From viewed me" to Utils.safeGetField(param.arg(0), "isFromViewedMe") as? Boolean,
                        "Inaccessible profile" to Utils.safeGetField(
                            param.arg(0),
                            "isInaccessibleProfile"
                        ) as? Boolean,
                        "JWT boosting" to Utils.safeGetField(param.arg(0), "isJwtBoosting") as? Boolean,
                        "New" to Utils.safeGetField(param.arg(0), "isNew") as? Boolean,
                        "Teleporting" to Utils.safeGetField(param.arg(0), "isTeleporting") as? Boolean,
                        "Online now" to Utils.safeGetField(param.arg(0), "onlineNow") as? Boolean
                    ).filterValues { it != null }

                    val dialog = AlertDialog.Builder(it.context)
                        .setTitle("Hidden profile details")
                        .setMessage(properties.map { (key, value) -> "• $key: $value" }
                            .joinToString("\n"))
                        .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
                        .create()

                    dialog.show()
                }
            }

        findClass(distanceUtils)
            .hook("c", HookStage.AFTER) { param ->
                val distance = param.arg<Double>(1)
                val isFeet = param.arg<Boolean>(2)

                param.setResult(if (isFeet) {
                    val feet = (distance * 3.280839895).roundToInt()
                    if (feet < 5280) {
                        String.format("%d feet", feet)
                    } else {
                        String.format("%d miles %d feet", feet / 5280, feet % 5280)
                    }
                } else {
                    val meters = distance.roundToInt()
                    if (meters < 1000) {
                        String.format("%d meters", meters)
                    } else {
                        String.format("%d km %d m", meters / 1000, meters % 1000)
                    }
                })
            }

        findClass(profileViewState)
            .hook("getWeight", HookStage.AFTER) { param ->
                val weight = param.getResult()
                val height = callMethod(param.thisObject(), "getHeight")

                if (weight != null && height != null) {
                    val BMI = calculateBMI(
                        "kg" in weight.toString(),
                        w2n("kg" in weight.toString(), weight.toString()),
                        h2n("kg" in weight.toString(), height.toString())
                    )
                    param.setResult("$weight - ${String.format("%.1f", BMI)} (${
                        mapOf(
                            "Underweight" to 18.5,
                            "Normal weight" to 24.9,
                            "Overweight" to 29.9,
                            "Obese" to Double.MAX_VALUE
                        ).entries.first { it.value > BMI }.key
                    })")
                }
            }

    }

    private fun fetchRemoteData(url: String, callback: (List<Pair<Long, Int>>) -> Unit) {
        val client = OkHttpClient()
        val request = Request.Builder().url(url).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                GrindrPlus.logger.apply {
                    log("Failed to fetch remote data: ${e.message}")
                    writeRaw(e.stackTraceToString())
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.body?.string()?.let { jsonString ->
                    try {
                        val jsonArray = JSONArray(jsonString)
                        val parsedPoints = mutableListOf<Pair<Long, Int>>()

                        for (i in 0 until jsonArray.length()) {
                            val obj = jsonArray.getJSONObject(i)
                            val time = obj.getLong("time")
                            val id = obj.getInt("id")
                            parsedPoints.add(time to id)
                        }

                        callback(parsedPoints)
                    } catch (e: Exception) {
                        GrindrPlus.logger.apply {
                            log("Failed to parse remote data: ${e.message}")
                            writeRaw(e.stackTraceToString())
                        }
                    }
                }
            }
        })
    }
}