package com.grindrplus.hooks

import android.app.Activity
import android.os.Handler
import android.os.Looper
import com.grindrplus.core.logd
import com.grindrplus.core.loge
import com.grindrplus.core.logi
import com.grindrplus.utils.Hook
import com.grindrplus.utils.HookAdapter
import com.grindrplus.utils.HookStage
import com.grindrplus.utils.hook
import de.robv.android.xposed.XposedHelpers.callMethod

class WebSocketAlive : Hook(
    "Keep Alive WebSocket",
    "Prevents WebSocket disconnections when app goes to background. Causes battery drain, use with caution."
) {
    private val safeDkLifecycleManager = "com.safedk.android.internal.b"
    private val webSocketClientImpl = "com.grindrapp.android.network.websocket.WebSocketClientImpl"
    private val webSocketFactory = "Ab.p"

    override fun init() {
        hookSafeDkBackgroundDetection()
        hookWebSocketLifecycle()
        hookWebSocketFactory()
    }

    private fun hookSafeDkBackgroundDetection() {
        try {
            findClass(safeDkLifecycleManager).hook("isInBackground", HookStage.BEFORE) { param ->
                logd("Spoofing SafeDK background detection")
                param.setResult(false)
            }

            findClass(safeDkLifecycleManager).hook("a", HookStage.BEFORE) { param ->
                if (param.args().isNotEmpty()) {
                    val isBackground = param.arg<Boolean>(0)
                    if (isBackground) {
                        logd("Preventing SafeDK from setting background state")
                        param.setResult(null)
                    }
                } else {
                    logd("SafeDK method 'a' called with no parameters")
                    param.setResult(null)
                }
            }

            findClass(safeDkLifecycleManager).hook("b", HookStage.BEFORE) { param ->
                logd("Preventing SafeDK background identification")
                param.setResult(null)
            }

            findClass(safeDkLifecycleManager).hook("onActivityStopped", HookStage.BEFORE) { param ->
                logd("Intercepting SafeDK onActivityStopped")
                if (param.args().isNotEmpty()) {
                    handleActivityStopped(param as HookAdapter<Any>)
                }
                param.setResult(null)
            }

            findClass(safeDkLifecycleManager).hook("registerBackgroundForegroundListener", HookStage.AFTER) { param ->
                if (param.args().isNotEmpty()) {
                    val listener = param.arg<Any>(0)
                    try {
                        callMethod(listener, "h")
                    } catch (e: Exception) {
                        // that's fine, we just want to ensure the listener is registered
                    }
                }
            }
        } catch (e: Exception) {
            loge("Failed to hook SafeDK background detection: $e")
        }
    }

    private fun hookWebSocketLifecycle() {
        try {
            findClass(webSocketClientImpl).hook("disconnect", HookStage.BEFORE) { param ->
                if (isBackgroundTriggeredDisconnect()) {
                    logd("Preventing background-triggered WebSocket disconnect")
                    param.setResult(null)
                }
            }

            findClass(webSocketClientImpl).hook("d", HookStage.BEFORE) { param ->
                val code = param.arg<Int>(0)
                val reason = param.arg<String>(1)

                if (isBackgroundRelatedDisconnect(code, reason)) {
                    logd("Blocking background-related WebSocket disconnect: $reason")
                    param.setResult(null)
                }
            }

            findClass(webSocketClientImpl).hook("onClosed", HookStage.AFTER) { param ->
                val code = param.arg<Int>(1)
                val reason = param.arg<String>(2)

                if (shouldAutoReconnect(code, reason)) {
                    logd("Scheduling WebSocket auto-reconnect")
                    scheduleReconnection(param.thisObject(), 2000)
                }
            }

            findClass(webSocketClientImpl).hook("onFailure", HookStage.AFTER) { param ->
                val throwable = param.arg<Throwable>(1)
                val message = throwable.message?.lowercase() ?: ""

                if (isNetworkRelatedFailure(message)) {
                    logd("Scheduling WebSocket auto-reconnect after network failure")
                    scheduleReconnection(param.thisObject(), 5000)
                }
            }

            logi("Successfully hooked WebSocket lifecycle methods")

        } catch (e: Exception) {
            loge("Failed to hook WebSocket lifecycle: $e")
        }
    }

    private fun hookWebSocketFactory() {
        try {
            findClass(webSocketFactory).hook("a", HookStage.AFTER) { param ->
                val webSocketUrl = param.arg<String>(0)
                logd("WebSocket connection created to: $webSocketUrl")
            }
        } catch (e: Exception) {
            loge("Failed to hook WebSocket factory: $e")
        }
    }

    private fun handleActivityStopped(param: HookAdapter<Any>) {
        try {
            val thisObject = param.thisObject()
            val activity = param.arg<Activity>(0)

            val isBackgroundBefore = callMethod(thisObject, "isInBackground") as Boolean

            val backgroundField = thisObject.javaClass.getDeclaredField("g")
            backgroundField.isAccessible = true
            val isBackgroundAfter = backgroundField.getBoolean(thisObject)

            if (!isBackgroundBefore && isBackgroundAfter) {
                logd("Reverting background state change from SafeDK")
                backgroundField.setBoolean(thisObject, false)
            }

        } catch (e: Exception) {
            loge("Error in SafeDK onActivityStopped hook: $e")
        }
    }

    private fun isBackgroundTriggeredDisconnect(): Boolean {
        val stackTrace = Thread.currentThread().stackTrace
        return stackTrace.any {
            it.methodName.contains("background", ignoreCase = true) ||
                    it.methodName.contains("pause", ignoreCase = true) ||
                    it.className.contains("lifecycle", ignoreCase = true) ||
                    it.className.contains("safedk", ignoreCase = true)
        }
    }

    private fun isBackgroundRelatedDisconnect(code: Int, reason: String): Boolean {
        return reason.contains("background", ignoreCase = true) ||
                reason.contains("inactive", ignoreCase = true) ||
                reason.contains("idle", ignoreCase = true) ||
                code == 1001
    }

    private fun shouldAutoReconnect(code: Int, reason: String): Boolean {
        return code == 1001 ||
                reason.contains("background", ignoreCase = true) ||
                reason.contains("inactive", ignoreCase = true)
    }

    private fun isNetworkRelatedFailure(message: String): Boolean {
        return message.contains("network") ||
                message.contains("timeout") ||
                message.contains("connection reset") ||
                message.contains("socket closed")
    }

    private fun scheduleReconnection(webSocketClient: Any, delayMs: Long) {
        Handler(Looper.getMainLooper()).postDelayed({
            try {
                val urlField = webSocketClient.javaClass.getDeclaredField("c")
                urlField.isAccessible = true
                val authToken = urlField.get(webSocketClient) as? String

                if (authToken != null) {
                    callMethod(webSocketClient, "b", authToken)
                    logi("WebSocket reconnection initiated")
                } else {
                    logd("Cannot reconnect WebSocket - no auth token found")
                }
            } catch (e: Exception) {
                loge("Failed to auto-reconnect WebSocket: $e")
            }
        }, delayMs)
    }
}