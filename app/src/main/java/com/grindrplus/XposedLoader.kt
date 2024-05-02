package com.grindrplus

import android.app.Application
import android.widget.Toast
import com.grindrplus.core.Constants.GRINDR_PACKAGE_NAME
import com.grindrplus.utils.HookStage
import com.grindrplus.utils.hook
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.IXposedHookZygoteInit
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedHelpers.findAndHookMethod
import de.robv.android.xposed.callbacks.XC_LoadPackage

class XposedLoader : IXposedHookZygoteInit, IXposedHookLoadPackage {
    private lateinit var modulePath: String

    override fun initZygote(startupParam: IXposedHookZygoteInit.StartupParam) {
        modulePath = startupParam.modulePath
    }

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (lpparam.packageName != GRINDR_PACKAGE_NAME) return

        if (BuildConfig.DEBUG) {
            // disable SSL pinning if running in debug mode
            findAndHookMethod(
                "okhttp3.OkHttpClient\$Builder",
                lpparam.classLoader,
                "certificatePinner",
                "okhttp3.CertificatePinner",
                XC_MethodReplacement.DO_NOTHING
            )
        }

        Application::class.java.hook("onCreate", HookStage.BEFORE) {
            val application = it.thisObject
            val pkgInfo = application.packageManager.getPackageInfo(application.packageName, 0)

            if (pkgInfo.versionName != BuildConfig.TARGET_GRINDR_VERSION) {
                Toast.makeText(
                    application,
                    "GrindrPlus: Grindr version mismatch (installed: ${pkgInfo.versionName}, expected: ${BuildConfig.TARGET_GRINDR_VERSION}). Mod disabled.",
                    Toast.LENGTH_LONG
                ).show()
                return@hook
            }

            GrindrPlus.init(modulePath, application)
        }
    }
}