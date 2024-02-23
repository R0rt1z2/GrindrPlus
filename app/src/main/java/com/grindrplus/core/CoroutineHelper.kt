package com.grindrplus

import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam
import java.lang.reflect.Proxy

class CoroutineHelper(
    private val lpparam: LoadPackageParam
) {

    private val BuildersKt = XposedHelpers.findClass(
        "kotlinx.coroutines.BuildersKt",
        lpparam.classLoader
    )
    private val Function2 = XposedHelpers.findClass(
        "kotlin.jvm.functions.Function2",
        lpparam.classLoader
    )
    private val EmptyCoroutineContextInstance = let {
        val EmptyCoroutineContext = XposedHelpers.findClass(
            "kotlin.coroutines.EmptyCoroutineContext",
            lpparam.classLoader
        )
        XposedHelpers.getStaticObjectField(EmptyCoroutineContext, "INSTANCE")
    }

    fun callSuspendFunction(function: (continuation: Any) -> Any?): Any {
        val proxy = Proxy.newProxyInstance(
            lpparam.classLoader,
            arrayOf(Function2)
        ) { _, _, args ->
            function(args[1])
        }
        return XposedHelpers.callStaticMethod(
            BuildersKt,
            "runBlocking",
            EmptyCoroutineContextInstance,
            proxy
        )
    }
}