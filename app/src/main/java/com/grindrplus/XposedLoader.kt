package com.grindrplus

import android.app.Application
import android.util.Log
import com.grindrplus.core.Constants.GRINDR_PACKAGE_NAME
import com.grindrplus.hooks.spoofSignatures
import com.grindrplus.hooks.sslUnpinning
import com.grindrplus.utils.HookStage
import com.grindrplus.utils.hook
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedHelpers.findAndHookMethod
import de.robv.android.xposed.callbacks.XC_LoadPackage

class XposedLoader : IXposedHookLoadPackage {
    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (lpparam.packageName.startsWith("com.grindrplus")) {
            findAndHookMethod(
                "com.grindrplus.manager.utils.MiscUtilsKt",
                lpparam.classLoader,
                "isLSPosed",
                XC_MethodReplacement.returnConstant(true)
            )
        }

        if (!lpparam.packageName.contains(GRINDR_PACKAGE_NAME)) return

        // XposedHelpers.ClassNotFoundError extends Error (not Exception) — must catch Throwable.
        // A failure here should disable the feature without aborting the entire module.
        try {
            spoofSignatures(lpparam)
        } catch (e: Throwable) {
            Log.e("GrindrPlus", "spoofSignatures failed: ${e.message}", e)
        }
        if (BuildConfig.DEBUG) {
            try {
                sslUnpinning(lpparam)
            } catch (e: Throwable) {
                Log.e("GrindrPlus", "sslUnpinning failed: ${e.message}", e)
            }
        }

        Application::class.java.hook("attach", HookStage.AFTER) {
            val application = it.thisObject()
            GrindrPlus.init(application,
                BuildConfig.TARGET_GRINDR_VERSION_CODES,
                BuildConfig.TARGET_GRINDR_VERSION_NAMES)
        }
    }
}