package com.grindrplus.hooks

import com.grindrplus.utils.Hook
import com.grindrplus.utils.HookStage
import com.grindrplus.utils.hook

class SignatureSpoofer :
    Hook("Signature Spoofer", "Allow logging in with Google while using LSPatch") {
    private val packageSignature = "823f5a17c33b16b4775480b31607e7df35d67af8"
    private val androidUtilsLight = "com.google.android.gms.common.util.AndroidUtilsLight"
    private val firebaseInstallationServiceClient =
        "com.google.firebase.installations.remote.FirebaseInstallationServiceClient"
    private val configRealtimeHttpClient =
        "com.google.firebase.remoteconfig.internal.ConfigRealtimeHttpClient"
    private val configFetchHttpClient =
        "com.google.firebase.remoteconfig.internal.ConfigFetchHttpClient"
    private val imgAndroidG = "ly.img.android.g"

    @OptIn(ExperimentalStdlibApi::class)
    override fun init() {
        findClass(androidUtilsLight).hook("getPackageCertificateHashBytes", HookStage.BEFORE) {
                param ->
            param.setResult(packageSignature.hexToByteArray())
        }

        for (className in
        listOf(
            firebaseInstallationServiceClient,
            configRealtimeHttpClient,
            configFetchHttpClient
        )) {
            findClass(className).hook("getFingerprintHashForPackage", HookStage.BEFORE) { param ->
                param.setResult(packageSignature)
            }
        }

        findClass(imgAndroidG).hook("a", HookStage.BEFORE) { param ->
            param.setResult(true)
        }
    }
}
