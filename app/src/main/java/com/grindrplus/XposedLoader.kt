package com.grindrplus

import android.app.Application
import android.widget.Toast
import com.grindrplus.core.Constants.GRINDR_PACKAGE_NAME
import com.grindrplus.core.Logger
import com.grindrplus.utils.HookStage
import com.grindrplus.utils.hook
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.IXposedHookZygoteInit
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedHelpers.callMethod
import de.robv.android.xposed.XposedHelpers.findAndHookConstructor
import de.robv.android.xposed.XposedHelpers.findAndHookMethod
import de.robv.android.xposed.callbacks.XC_LoadPackage
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSession
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

class XposedLoader : IXposedHookZygoteInit, IXposedHookLoadPackage {
    private lateinit var modulePath: String

    override fun initZygote(startupParam: IXposedHookZygoteInit.StartupParam) {
        modulePath = startupParam.modulePath
    }

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (lpparam.packageName != GRINDR_PACKAGE_NAME) return

        if (BuildConfig.DEBUG) {
            // disable SSL pinning if running in debug mode
            findAndHookConstructor(
                "okhttp3.OkHttpClient\$Builder",
                lpparam.classLoader,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam<*>) {
                        val trustAlLCerts = arrayOf<TrustManager>(object : X509TrustManager {
                            override fun checkClientTrusted(
                                chain: Array<out X509Certificate>?,
                                authType: String?
                            ) {
                            }

                            override fun checkServerTrusted(
                                chain: Array<out X509Certificate>?,
                                authType: String?
                            ) {
                            }

                            override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()

                        })
                        val sslContext = SSLContext.getInstance("TLSv1.3")
                        sslContext.init(null, trustAlLCerts, SecureRandom())
                        callMethod(
                            param.thisObject,
                            "sslSocketFactory",
                            sslContext.socketFactory,
                            trustAlLCerts.first() as X509TrustManager
                        )
                        callMethod(param.thisObject, "hostnameVerifier", object : HostnameVerifier {
                            override fun verify(hostname: String?, session: SSLSession?): Boolean =
                                true
                        })
                    }
                })

            findAndHookMethod(
                "okhttp3.OkHttpClient\$Builder",
                lpparam.classLoader,
                "certificatePinner",
                "okhttp3.CertificatePinner",
                XC_MethodReplacement.DO_NOTHING
            )
        }

        Application::class.java.hook("attach", HookStage.AFTER) {
            val application = it.thisObject()
            val pkgInfo = application.packageManager.getPackageInfo(application.packageName, 0)
            val logger = Logger(application.filesDir.absolutePath + "/grindrplus.log")

            if (pkgInfo.versionName !in BuildConfig.TARGET_GRINDR_VERSIONS) {
                Toast.makeText(
                    application,
                    "GrindrPlus: Grindr version mismatch (installed: ${pkgInfo.versionName}, expected one of: ${BuildConfig.TARGET_GRINDR_VERSIONS.joinToString("_")}). Mod disabled.",
                    Toast.LENGTH_LONG
                ).show()
                logger.log("Grindr version mismatch (installed: ${pkgInfo.versionName}, expected one of: ${BuildConfig.TARGET_GRINDR_VERSIONS.joinToString("_")}). Mod disabled.")
                return@hook
            }

            GrindrPlus.init(modulePath, application, logger)
        }
    }
}