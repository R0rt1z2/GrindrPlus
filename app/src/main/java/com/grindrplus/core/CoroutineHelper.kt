package com.grindrplus.core

import de.robv.android.xposed.XposedHelpers
import java.lang.reflect.Proxy

class CoroutineHelper(
    private val classLoader: ClassLoader
) {
    private val buildersKt = XposedHelpers.findClass(
        "kotlinx.coroutines.BuildersKt",
        classLoader
    )

    private val function2 = XposedHelpers.findClass(
        "kotlin.jvm.functions.Function2",
        classLoader
    )

    private val emptyCoroutineContextInstance = let {
        val emptyCoroutineContext = XposedHelpers.findClass(
            "kotlin.coroutines.EmptyCoroutineContext",
            classLoader
        )
        XposedHelpers.getStaticObjectField(emptyCoroutineContext, "INSTANCE")
    }

    fun callSuspendFunction(function: (continuation: Any) -> Any?): Any {
        val proxy = Proxy.newProxyInstance(
            classLoader,
            arrayOf(function2)
        ) { _, _, args ->
            function(args[1])
        }
        return XposedHelpers.callStaticMethod(
            buildersKt,
            "runBlocking",
            emptyCoroutineContextInstance,
            proxy
        )
    }
}