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
        "qf.z0",       // Viewed Me
        "c7.s",        // Favorites
        "ed.n0\$a\$a", // Chat ($1)
        "ed.o0\$a\$a", // Chat ($2)
        "ed.p0\$a\$a", // Chat ($3)
        "oe.s1\$a\$a"  // Profiles
    )

    override fun init() {
        val userSessionClass = findClass(userSession)

        userSessionClass.hook( // hasFeature()
            "m", HookStage.BEFORE
        ) { param ->
            val disallowedFeatures = setOf("DisableScreenshot")
            param.result = param.args[0].toString() !in disallowedFeatures
        }

        userSessionClass.hook( // isNoXtraUpsell()
            "k", HookStage.BEFORE
        ) { param ->
            param.result = true
        }

        userSessionClass.hook( // isNoPlusUpsell()
            "D", HookStage.BEFORE
        ) { param ->
            param.result = true
        }

        userSessionClass.hook( // isFree()
            "w", HookStage.BEFORE
        ) { param ->
            param.result = false
        }

        userSessionClass.hook( // isFreeXtra()
            "u", HookStage.BEFORE
        ) { param ->
            param.result = false
        }

        userSessionClass.hook( // isFreePlus()
            "B", HookStage.BEFORE
        ) { param ->
            param.result = false
        }

        userSessionClass.hook( // isFreeUnlimited()
            "A", HookStage.BEFORE
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