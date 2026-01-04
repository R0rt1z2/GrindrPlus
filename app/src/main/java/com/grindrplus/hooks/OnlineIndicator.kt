package com.grindrplus.hooks

import com.grindrplus.core.Config
import com.grindrplus.utils.Hook
import com.grindrplus.utils.HookStage
import com.grindrplus.utils.hook
import kotlin.time.Duration.Companion.minutes

// supported version: 25.20.0
class OnlineIndicator : Hook(
    "Online indicator",
    "Customize online indicator duration"
) {
    val utils = "Vm.m0" // search for ' <= 600000;'
    val isFeatureFlagEnabled = "ih.e" // search for 'implements IsFeatureFlagEnabled'

    override fun init() {
        val savedDurationMinutes = Config.get("online_indicator", 3).toString().toInt()
        val savedDurationMillis = savedDurationMinutes.minutes.inWholeMilliseconds

        findClass(utils)// shouldShowOnlineIndicator()
            .hook("a", HookStage.BEFORE) { param ->
                val lastSeen = param.arg<Long>(0)
                param.setResult(System.currentTimeMillis() - lastSeen <= savedDurationMillis)
            }

        findClass(isFeatureFlagEnabled)
            .hook("a", HookStage.BEFORE) { param ->
                val a = param.args()[0]
                val flagKey = a!!.javaClass.getMethod("getKey").invoke(a)

                if (flagKey == "online-until-updates")
                    param.setResult(false)
            }

        findClass("com.grindrapp.android.utils.ProfileUtilsV2") // search for 'R.string.profile_time_online_minutes_ago'
            .hook("b", HookStage.BEFORE) { param ->
                val onlineUntilThreshold = param.arg<Long>(1)
                if (onlineUntilThreshold == 600000L)
                    param.setArg(1, savedDurationMillis)
            }

    }
}