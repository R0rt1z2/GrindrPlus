package com.grindrplus.hooks

import com.grindrplus.utils.Hook
import com.grindrplus.utils.HookStage
import com.grindrplus.utils.hook

class EnableUnlimited : Hook(
    "Enable unlimited",
    "Enable Grindr Unlimited features"
) {
    private val userSession = "com.grindrapp.android.storage.b"

    override fun init() {
        val userSessionClass = findClass(userSession)

        userSessionClass?.hook( // hasFeature()
            "k", HookStage.BEFORE
        ) { param ->
            val disallowedFeatures = setOf("DisableScreenshot")
            param.result = param.arg(0) !in disallowedFeatures
        }

        userSessionClass?.hook( // isNoXtraUpsell()
            "i", HookStage.BEFORE
        ) { param ->
            param.result = true
        }

        userSessionClass?.hook( // isNoPlusUpsell()
            "D", HookStage.BEFORE
        ) { param ->
            param.result = true
        }

        userSessionClass?.hook( // isFree()
            "u", HookStage.BEFORE
        ) { param ->
            param.result = false
        }

        userSessionClass?.hook( // isXtra()
            "s", HookStage.BEFORE
        ) { param ->
            param.result = false
        }

        userSessionClass?.hook( // isPlus()
            "B", HookStage.BEFORE
        ) { param ->
            param.result = false
        }

        userSessionClass?.hook( // isUnlimited()
            "A", HookStage.BEFORE
        ) { param ->
            param.result = true
        }
    }
}