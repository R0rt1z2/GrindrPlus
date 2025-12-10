package com.grindrplus.core

import android.annotation.SuppressLint
import android.content.Context
import com.grindrplus.BuildConfig
import com.grindrplus.bridge.BridgeClient
import com.grindrplus.utils.Hook
import com.grindrplus.utils.Task
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

enum class LogLevel { DEBUG, INFO, WARNING, ERROR, SUCCESS }
enum class LogSource { MODULE, MANAGER, HOOK, TASK, BRIDGE, UNKNOWN, HTTP }

@SuppressLint("StaticFieldLeak", "ConstantLocale")
object Logger {
    private const val TAG = "GrindrPlus"
    private var isModuleContext = false
    private var bridgeClient: BridgeClient? = null
    private val hookPrefixes = ConcurrentHashMap<String, String>()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    fun initialize(context: Context, bridge: BridgeClient, isModule: Boolean) {
        bridgeClient = bridge
        isModuleContext = isModule

        if (Timber.forest().isEmpty()) {
            Timber.plant(Timber.DebugTree())
        }
    }

    fun registerHookPrefix(subName: String, prefix: String = subName) {
        hookPrefixes[subName] = prefix
    }

    fun unregisterHookPrefix(subName: String) {
        hookPrefixes.remove(subName)
    }

    fun d(message: String, source: LogSource? = null, subName: String? = null) {
        if (!debugEnabled) return
        log(message, LogLevel.DEBUG, source ?: getDefaultSource(), subName)
    }

    fun i(message: String, source: LogSource? = null, subName: String? = null) =
        log(message, LogLevel.INFO, source ?: getDefaultSource(), subName)

    fun w(message: String, source: LogSource? = null, subName: String? = null) =
        log(message, LogLevel.WARNING, source ?: getDefaultSource(), subName)

    fun e(message: String, source: LogSource? = null, subName: String? = null) =
        log(message, LogLevel.ERROR, source ?: getDefaultSource(), subName)

    fun s(message: String, source: LogSource? = null, subName: String? = null) =
        log(message, LogLevel.SUCCESS, source ?: getDefaultSource(), subName)

    fun log(
        message: String,
        level: LogLevel = LogLevel.INFO,
        source: LogSource = LogSource.UNKNOWN,
        subName: String? = null
    ) {
        val priorityChar = when(level) {
            LogLevel.DEBUG -> "V"
            LogLevel.INFO -> "I"
            LogLevel.WARNING -> "W"
            LogLevel.ERROR -> "E"
            LogLevel.SUCCESS -> "S"
        }

        val timestamp = dateFormat.format(Date())
        val sourceName = source.toString().lowercase()
        val conciseMessage = if (subName != null) {
            "$priorityChar/$timestamp/$sourceName/$subName: $message"
        } else {
            "$priorityChar/$timestamp/$sourceName: $message"
        }

        val logcatMessage = buildLogcatMessage(source, subName, message, level == LogLevel.SUCCESS)
        when (level) {
            LogLevel.DEBUG -> Timber.tag(TAG).v(logcatMessage)
            LogLevel.INFO -> Timber.tag(TAG).i(logcatMessage)
            LogLevel.WARNING -> Timber.tag(TAG).w(logcatMessage)
            LogLevel.ERROR -> Timber.tag(TAG).e(logcatMessage)
            LogLevel.SUCCESS -> Timber.tag(TAG).i(logcatMessage)
        }

        bridgeClient?.let { bridge ->
            try {
                bridge.getService()?.writeRawLog(conciseMessage)
            } catch (e: Exception) {
                Timber.tag(TAG).e("Failed to send log to bridge service: ${e.message}")
            }
        }
    }

    private fun buildLogcatMessage(source: LogSource, subName: String?, message: String, isSuccess: Boolean): String {
        val sourceStr = source.toString().lowercase().replaceFirstChar { it.uppercase() }

        val prefix = when {
            subName != null -> "$sourceStr: ${hookPrefixes[subName] ?: subName}"
            else -> sourceStr
        }

        return "$prefix: $message"
    }

    fun writeRaw(content: String) {
        bridgeClient?.let { bridge ->
            try {
                bridge.getService()?.writeRawLog(content)
            } catch (e: Exception) {
                Timber.tag(TAG).e("Failed to write raw log to bridge service: ${e.message}")
            }
        }
    }

    fun clearLogs() {
        bridgeClient?.let { bridge ->
            try {
                bridge.getService()?.clearLogs()
            } catch (e: Exception) {
                Timber.tag(TAG).e("Failed to clear logs in bridge service: ${e.message}")
            }
        }
    }

    private fun getDefaultSource(): LogSource =
        if (isModuleContext) LogSource.MODULE else LogSource.MANAGER

    private val debugEnabled: Boolean
        get() = when (val value = Config.get("debug_mode", false)) {
            is Boolean -> value // We account for both strings and booleans
            is String -> value.equals("true", ignoreCase = true)
            else -> false
        } || BuildConfig.DEBUG // Always true in debug builds
}

fun Hook.logd(message: String) = Logger.d(message, LogSource.HOOK, this.hookName)
fun Hook.logi(message: String) = Logger.i(message, LogSource.HOOK, this.hookName)
fun Hook.logw(message: String) = Logger.w(message, LogSource.HOOK, this.hookName)
fun Hook.loge(message: String) = Logger.e(message, LogSource.HOOK, this.hookName)
fun Hook.logs(message: String) = Logger.s(message, LogSource.HOOK, this.hookName)

fun Task.logd(message: String) = Logger.d(message, LogSource.TASK, this.id)
fun Task.logi(message: String) = Logger.i(message, LogSource.TASK, this.id)
fun Task.logw(message: String) = Logger.w(message, LogSource.TASK, this.id)
fun Task.loge(message: String) = Logger.e(message, LogSource.TASK, this.id)
fun Task.logs(message: String) = Logger.s(message, LogSource.TASK, this.id)