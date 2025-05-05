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
        if (!BuildConfig.DEBUG) {
            return
        }

        try {
            val forestClass = findClass("timber.log.Timber\$Forest")
            val logMethods = arrayOf("v", "d", "i", "w", "e", "wtf")

            for (method in logMethods) {
                forestClass.hook(method, HookStage.BEFORE) { param ->
                    if (param.args().isNotEmpty() && param.args()[0] is String) {
                        val priority =
                            when (method) {
                                "v" -> Log.VERBOSE
                                "d" -> Log.DEBUG
                                "i" -> Log.INFO
                                "w" -> Log.WARN
                                "e" -> Log.ERROR
                                "wtf" -> Log.ASSERT
                                else -> Log.INFO
                            }

                        val message = param.args()[0] as String
                        val tag =
                            XposedHelpers.getObjectField(param.thisObject(), "explicitTag")
                                ?.toString() ?: "Timber"

                        when (priority) {
                            Log.VERBOSE -> Log.v(tag, message)
                            Log.DEBUG -> Log.d(tag, message)
                            Log.INFO -> Log.i(tag, message)
                            Log.WARN -> Log.w(tag, message)
                            Log.ERROR -> Log.e(tag, message)
                            Log.ASSERT -> Log.e(tag, message)
                        }
                    }
                }

                forestClass.hook(method, HookStage.BEFORE) { param ->
                    if (
                        param.args().size >= 2 &&
                        param.args()[0] is Throwable &&
                        param.args()[1] is String
                    ) {
                        val priority =
                            when (method) {
                                "v" -> Log.VERBOSE
                                "d" -> Log.DEBUG
                                "i" -> Log.INFO
                                "w" -> Log.WARN
                                "e" -> Log.ERROR
                                "wtf" -> Log.ASSERT
                                else -> Log.INFO
                            }

                        val throwable = param.args()[0] as Throwable
                        val message = param.args()[1] as String
                        val tag =
                            XposedHelpers.getObjectField(param.thisObject(), "explicitTag")
                                ?.toString() ?: "Timber"

                        val fullMessage = "$tag: $message - Exception: ${throwable.message}"

                        when (priority) {
                            Log.VERBOSE -> Log.v(tag, fullMessage)
                            Log.DEBUG -> Log.d(tag, fullMessage)
                            Log.INFO -> Log.i(tag, fullMessage)
                            Log.WARN -> Log.w(tag, fullMessage)
                            Log.ERROR -> Log.e(tag, fullMessage)
                            Log.ASSERT -> Log.e(tag, fullMessage)
                        }
                    }
                }

                forestClass.hook(method, HookStage.BEFORE) { param ->
                    if (param.args().size == 1 && param.args()[0] is Throwable) {
                        val priority =
                            when (method) {
                                "v" -> Log.VERBOSE
                                "d" -> Log.DEBUG
                                "i" -> Log.INFO
                                "w" -> Log.WARN
                                "e" -> Log.ERROR
                                "wtf" -> Log.ASSERT
                                else -> Log.INFO
                            }

                        val throwable = param.args()[0] as Throwable
                        val tag =
                            XposedHelpers.getObjectField(param.thisObject(), "explicitTag")
                                ?.toString() ?: "Timber"

                        val fullMessage = "$tag: Exception: ${throwable.message}"

                        when (priority) {
                            Log.VERBOSE -> Log.v(tag, fullMessage)
                            Log.DEBUG -> Log.d(tag, fullMessage)
                            Log.INFO -> Log.i(tag, fullMessage)
                            Log.WARN -> Log.w(tag, fullMessage)
                            Log.ERROR -> Log.e(tag, fullMessage)
                            Log.ASSERT -> Log.e(tag, fullMessage)
                        }
                    }
                }
            }

            findClass("timber.log.Timber\$Tree").hook("log", HookStage.BEFORE) { param ->
                if (param.args().size >= 4) {
                    val priority = param.args()[0] as Int
                    val tag = param.args()[1] as? String ?: "Unknown"
                    val message = param.args()[2] as? String ?: "null"
                    val throwable = param.args()[3] as? Throwable

                    val fullMsg =
                        if (throwable != null) {
                            "$message - Exception: ${throwable.message}"
                        } else {
                            message
                        }

                    when (priority) {
                        Log.VERBOSE -> Log.v(tag, fullMsg)
                        Log.DEBUG -> Log.d(tag, fullMsg)
                        Log.INFO -> Log.i(tag, fullMsg)
                        Log.WARN -> Log.w(tag, fullMsg)
                        Log.ERROR -> Log.e(tag, fullMsg)
                        Log.ASSERT -> Log.e(tag, fullMsg)
                    }
                }
            }
        } catch (e: Exception) {
            loge("Failed to hook Timber: ${e.message}")
            Logger.writeRaw(e.stackTraceToString())
        }
    }
}
