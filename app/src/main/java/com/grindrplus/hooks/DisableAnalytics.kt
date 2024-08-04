package com.grindrplus.hooks

import com.grindrplus.core.Utils.createServiceProxy
import com.grindrplus.utils.Hook
import com.grindrplus.utils.HookStage
import com.grindrplus.utils.hook

class DisableAnalytics : Hook(
    "Disable analytics",
    "Disable Grindr analytics (data collection)"
) {
    private val analyticsRestService = "v5.b"

    override fun init() {
        val analyticsRestServiceClass = findClass(analyticsRestService)

        //first party analytics
        findClass("retrofit2.Retrofit")
            .hook("create", HookStage.AFTER) { param ->
                val service = param.result
                if (service != null && analyticsRestServiceClass.isAssignableFrom(service.javaClass)) {
                    param.result = createServiceProxy(service, analyticsRestServiceClass)
                }
            }

        // AgoraRtcEngine (intentionally left enabled)

        // Amplitude Experiments (intentionally left enabled)

        // Amplitude Analytics
        findClass("com.amplitude.android.Configuration")
            .hook("getOptOut", HookStage.AFTER) { param ->
                param.result = true
            }

        // AppLovin
        // ctrl + f "ConnectionManager" to find this
        findClass("com.applovin.impl.b4")
            .hook("a", HookStage.BEFORE) {
                    param -> param.result = null
            }
        // some error reporting thing
        findClass("com.applovin.impl.sdk.o")
            .hook("a", HookStage.BEFORE) {
                    param -> param.result = null
            }

        // AppsFlyer
        findClass("f4.z")
            .hook("b", HookStage.BEFORE) { param ->
                param.args[0] = false
            }

        // Braze
        // URL has information for review
        // https://braze-inc.github.io/braze-android-sdk/kdoc/braze-android-sdk/com.braze/-braze/-companion/outbound-network-requests-offline.html?query=var%20outboundNetworkRequestsOffline:%20Boolean
        findClass("com.braze.Braze\$Companion")
            .hook("setOutboundNetworkRequestsOffline", HookStage.BEFORE) {
                    param -> param.args[0] = true
            }

        // Digital Turbine
        findClass("com.fyber.inneractive.sdk.network.i")
            .hook("a", HookStage.BEFORE) {
                    param -> param.result = null
            }

        // Google Tools - Media

        // Google AdMob

        // Google Ad Manager

        // Google Analytics
        findClass("com.google.firebase.analytics.FirebaseAnalytics")
            .hook("setAnalyticsCollectionEnabled", HookStage.BEFORE) { param ->
                param.args[0] = false
            }

        // Google Crashlytics
        findClass("com.google.firebase.crashlytics.FirebaseCrashlytics")
            .hook("setCrashlyticsCollectionEnabled", HookStage.BEFORE) { param ->
                param.args[0] = false
            }

        // Hybid (Verve Group) (unused or unnecessary)

        // Ironsource
        findClass("com.ironsource.mediationsdk.server.ServerURL")
            .hook("getRequestURL", HookStage.BEFORE) {
                    param -> param.result = null
            }

        // Liftoff (Vungle)
        findClass("com.vungle.ads.internal.network.VungleApiClient")
            .hook("config", HookStage.BEFORE) {
                    param -> param.result = null
            }

        // Location (unused or unnecessary)

        // OTPublishersSDK (onetrust) (intentionally left enabled)

        // Open measurement (unused or unnecessary)

        // Unity
        findClass("com.unity3d.services.ads.UnityAdsImplementation")
            .hook("getInstance", HookStage.BEFORE) {
                    param -> param.result = null
            }

        // ZendeskCoreSDK (intentionally left enabled)
    }
}