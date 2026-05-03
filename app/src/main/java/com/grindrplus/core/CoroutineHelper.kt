package com.grindrplus.core

import com.grindrplus.GrindrPlus
import de.robv.android.xposed.XposedHelpers
import java.lang.reflect.Proxy

object CoroutineHelper {

    // lazy: defers class lookup until first use so a missing class throws at call-site
    // (object init failures produce ExceptionInInitializerError which is non-recoverable)
    private val BuildersKt: Class<*> by lazy {
        XposedHelpers.findClassIfExists("kotlinx.coroutines.BuildersKt", GrindrPlus.classLoader)
            ?: error("CoroutineHelper: kotlinx.coroutines.BuildersKt not found in classLoader")
    }
    private val Function2: Class<*> by lazy {
        XposedHelpers.findClassIfExists("kotlin.jvm.functions.Function2", GrindrPlus.classLoader)
            ?: error("CoroutineHelper: kotlin.jvm.functions.Function2 not found in classLoader")
    }
    private val EmptyCoroutineContextInstance: Any by lazy {
        val emptyCoroutineContext = XposedHelpers.findClassIfExists(
            "kotlin.coroutines.EmptyCoroutineContext",
            GrindrPlus.classLoader
        ) ?: error("CoroutineHelper: kotlin.coroutines.EmptyCoroutineContext not found in classLoader")
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