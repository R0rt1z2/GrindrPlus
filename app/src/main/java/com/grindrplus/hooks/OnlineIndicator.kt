package com.grindrplus.hooks

import com.grindrplus.core.Config
import com.grindrplus.utils.Hook
import com.grindrplus.utils.HookStage
import com.grindrplus.utils.hook
import kotlin.time.Duration.Companion.minutes

class OnlineIndicator : Hook(
    "Online indicator",
    "Customize online indicator duration"
) {
    val utils = "Ra.a0"

    override fun init() {
        findClass(utils) // shouldShowOnlineIndicator()
            .hook("a", HookStage.BEFORE) { param ->
                val savedDuration = Config.get("online_indicator", 3) as Int
                param.setResult(System.currentTimeMillis() - param.arg<Long>(0) <= savedDuration.minutes.inWholeMilliseconds)
            }
    }
}