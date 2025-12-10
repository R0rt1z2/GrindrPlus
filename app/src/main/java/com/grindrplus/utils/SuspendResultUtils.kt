package com.grindrplus.utils

import de.robv.android.xposed.XC_MethodHook

fun withSuspendResult(args: Array<Any?>, result: Any, onResult: (Array<Any?>, Any) -> Any): Any {
    return if (result.toString() == "COROUTINE_SUSPENDED") {
        var unhook: Set<XC_MethodHook.Unhook>? = null
        unhook = args.last()!!.javaClass.hook("invokeSuspend", HookStage.BEFORE) {
            unhook?.forEach(XC_MethodHook.Unhook::unhook)
            unhook = null
            it.setArg(0, onResult(args, it.arg(0)))
        }
        result
    } else {
        onResult(args, result)
    }
}