package com.grindrplus.hooks

import android.annotation.SuppressLint
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

@OptIn(ExperimentalStdlibApi::class)
@SuppressLint("CustomX509TrustManager", "TrustAllX509TrustManager", "BadHostnameVerifier")
fun sslUnpinning(param: XC_LoadPackage.LoadPackageParam) {
    findAndHookConstructor(
        "okhttp3.OkHttpClient\$Builder",
        param.classLoader,
        object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam<*>) {
                val trustAlLCerts =
                    arrayOf<TrustManager>(
                        object : X509TrustManager {
                            override fun checkClientTrusted(
                                chain: Array<out X509Certificate>?,
                                authType: String?,
                            ) {}

                            override fun checkServerTrusted(
                                chain: Array<out X509Certificate>?,
                                authType: String?,
                            ) {}

                            override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
                        }
                    )
                val sslContext = SSLContext.getInstance("TLSv1.3")
                sslContext.init(null, trustAlLCerts, SecureRandom())
                callMethod(
                    param.thisObject,
                    "sslSocketFactory",
                    sslContext.socketFactory,
                    trustAlLCerts.first() as X509TrustManager
                )
                callMethod(
                    param.thisObject,
                    "hostnameVerifier",
                    object : HostnameVerifier {
                        override fun verify(hostname: String?, session: SSLSession?): Boolean = true
                    }
                )
            }
        }
    )

    findAndHookMethod(
        "okhttp3.OkHttpClient\$Builder",
        param.classLoader,
        "certificatePinner",
        "okhttp3.CertificatePinner",
        XC_MethodReplacement.DO_NOTHING
    )

    findAndHookMethod(
        "com.android.org.conscrypt.TrustManagerImpl",
        param.classLoader,
        "verifyChain",
        List::class.java, // List<X509Certificate> untrustedChain
        List::class.java, // List<TrustAnchor> trustAnchorChain
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
