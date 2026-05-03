package com.grindrplus.hooks

import android.annotation.SuppressLint
import android.util.Log
import com.grindrplus.BuildConfig
import com.grindrplus.core.Logger
import com.grindrplus.core.loge
import com.grindrplus.utils.Hook
import com.grindrplus.utils.HookStage
import com.grindrplus.utils.hook
import de.robv.android.xposed.XposedHelpers

@SuppressLint("LogNotTimber")
class TimberLogging :
    Hook("Timber Logging", "Forces Timber to log messages even if no tree is planted") {
    override fun init() {
        if (!BuildConfig.DEBUG) return

        try {
            val forestClass = findClass("timber.log.Timber\$Forest")
            for (method in arrayOf("v", "d", "i", "w", "e", "wtf")) {
                hookForestMethod(forestClass, method)
            }

            findClass("timber.log.Timber\$Tree").hook("log", HookStage.BEFORE) { param ->
                if (param.args().size >= 4) {
                    val priority = param.args()[0] as Int
                    val tag = param.args()[1] as? String ?: "Unknown"
                    val message = param.args()[2] as? String ?: "null"
                    val throwable = param.args()[3] as? Throwable
                    val fullMsg = if (throwable != null) "$message - Exception: ${throwable.message}" else message
                    logByPriority(priority, tag, fullMsg)
                }
            }
        } catch (e: Exception) {
            loge("Failed to hook Timber: ${e.message}")
            Logger.writeRaw(e.stackTraceToString())
        }
    }

    private fun hookForestMethod(forestClass: Class<*>, method: String) {
        val priority = methodToPriority(method)

        forestClass.hook(method, HookStage.BEFORE) { param ->
            if (param.args().isNotEmpty() && param.args()[0] is String) {
                val message = param.args()[0] as String
                val tag = XposedHelpers.getObjectField(param.thisObject(), "explicitTag")?.toString() ?: "Timber"
                logByPriority(priority, tag, message)
            }
        }

        forestClass.hook(method, HookStage.BEFORE) { param ->
            if (param.args().size >= 2 && param.args()[0] is Throwable && param.args()[1] is String) {
                val throwable = param.args()[0] as Throwable
                val message = param.args()[1] as String
                val tag = XposedHelpers.getObjectField(param.thisObject(), "explicitTag")?.toString() ?: "Timber"
                logByPriority(priority, tag, "$tag: $message - Exception: ${throwable.message}")
            }
        }

        forestClass.hook(method, HookStage.BEFORE) { param ->
            if (param.args().size == 1 && param.args()[0] is Throwable) {
                val throwable = param.args()[0] as Throwable
                val tag = XposedHelpers.getObjectField(param.thisObject(), "explicitTag")?.toString() ?: "Timber"
                logByPriority(priority, tag, "$tag: Exception: ${throwable.message}")
            }
        }
    }

    private fun methodToPriority(method: String): Int = when (method) {
        "v" -> Log.VERBOSE
        "d" -> Log.DEBUG
        "i" -> Log.INFO
        "w" -> Log.WARN
        "e" -> Log.ERROR
        "wtf" -> Log.ASSERT
        else -> Log.INFO
    }

    private fun logByPriority(priority: Int, tag: String, message: String) = when (priority) {
        Log.VERBOSE -> Log.v(tag, message)
        Log.DEBUG -> Log.d(tag, message)
        Log.INFO -> Log.i(tag, message)
        Log.WARN -> Log.w(tag, message)
        Log.ERROR -> Log.e(tag, message)
        else -> Log.e(tag, message)
    }
}
