package com.grindrplus.utils

import com.grindrplus.GrindrPlus
import de.robv.android.xposed.XposedHelpers.callMethod
import de.robv.android.xposed.XposedHelpers.getObjectField
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy

object RetrofitUtils {
    const val FAIL_CLASS_NAME = "g10.a\$a" // search for '"Fail(failValue="'
    const val SUCCESS_CLASS_NAME = "g10.a\$b" // search for '"Success(successValue="'
    const val SUCCESS_VALUE_NAME = "a" // probably the only field in the success class
    const val FAIL_VALUE_NAME = "a" // probably the only field in the fail class
    const val RETROFIT_NAME = "retrofit2.Retrofit"

    fun findPOSTMethod(clazz: Class<*>, value: String): Method? {
        return clazz.declaredMethods.find { method ->
            method.annotations.any {
                it.annotationClass.java.name == "retrofit2.http.POST"
                        && callMethod(it, "value") == value
            }
        }
    }

    fun Method.isPOST(value: String): Boolean {
        return this.annotations.any {
            it.annotationClass.java.name == "retrofit2.http.POST"
                    && callMethod(it, "value") == value
        }
    }

    fun Method.isDELETE(value: String): Boolean {
        return this.annotations.any {
            it.annotationClass.java.name == "retrofit2.http.DELETE"
                    && callMethod(it, "value") == value
        }
    }

    fun Method.isGET(value: String): Boolean {
        return this.annotations.any {
            it.annotationClass.java.name == "retrofit2.http.GET"
                    && callMethod(it, "value") == value
        }
    }

    fun Method.isPUT(value: String): Boolean {
        return this.annotations.any {
            it.annotationClass.java.name == "retrofit2.http.PUT"
                    && callMethod(it, "value") == value
        }
    }

    fun Any.isFail(): Boolean {
        return javaClass.name == FAIL_CLASS_NAME
    }

    fun Any.isSuccess(): Boolean {
        return javaClass.name == SUCCESS_CLASS_NAME
    }

    fun Any.isResult(): Boolean {
        return isSuccess() || isFail()
    }

    fun Any.getSuccessValue(): Any {
        return getObjectField(this, SUCCESS_VALUE_NAME)
    }

    fun Any.getFailValue(): Any {
        return getObjectField(this, FAIL_VALUE_NAME)
    }

    fun createSuccess(value: Any): Any {
        val successClass = GrindrPlus.loadClass(SUCCESS_CLASS_NAME)
        return successClass.constructors.first().newInstance(value)
    }

    fun createServiceProxy(
        originalService: Any,
        serviceClass: Class<*>,
        blacklist: Array<String> = emptyArray()
    ): Any {
        val invocationHandler = Proxy.getInvocationHandler(originalService)
        val successConstructor =
            GrindrPlus.loadClass(SUCCESS_CLASS_NAME).constructors.firstOrNull()

        return Proxy.newProxyInstance(
            originalService.javaClass.classLoader,
            arrayOf(serviceClass)
        ) { proxy, method, args ->
            if (successConstructor != null && (blacklist.isEmpty() || method.name in blacklist)) {
                successConstructor.newInstance(Unit)
            } else {
                invocationHandler.invoke(proxy, method, args)
            }
        }
    }

    /**
     * hook Retrofit's create method, which is used to create instances of "API services"
     * we take the original create result and replace it with our own proxy.
     * The proxy then calls our own method to intercept and modify the request and the result
     */
    fun hookService(
        serviceClass: Class<*>,
        invoke: (originalHandler: InvocationHandler, proxy: Any, method: Method, args: Array<Any?>) -> Any?
    ) {
        GrindrPlus.loadClass(RETROFIT_NAME)
            .hook("create", HookStage.AFTER) { param ->
                val serviceInstance = param.getResult()
                if (serviceInstance != null && serviceClass.isAssignableFrom(serviceInstance.javaClass)) {
                    val invocationHandler = Proxy.getInvocationHandler(serviceInstance)

                    // create a proxy for the Retrofit service
                    val serviceInstanceProxy = Proxy.newProxyInstance(
                        serviceInstance.javaClass.classLoader,
                        arrayOf(serviceClass)
                    ) { proxy, method, args ->
                        invoke(invocationHandler, proxy, method, args)
                    }

                    // return our proxy instead of the original service
                    param.setResult(serviceInstanceProxy)
                }
            }
    }

    /**
     * Kotlin uspend funcs are compiled in java to continuation "objects". The return value
     * is not simply returned to the calling method, but is provided by calling it's resumeWith
     * something very similar to a callback. Therefore we try to replace this callback with our own,
     * which will to get the return value first, possibly modify it and then pass it to the original
     * caller by passing the new value to the original "callback"
     */
    val continuationInterface = GrindrPlus.loadClass("kotlin.coroutines.Continuation")
    fun wrapContinuation(
        originalContinuation: Any,
        resultMapper: (result: Any) -> Any
    ): Any {
        if (!continuationInterface.isAssignableFrom(originalContinuation.javaClass))
            throw Exception("Provided object does not implement Continuation interface")

        return Proxy.newProxyInstance(
            originalContinuation.javaClass.classLoader,
            arrayOf(continuationInterface)
        ) { proxy, method, args ->
            // only intercept resumeWith method
            if (method.name == "resumeWith") {
                val result = args!![0]
                val newResult = resultMapper(result)
                method.invoke(originalContinuation, newResult)

            } else if (method.parameterCount == 0)
                return@newProxyInstance method.invoke(originalContinuation)
            else
                return@newProxyInstance method.invoke(originalContinuation, args)
        }
    }

    fun invokeAndReplaceResult(
        originalHandler: InvocationHandler,
        proxy: Any,
        method: Method,
        args: Array<Any?>,
        resultMapper: (result: Any) -> Any
    ): Any? {
        // suspend fun has Continuation as last argument
        val isSuspendFun = !args.isEmpty() || continuationInterface.isAssignableFrom(args.last()!!.javaClass)

        if (!isSuspendFun) {
            val result = originalHandler.invoke(proxy, method, args)
            return resultMapper.invoke(result)
        }

        val newContinuation = wrapContinuation(args.last()!!, resultMapper)

        val newArgs = args.clone()
        newArgs[newArgs.lastIndex] = newContinuation

        return originalHandler.invoke(proxy, method, newArgs)
    }

}