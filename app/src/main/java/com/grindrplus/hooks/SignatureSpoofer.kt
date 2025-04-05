package com.grindrplus.hooks

import android.content.ContextWrapper
import com.grindrplus.core.Constants.GRINDR_PACKAGE_NAME
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers.findAndHookMethod
import de.robv.android.xposed.callbacks.XC_LoadPackage

class SignatureSpoofer {
    private val packageSignature = "823f5a17c33b16b4775480b31607e7df35d67af8"
    private val androidUtilsLight = "com.google.android.gms.common.util.AndroidUtilsLight"
    private val firebaseInstallationServiceClient =
        "com.google.firebase.installations.remote.FirebaseInstallationServiceClient"
    private val configRealtimeHttpClient =
        "com.google.firebase.remoteconfig.internal.ConfigRealtimeHttpClient"
    private val configFetchHttpClient =
        "com.google.firebase.remoteconfig.internal.ConfigFetchHttpClient"

    @OptIn(ExperimentalStdlibApi::class)
    fun init(lpparam: XC_LoadPackage.LoadPackageParam) {

        listOf(
            firebaseInstallationServiceClient,
            configRealtimeHttpClient,
            configFetchHttpClient
        ).forEach { className ->
            findAndHookMethod(
                className,
                lpparam.classLoader,
                "getFingerprintHashForPackage",
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam<*>) {
                        param.result = packageSignature
                    }
                })
        }

        findAndHookMethod(
            "ly.img.android.e",
            lpparam.classLoader,
            "d",
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam<*>) {
                    param.result = GRINDR_PACKAGE_NAME
                }
            })


        if (lpparam.packageName != GRINDR_PACKAGE_NAME) {
            fun isFirebaseInstallationServiceClient() = Thread.currentThread().stackTrace.any {
                it.className.startsWith("com.google.firebase.installations.remote.FirebaseInstallationServiceClient")
            }

            findAndHookMethod(
                ContextWrapper::class.java,
                "getPackageName",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam<*>) {
                        if (isFirebaseInstallationServiceClient()) {
                            param.result = GRINDR_PACKAGE_NAME
                        }
                    }
                }
            )

            findAndHookMethod(
                "com.google.firebase.messaging.Metadata",
                lpparam.classLoader,
                "getPackageInfo",
                String::class.java,  // packageName
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam<*>) {
                        if ((param.args[0] as String).contains("grindr")) {
                            param.args[0] = GRINDR_PACKAGE_NAME
                        }
                    }
                }
            )
        }
    }
}