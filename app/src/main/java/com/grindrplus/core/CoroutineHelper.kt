package com.grindrplus.core

import com.grindrplus.GrindrPlus
import de.robv.android.xposed.XposedHelpers
import java.lang.reflect.Proxy

object CoroutineHelper {

    private val BuildersKt = XposedHelpers.findClass(
        "kotlinx.coroutines.BuildersKt",
        GrindrPlus.classLoader
    )
    private val Function2 = XposedHelpers.findClass(
        "kotlin.jvm.functions.Function2",
        GrindrPlus.classLoader
    )
    private val EmptyCoroutineContextInstance = let {
        val emptyCoroutineContext = XposedHelpers.findClass(
            "kotlin.coroutines.EmptyCoroutineContext",
            GrindrPlus.classLoader
        )
        XposedHelpers.getStaticObjectField(emptyCoroutineContext, "INSTANCE")
    }

    fun callSuspendFunction(function: (continuation: Any) -> Any?): Any {
        val proxy = Proxy.newProxyInstance(
            GrindrPlus.classLoader,
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