package com.grindrplus.utils

import com.grindrplus.core.LogSource
import com.grindrplus.core.Logger
import de.robv.android.xposed.XC_MethodHook

fun withSuspendResult(args: Array<Any?>, result: Any, onResult: (Array<Any?>, Any) -> Any): Any {
    return if (result.toString() == "COROUTINE_SUSPENDED") {
        val continuation = args.last()!!
        var unhook: Set<XC_MethodHook.Unhook>? = null
        unhook = continuation.javaClass.hook("invokeSuspend", HookStage.BEFORE) {
            if (it.thisObject() !== continuation) return@hook
            val incomingResult: Any? = it.arg(0)

            // If the coroutine was cancelled (Result.Failure wrapping a CancellationException),
            // unhook and let the cancellation propagate without calling onResult.
            val cancellationException = extractCancellationException(incomingResult)
            if (cancellationException != null) {
                unhook?.forEach(XC_MethodHook.Unhook::unhook)
                unhook = null
                return@hook
            }

            unhook?.forEach(XC_MethodHook.Unhook::unhook)
            unhook = null
            if (incomingResult == null) return@hook
            try {
                it.setArg(0, onResult(args, incomingResult))
            } catch (e: Exception) {
                Logger.e("SuspendResultUtils: onResult threw: ${e.message}", LogSource.MODULE)
            }
        }
        result
    } else {
        onResult(args, result)
    }
}

// kotlin.Result at runtime is boxed as kotlin.Result$Failure for failures.
private fun extractCancellationException(result: Any?): java.util.concurrent.CancellationException? {
    result ?: return null
    return runCatching {
        if (result.javaClass.name == "kotlin.Result\$Failure") {
            result.javaClass.getField("exception").get(result) as? java.util.concurrent.CancellationException
        } else null
    }.getOrNull()
}