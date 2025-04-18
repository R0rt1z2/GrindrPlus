package com.grindrplus

import android.annotation.SuppressLint
import android.app.Application
import android.os.Build
import android.os.Handler
import com.grindrplus.core.Constants.GRINDR_PACKAGE_NAME
import com.grindrplus.hooks.spoofSignatures
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
import timber.log.Timber
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSession
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

@SuppressLint("TrustAllX509TrustManager", "BadHostnameVerifier")
class XposedLoader : IXposedHookZygoteInit, IXposedHookLoadPackage {
    private lateinit var modulePath: String

    override fun initZygote(startupParam: IXposedHookZygoteInit.StartupParam) {
        modulePath = startupParam.modulePath
    }

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (!lpparam.packageName.contains(GRINDR_PACKAGE_NAME)) {
            println("Module mismatch! package is ${lpparam.packageName}, expected $GRINDR_PACKAGE_NAME")
            return
        }

        println("Spoofing signatures..")
        spoofSignatures(lpparam)

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
                                authType: String?,
                            ) {
                            }

                            override fun checkServerTrusted(
                                chain: Array<out X509Certificate>?,
                                authType: String?,
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

            // Hook TrustManagerImpl to be able to unpin more information
            findAndHookMethod(
                "com.android.org.conscrypt.TrustManagerImpl",
                lpparam.classLoader,
                "verifyChain",
                List::class.java,  // List<X509Certificate> untrustedChain
                List::class.java,  // List<TrustAnchor> trustAnchorChain
                String::class.java, // String host
                Boolean::class.java, // boolean clientAuth
                ByteArray::class.java, // byte[] ocspData
                ByteArray::class.java, // byte[] tlsSctData
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam<*>) {
                        param.result = param.args[0]
                    }
                }
            )
        }

        Application::class.java.hook("attach", HookStage.AFTER) {
            val application = it.thisObject()
            GrindrPlus.init(modulePath, application,
                BuildConfig.TARGET_GRINDR_VERSION_CODES,
                BuildConfig.TARGET_GRINDR_VERSION_NAMES)
        }
    }
}