package com.grindrplus.utils

import android.os.Build
import androidx.annotation.RequiresApi
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import java.lang.reflect.Member
import java.util.function.Consumer

@Suppress("UNCHECKED_CAST")
class HookAdapter<T>(
    private val methodHookParam: XC_MethodHook.MethodHookParam
) {
    val method: Member
        get() = methodHookParam.method

    val thisObject: T
        get() = methodHookParam.thisObject as T

    val nullableThisObject: T?
        get() = methodHookParam.thisObject as T?

    val args: Array<Any?>
        get() = methodHookParam.args

    var result: Any?
        get() = methodHookParam.result
        set(value) {
            methodHookParam.result = value
        }

    var throwable: Throwable?
        get() = methodHookParam.throwable
        set(value) {
            methodHookParam.throwable = value
        }

    fun <U : Any> arg(index: Int): U {
        return methodHookParam.args[index] as U
    }

    fun <U : Any> argNullable(index: Int): U? {
        return methodHookParam.args.getOrNull(index) as U?
    }

    fun setArg(index: Int, value: Any?) {
        if (index < 0 || index >= methodHookParam.args.size) return
        methodHookParam.args[index] = value
    }

    fun invokeOriginal(): Any? {
        return XposedBridge.invokeOriginalMethod(method, thisObject, args)
    }

    fun invokeOriginal(args: Array<Any?>): Any? {
        return XposedBridge.invokeOriginalMethod(method, thisObject, args)
    }

    @RequiresApi(Build.VERSION_CODES.N)
    fun invokeOriginalSafe(errorCallback: Consumer<Throwable>) {
        invokeOriginalSafe(args, errorCallback)
    }

    @RequiresApi(Build.VERSION_CODES.N)
    fun invokeOriginalSafe(args: Array<Any?>, errorCallback: Consumer<Throwable>) {
        runCatching {
            result = XposedBridge.invokeOriginalMethod(method, thisObject, args)
        }.onFailure {
            errorCallback.accept(it)
        }
    }
}