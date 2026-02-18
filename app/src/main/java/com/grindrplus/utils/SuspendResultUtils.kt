package com.grindrplus.utils

import de.robv.android.xposed.XC_MethodHook

fun withSuspendResult(args: Array<Any?>, result: Any, onResult: (Array<Any?>, Any) -> Any): Any {
    return if (result.toString() == "COROUTINE_SUSPENDED") {
        val continuation = args.last()!!
        var unhook: Set<XC_MethodHook.Unhook>? = null
        unhook = continuation.javaClass.hook("invokeSuspend", HookStage.BEFORE) {
            if (it.thisObject() !== continuation) return@hook // skip other instances
            unhook?.forEach(XC_MethodHook.Unhook::unhook)
            unhook = null
            it.setArg(0, onResult(args, it.arg(0)))
        }
        result
    } else {
        onResult(args, result)
    }
}