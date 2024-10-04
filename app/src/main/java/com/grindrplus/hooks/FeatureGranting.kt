package com.grindrplus.hooks

import com.grindrplus.utils.Feature
import com.grindrplus.utils.FeatureManager
import com.grindrplus.utils.Hook
import com.grindrplus.utils.HookStage
import com.grindrplus.utils.hook
import com.grindrplus.utils.hookConstructor
import de.robv.android.xposed.XposedHelpers.getObjectField

class FeatureGranting : Hook(
    "Feature granting",
    "Grant all Grindr features"
) {
    private val featureFlags = "T4.h"
    private val upsellsV8Model = "com.grindrapp.android.model.UpsellsV8"
    private val insertsModel = "com.grindrapp.android.model.Inserts"
    private val settingDistanceVisibilityViewModel =
        "com.grindrapp.android.ui.settings.distance.a\$e"
    private val featureManager = FeatureManager()

    override fun init() {
        initFeatures()

        val featureFlagsClass = findClass(featureFlags)

        featureFlagsClass.hook(
            "isEnabled", HookStage.BEFORE
        ) { param ->
            val featureFlagName = getObjectField(
                param.thisObject(),
                "featureFlagName"
            ) as String
            if (featureManager.isManaged(featureFlagName)) {
                param.setResult(featureManager.isEnabled(featureFlagName))
            }
        }

        featureFlagsClass.hook(
            "isDisabled", HookStage.BEFORE
        ) { param ->
            val featureFlagName = getObjectField(
                param.thisObject(),
                "featureFlagName"
            ) as String
            if (featureManager.isManaged(featureFlagName)) {
                param.setResult(!featureManager.isEnabled(featureFlagName))
            }
        }

        findClass(settingDistanceVisibilityViewModel)
            .hookConstructor(HookStage.BEFORE) { param ->
                param.setArg(4, false) // hidePreciseDistance
            }

        listOf(upsellsV8Model, insertsModel).forEach { model ->
            findClass(model)
                .hook("getMpuFree", HookStage.BEFORE) { param ->
                    param.setResult(Int.MAX_VALUE)
                }

            findClass(model)
                .hook("getMpuXtra", HookStage.BEFORE) { param ->
                    param.setResult(0)
                }
        }
    }

    private fun initFeatures() {
        featureManager.add(Feature("ad-backfill", false))
        featureManager.add(Feature("profile-redesign-20230214", true))
        featureManager.add(Feature("notification-action-chat-20230206", true))
        featureManager.add(Feature("gender-updates", true))
        featureManager.add(Feature("gender-filter", true))
        featureManager.add(Feature("gender-exclusion", true))
        featureManager.add(Feature("calendar-ui", true))
        featureManager.add(Feature("vaccine-profile-field", true))
        featureManager.add(Feature("tag-search", true))
        featureManager.add(Feature("approximate-distance", true))
        featureManager.add(Feature("spectrum_solicitation_sex", true))
        featureManager.add(Feature("allow-mock-location", true))
        featureManager.add(Feature("spectrum-solicitation-of-drugs", true))
        featureManager.add(Feature("side-profile-link", true))
        featureManager.add(Feature("sk-privacy-policy-20230130", false))
        featureManager.add(Feature("intro-offer-free-trial-20221222", true))
        featureManager.add(Feature("canceled-screen", true))
        featureManager.add(Feature("takemehome-button", true))
        featureManager.add(Feature("download-my-data", true))
        featureManager.add(Feature("face-auth-android", true))
    }
}