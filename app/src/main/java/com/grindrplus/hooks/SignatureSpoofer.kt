package com.grindrplus.hooks

import android.content.ContextWrapper
import com.grindrplus.core.Constants.GRINDR_PACKAGE_NAME
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.XposedHelpers.findAndHookMethod
import de.robv.android.xposed.callbacks.XC_LoadPackage

private const val packageSignature = "823f5a17c33b16b4775480b31607e7df35d67af8"
private const val firebaseInstallationServiceClient =
    "com.google.firebase.installations.remote.FirebaseInstallationServiceClient"
private const val configRealtimeHttpClient =
    "com.google.firebase.remoteconfig.internal.ConfigRealtimeHttpClient"
private const val configFetchHttpClient =
    "com.google.firebase.remoteconfig.internal.ConfigFetchHttpClient"

@OptIn(ExperimentalStdlibApi::class)
fun spoofSignatures(param: XC_LoadPackage.LoadPackageParam) {

    listOf(
        firebaseInstallationServiceClient,
        configRealtimeHttpClient,
        configFetchHttpClient
    ).forEach { className ->
        findAndHookMethod(
            className,
            param.classLoader,
            "getFingerprintHashForPackage",
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam<*>) {
                    param.result = packageSignature
                }
            })
    }

    findAndHookMethod(
        "ly.img.android.c",
        param.classLoader,
        "d", // getPackageName
        object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam<*>) {
                param.result = GRINDR_PACKAGE_NAME
            }
        })

    // The Facebook SDK tries to handle the login using the Facebook app in case it is installed.
    // However, the Facebook app does signature checks with the app that is requesting the authentication,
    // which ends up making the Facebook server reject with an invalid key hash for the app signature.
    // Override the Facebook SDK to always handle the login using the web browser, which does not perform
    // signature checks.
    //
    // Always return 0 (no Intent was launched) as the result of trying to authorize with the Facebook app to
    // make the login fallback to a web browser window.
    //
    findAndHookMethod(
        "com.facebook.login.KatanaProxyLoginMethodHandler",
        param.classLoader,
        "tryAuthorize",
        XposedHelpers.findClass("com.facebook.login.LoginClient\$Request", param.classLoader),
        object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam<*>) {
                param.result = 0
            }
        }
    )

    if (param.packageName != GRINDR_PACKAGE_NAME) {
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
            param.classLoader,
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