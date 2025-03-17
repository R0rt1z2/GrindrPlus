package com.grindrplus.hooks

import android.view.View
import com.grindrplus.GrindrPlus
import com.grindrplus.ui.Utils.getId
import com.grindrplus.utils.Hook
import com.grindrplus.utils.HookStage
import com.grindrplus.utils.hook
import de.robv.android.xposed.XposedHelpers.callMethod

class EnableUnlimited : Hook(
    "Enable unlimited",
    "Enable Grindr Unlimited features"
) {
    private val userSession = "sa.T" // search for 'com.grindrapp.android.storage.UserSessionImpl$1'
    private val subscribeToInterstitialsList = listOf(
        "u5.E\$a" // search for 'com.grindrapp.android.chat.presentation.ui.ChatActivityV2$subscribeToInterstitialAds$1$1$1'
    )
    override fun init() {
        val userSessionClass = findClass(userSession)

        userSessionClass.hook( // hasFeature()
            "x", HookStage.BEFORE // search for 'Intrinsics.checkNotNullParameter(feature, "feature")' in userSession
        ) { param ->
            val disallowedFeatures = setOf("DisableScreenshot")
            param.setResult(param.arg(0, String::class.java) !in disallowedFeatures)
        }

        userSessionClass.hook( // isNoXtraUpsell()
            "m", HookStage.BEFORE // search for '()) ? false : true;' in userSession
        ) { param ->
            param.setResult(true)
        }

        userSessionClass.hook( // isNoPlusUpsell()
            "G", HookStage.BEFORE // search for 'Role.PLUS, Role.FREE_PLUS' in userSession
        ) { param ->
            param.setResult(true)
        }

        userSessionClass.hook( // isFree()
            "y", HookStage.BEFORE // search for '.isEmpty();' in userSession
        ) { param ->
            param.setResult(false)
        }

        userSessionClass.hook( // isFreeXtra()
            "v", HookStage.BEFORE // search for 'Role.XTRA, Role.FREE_XTRA' in userSession
        ) { param ->
            param.setResult(false)
        }

        userSessionClass.hook( // isFreeUnlimited()
            "D", HookStage.BEFORE // search for 'Role.UNLIMITED, Role.FREE_UNLIMITED' in userSession
        ) { param ->
            param.setResult(true)
        }

        subscribeToInterstitialsList.forEach {
            findClass(it)
                .hook("emit", HookStage.BEFORE) { param ->
                    val modelName = param.arg<Any>(0)::class.java.name
                    if (!modelName.contains("NoInterstitialCreated")
                        && !modelName.contains("OnInterstitialDismissed")
                    ) {
                        param.setResult(null)
                    }
                }
        }

        // search for '(str, "treatment_exact_count") ?'
        findClass("S1.a").hook("b", HookStage.BEFORE) { param ->
           param.setResult(false)
        }
    }
}