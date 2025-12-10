package com.grindrplus.hooks

import com.grindrplus.core.logi
import com.grindrplus.utils.Hook
import com.grindrplus.utils.HookStage
import com.grindrplus.utils.RetrofitUtils.RETROFIT_NAME
import com.grindrplus.utils.RetrofitUtils.createServiceProxy
import com.grindrplus.utils.hook

class DisableAnalytics : Hook(
    "Disable analytics",
    "Disable Grindr analytics (data collection)"
) {
    override fun init() {
        // Braze
        findClass("com.braze.Braze\$Companion")
            // See https://braze-inc.github.io/braze-android-sdk/kdoc/braze-android-sdk/com.braze/-braze/-companion/outbound-network-requests-offline.html
            .hook("setOutboundNetworkRequestsOffline", HookStage.BEFORE) {
                param -> param.setArg(0, true)
            }

        // Digital Turbine
        findClass("com.fyber.inneractive.sdk.network.i")
            .hook("a", HookStage.BEFORE) {
                param -> param.setResult(null)
            }

        // Google Analytics
        findClass("com.google.firebase.analytics.FirebaseAnalytics")
            .hook("setAnalyticsCollectionEnabled", HookStage.BEFORE) { param ->
                param.setArg(0, false)
            }

        // Google Crashlytics
        findClass("com.google.firebase.crashlytics.FirebaseCrashlytics")
            .hook("setCrashlyticsCollectionEnabled", HookStage.BEFORE) { param ->
                param.setArg(0, false)
            }

        // Ironsource
        findClass("com.ironsource.mediationsdk.server.ServerURL")
            .hook("getRequestURL", HookStage.BEFORE) {
                param -> param.setResult(null)
            }

        // Liftoff (Vungle)
        findClass("com.vungle.ads.internal.network.VungleApiClient")
            .hook("config", HookStage.BEFORE) {
                param -> param.setResult(null)
            }

        // Unity
        findClass("com.unity3d.services.ads.UnityAdsImplementation")
            .hook("getInstance", HookStage.BEFORE) {
                param -> param.setResult(null)
            }
    }
}