package com.grindrplus.hooks

import com.grindrplus.GrindrPlus
import com.grindrplus.core.Config
import com.grindrplus.utils.Hook
import com.grindrplus.utils.HookStage
import com.grindrplus.utils.hook
import kotlin.time.Duration.Companion.minutes

class OnlineIndicator : Hook(
    "Online indicator",
    "Customize online indicator duration"
) {
    private val utils = "sd.f0" // search for ' <= 600000;'

    override fun init() {
        try {
            val utilsClass = findClass(utils)

            val targetMethod = utilsClass.methods.firstOrNull { method ->
                method.returnType == Boolean::class.java &&
                        method.parameterTypes.size == 1 &&
                        method.parameterTypes[0] == Long::class.java
            }

            if (targetMethod == null) {
                return GrindrPlus.logger.log("OnlineIndicator: Target method not found")
            }

            targetMethod.hook(HookStage.BEFORE) { param ->
                val savedDuration = Config.get("online_indicator", 3).toString().toInt()
                val durationMillis = savedDuration.minutes.inWholeMilliseconds
                param.setResult(System.currentTimeMillis() - param.arg<Long>(0) <= durationMillis)
            }
        } catch (e: Exception) {
            GrindrPlus.logger.log("OnlineIndicator: Unable to initialize hook: ${e.message}")
        }
    }
}