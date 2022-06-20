package com.eljaviluki.grindrplus

import android.app.Application
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam
import kotlin.time.Duration.Companion.minutes


class Hooker : IXposedHookLoadPackage {

    override fun handleLoadPackage(lpparam: LoadPackageParam) {
        if (lpparam.packageName != Constants.GRINDR_PKG) return
        pkgParam = lpparam
        XposedHelpers.findAndHookMethod(
            Application::class.java,
            "onCreate",
            object : XC_MethodHook() {
                @Throws(Throwable::class)
                override fun afterHookedMethod(param: MethodHookParam) {
                    Logger.xLog("Application.onCreate()")
                    Hooks.hookFeatureGranting()
                    Hooks.allowScreenshotsHook()
                    Hooks.unlimitedExpiringPhotos()
                    Hooks.addExtraProfileFields()
                    Hooks.hookUserSessionImpl()
                    Hooks.allowMockProvider()
                    Hooks.allowVideocallsOnEmptyChats()
                    Hooks.allowSomeExperiments()

                    //I've set this to max 3 min. If we make an UI for Hook Settings, we'll let the user to change this.
                    Hooks.hookOnlineIndicatorDuration(3.minutes)
                    Hooks.unlimitedTaps()
                }
            })
    }

    companion object {
        var pkgParam: LoadPackageParam? = null
    }
}