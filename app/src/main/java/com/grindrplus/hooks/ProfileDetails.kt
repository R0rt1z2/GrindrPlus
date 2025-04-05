package com.grindrplus.hooks

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.widget.TextView
import android.widget.Toast
import com.grindrplus.GrindrPlus
import com.grindrplus.core.Utils
import com.grindrplus.core.Utils.calculateBMI
import com.grindrplus.core.Utils.h2n
import com.grindrplus.core.Utils.w2n
import com.grindrplus.ui.Utils.copyToClipboard
import com.grindrplus.ui.Utils.formatEpochSeconds
import com.grindrplus.utils.Hook
import com.grindrplus.utils.HookStage
import com.grindrplus.utils.hook
import de.robv.android.xposed.XposedHelpers.callMethod
import de.robv.android.xposed.XposedHelpers.getObjectField
import de.robv.android.xposed.XposedHelpers.setObjectField
import kotlin.math.roundToInt

class ProfileDetails : Hook(
    "Profile details",
    "Add extra fields and details to profiles"
) {
    private var boostedProfilesList = emptyList<String>()
    private val distanceUtils = "com.grindrapp.android.utils.DistanceUtils"
    private val profileBarView = "com.grindrapp.android.ui.profileV2.ProfileBarView"
    private val profileViewState = "com.grindrapp.android.ui.profileV2.model.ProfileViewState"
    private val serverDrivenCascadeCachedState =
        "com.grindrapp.android.persistence.model.serverdrivencascade.ServerDrivenCascadeCacheState"
    private val serverDrivenCascadeCachedProfile =
        "com.grindrapp.android.persistence.model.serverdrivencascade.ServerDrivenCascadeCachedProfile"

    @SuppressLint("DefaultLocale")
    override fun init() {
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
                val accountCreationTime = formatEpochSeconds(GrindrPlus.spline.invert(profileId.toDouble()).toLong())
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
                    if (weight.toString().contains("(")) return@hook
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
}