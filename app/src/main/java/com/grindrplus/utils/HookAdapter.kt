package com.grindrplus.utils

import android.os.Build
import androidx.annotation.RequiresApi
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import java.lang.reflect.Member
import java.util.function.Consumer

@Suppress("UNCHECKED_CAST")
class HookAdapter<Clazz>(
    private val methodHookParam: XC_MethodHook.MethodHookParam<*>
) {
    fun thisObject(): Clazz {
        return methodHookParam.thisObject as Clazz
    }

    fun nullableThisObject(): Clazz? {
        return methodHookParam.thisObject as Clazz?
    }

    fun method(): Member {
        return methodHookParam.method
    }

    fun <T : Any> arg(index: Int): T {
        return methodHookParam.args[index] as T
    }

    fun <T : Any> argNullable(index: Int): T? {
        return methodHookParam.args.getOrNull(index) as T?
    }

    fun setArg(index: Int, value: Any?) {
        if (index < 0 || index >= methodHookParam.args.size) return
        methodHookParam.args[index] = value
    }

    fun args(): Array<Any?> {
        return methodHookParam.args
    }

    fun getResult(): Any? {
        return methodHookParam.result
    }

    fun setResult(result: Any?) {
        methodHookParam.result = result
    }

    fun setThrowable(throwable: Throwable) {
        methodHookParam.throwable = throwable
    }

    fun throwable(): Throwable? {
        return methodHookParam.throwable
    }

    fun invokeOriginal(): Any? {
        return XposedBridge.invokeOriginalMethod(method(), thisObject(), args())
    }

    fun invokeOriginal(args: Array<Any?>): Any? {
        return XposedBridge.invokeOriginalMethod(method(), thisObject(), args)
    }

    @RequiresApi(Build.VERSION_CODES.N)
    fun invokeOriginalSafe(errorCallback: Consumer<Throwable>) {
        invokeOriginalSafe(args(), errorCallback)
    }

    @RequiresApi(Build.VERSION_CODES.N)
    fun invokeOriginalSafe(args: Array<Any?>, errorCallback: Consumer<Throwable>) {
        runCatching {
            setResult(XposedBridge.invokeOriginalMethod(method(), thisObject(), args))
        }.onFailure {
            errorCallback.accept(it)
        }
    }
}