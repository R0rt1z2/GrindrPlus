package com.grindrplus.hooks

import com.grindrplus.core.Config
import com.grindrplus.utils.Hook
import com.grindrplus.utils.HookStage
import com.grindrplus.utils.hook
import com.grindrplus.utils.hookConstructor
import de.robv.android.xposed.XposedHelpers.setObjectField
import kotlin.time.Duration.Companion.minutes

class OnlineIndicator : Hook(
    "Online indicator",
    "Customize online indicator duration"
) {
    // Fallback: force cached profiles to appear online by pushing onlineUntil far into the future.
    // The older util class (di.q0) is now obfuscated; this keeps the indicator working.
    private val cascadeProfile = "com.grindrapp.android.persistence.model.serverdrivencascade.ServerDrivenCascadeCachedProfile"

    override fun init() {
        findClass(cascadeProfile).hookConstructor(HookStage.AFTER) { param ->
            // Keep the profile online for a very long time (configurable minutes)
            val savedDuration = Config.get("online_indicator", 3).toString().toInt()
            val forcedUntil = System.currentTimeMillis() + savedDuration.minutes.inWholeMilliseconds
            setObjectField(param.thisObject(), "onlineUntil", forcedUntil)
        }
    }
}
