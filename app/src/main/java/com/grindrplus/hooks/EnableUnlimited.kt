package com.grindrplus.hooks

import com.grindrplus.utils.Hook
import com.grindrplus.utils.HookStage
import com.grindrplus.utils.hook

class EnableUnlimited : Hook(
    "Enable unlimited",
    "Enable Grindr Unlimited features"
) {
    private val userSession = "com.grindrapp.android.storage.b"
    private val subscribeToInterstitialsList = listOf(
        "af.z0",       // Viewed Me
        "t6.t",        // Favorites
        "mc.o0\$a\$a", // Chat ($1)
        "mc.p0\$a\$a", // Chat ($2)
        "mc.q0\$a\$a", // Chat ($3)
        "wd.s1\$a\$a"  // Profiles
    )

    override fun init() {
        val userSessionClass = findClass(userSession)

        userSessionClass.hook( // hasFeature()
            "q", HookStage.BEFORE
        ) { param ->
            val disallowedFeatures = setOf("DisableScreenshot")
            param.result = param.arg(0) !in disallowedFeatures
        }

        userSessionClass.hook( // isNoXtraUpsell()
            "i", HookStage.BEFORE
        ) { param ->
            param.result = true
        }

        userSessionClass.hook( // isNoPlusUpsell()
            "E", HookStage.BEFORE
        ) { param ->
            param.result = true
        }

        userSessionClass.hook( // isFree()
            "v", HookStage.BEFORE
        ) { param ->
            param.result = false
        }

        userSessionClass.hook(
            "t", HookStage.BEFORE
        ) { param ->
            param.result = false
        }

        userSessionClass.hook(
            "C", HookStage.BEFORE
        ) { param ->
            param.result = false
        }

        userSessionClass.hook(
            "B", HookStage.BEFORE
        ) { param ->
            param.result = true
        }

        subscribeToInterstitialsList.forEach {
            findClass(it)
                .hook("emit", HookStage.BEFORE) { param ->
                    val modelName = param.arg<Any>(0)::class.java.name
                    if (!modelName.contains("NoInterstitialCreated")
                        && !modelName.contains("OnInterstitialDismissed")
                    ) {
                        param.result = null
                    }
                }
        }
    }
}