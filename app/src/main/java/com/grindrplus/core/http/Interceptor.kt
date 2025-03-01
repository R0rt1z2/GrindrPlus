package com.grindrplus.core.http

import com.grindrplus.GrindrPlus
import de.robv.android.xposed.XposedBridge
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.util.TimeZone
import okhttp3.Request.Builder

class Interceptor(
    private val userSession: Any,
    private val userAgent: Any,
    private val deviceInfo: Any
) :
    Interceptor {
    private fun modifyRequest(originalRequest: Request): Request {
        try {
            // search for 'return value != null && value.length() > 0' in userSession
            val isLoggedIn = invokeMethodSafe(userSession, "r") as Boolean

            // search for 'return FlowKt.asStateFlow' in userSession (return type is String)
            val authTokenFlow = invokeMethodSafe(userSession, "s")
            val authToken = invokeMethodSafe(authTokenFlow, "getValue") as String

            // search for one line method returning an string in userSession
            val roles = invokeMethodSafe(userSession, "x") as String

            // search for 'getValue().getNameTitleCase()' in userAgent
            val userAgent = invokeMethodSafe(userAgent, "a") as String

            // search for 'if (info == null) {' in deviceInfo
            val deviceInfoLazy = getFieldSafe(deviceInfo, "c") as Any
            val lDeviceInfo = invokeMethodSafe(deviceInfoLazy, "getValue") as String

            val builder: Builder = originalRequest.newBuilder()

            if (isLoggedIn) {
                builder.header("Authorization", "Grindr3 $authToken")
                builder.header("L-Time-Zone", TimeZone.getDefault().id)
                builder.header("L-Grindr-Roles", roles)
                builder.header("L-Device-Info", lDeviceInfo)
            } else {
                builder.header("L-Time-Zone", "Unknown")
            }

            builder.header("Accept", "application/json; charset=UTF-8")
            builder.header("User-Agent", userAgent)
            builder.header("L-Locale", "en_US")
            builder.header("Accept-language", "en-US")

            return builder.build()
        } catch (e: Exception) {
            throw IOException("Failed to modify request: " + e.message, e)
        }
    }

    private fun invokeMethodSafe(userSession: Any, s: String): Any {
        return try {
            userSession::class.java.getMethod(s).invoke(userSession)
        } catch (e: Exception) {
            XposedBridge.log("Failed to invoke method: ${e.printStackTrace()} $s")
        }
    }

    private fun getFieldSafe(userSession: Any, fieldName: String): Any? {
        return try {
            val field = userSession::class.java.getDeclaredField(fieldName)
            field.isAccessible = true
            val value = field.get(userSession)
            value
        } catch (e: Exception) {
            XposedBridge.log("Failed to get field: ${e.message} $fieldName")
            null
        }
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request: Request = chain.request()
        try {
            return chain.proceed(modifyRequest(request))
        } catch (e: Exception) {
            throw IOException("Failed to intercept request: " + e.message, e)
        }
    }
}