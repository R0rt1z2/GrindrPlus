package com.grindrplus.utils

import com.grindrplus.GrindrPlus
import de.robv.android.xposed.XposedHelpers.callMethod
import de.robv.android.xposed.XposedHelpers.getObjectField
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy

/**
 * Utilities for hooking and manipulating Retrofit services.
 * This object allows intercepting Retrofit service creation, inspecting methods,
 * and modifying return values of both synchronous and asynchronous (suspend) functions.
 */
object RetrofitUtils {
    // Obfuscated class names for Retrofit's Success/Fail result wrappers.
    const val FAIL_CLASS_NAME = "g10.a\$a" // search for '"Fail(failValue="'
    const val SUCCESS_CLASS_NAME = "g10.a\$b" // search for '"Success(successValue="'
    const val SUCCESS_VALUE_NAME = "a" // probably the only field in the success class
    const val FAIL_VALUE_NAME = "a" // probably the only field in the fail class
    const val RETROFIT_NAME = "retrofit2.Retrofit"
    
    val continuationInterface = GrindrPlus.loadClass("kotlin.coroutines.Continuation")

    /**
     * Finds a method annotated with @POST matching the given URL value.
     */
    fun findPOSTMethod(clazz: Class<*>, value: String): Method? {
        return clazz.declaredMethods.find { method ->
            method.annotations.any {
                it.annotationClass.java.name == "retrofit2.http.POST"
                        && callMethod(it, "value") == value
            }
        }
    }

    // Helper extension methods to check Retrofit annotations on methods.
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

    // Helper extension methods to check and extract values from Retrofit Result wrappers.
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

    /**
     * Creates a new instance of the Retrofit Success wrapper class.
     * Useful for returning successful mock responses.
     */
    fun createSuccess(value: Any): Any {
        val successClass = GrindrPlus.loadClass(SUCCESS_CLASS_NAME)
        return successClass.constructors.first().newInstance(value)
    }

    fun createServiceProxy(
        originalProxy: Any,
        serviceClass: Class<*>,
        blacklist: Array<String> = emptyArray()
    ): Any {
        val originalHandler = Proxy.getInvocationHandler(originalProxy)
        val successConstructor =
            GrindrPlus.loadClass(SUCCESS_CLASS_NAME).constructors.firstOrNull()

        return Proxy.newProxyInstance(
            originalProxy.javaClass.classLoader,
            arrayOf(serviceClass)
        ) { _, method, args ->
            if (successConstructor != null && (blacklist.isEmpty() || method.name in blacklist)) {
                successConstructor.newInstance(Unit)
            } else {
                originalHandler.invoke(originalProxy, method, args)
            }
        }
    }

    /**
     * Hooks Retrofit's `create` method to wrap the returned service instance.
     * 
     * Retrofit.create() returns a Proxy implementation of the API interface.
     * We intercept this and wrap it in *another* Proxy (our wrapper).
     * This allows us to intercept every method call made to the API service.
     *
     * @param serviceClass The interface class of the service we want to hook (e.g., AlbumService::class.java).
     * @param invoke A lambda that acts as the InvocationHandler for our wrapper proxy.
     */
    fun hookService(
        serviceClass: Class<*>,
        invoke: (originalHandler: InvocationHandler, originalProxy: Any, method: Method, args: Array<Any?>) -> Any?
    ) {
        GrindrPlus.loadClass(RETROFIT_NAME)
            .hook("create", HookStage.AFTER) { param ->
                val originalProxy = param.getResult()
                if (originalProxy != null && serviceClass.isAssignableFrom(originalProxy.javaClass)) {
                    val originalHandler = Proxy.getInvocationHandler(originalProxy)

                    // create a proxy for the Retrofit service
                    val serviceInstanceProxy = Proxy.newProxyInstance(
                        originalProxy.javaClass.classLoader,
                        arrayOf(serviceClass)
                    ) { _, method, args ->
                        invoke(originalHandler, originalProxy, method, args)
                    }

                    // Return our wrapper proxy to the app, so all calls go through us.
                    param.setResult(serviceInstanceProxy)
                }
            }
    }

    /**
     * Helper to wrap a Continuation using Proxy.
     * This allows for intercepting and modifying the result of a suspend function
     * before it resumes the original caller.
     */
    fun wrapContinuation(
        originalContinuation: Any,
        resultMapper: (result: Any) -> Any
    ): Any {
        if (!continuationInterface.isAssignableFrom(originalContinuation.javaClass))
            throw Exception("Provided object does not implement Continuation interface")

        return Proxy.newProxyInstance(
            originalContinuation.javaClass.classLoader,
            arrayOf(continuationInterface)
        ) { _, method, args ->
            if (method.name == "resumeWith") {
                val result = args[0]

                 // Retrofit's suspend support returns the value directly (or throws).
                 // args[0] is the result value (Success wrapper or Failure exception)

                 val newResult = resultMapper(result)
                 method.invoke(originalContinuation, newResult)
            } else if (method.name == "getContext") {
                 method.invoke(originalContinuation)
            } else {
                 method.invoke(originalContinuation, *args)
            }
        }
    }

    /**
     * Intercepts a method call, executes it, and allows modifying the result.
     * Handles both standard synchronous methods and Kotlin `suspend` functions.
     * 
     * @param resultMapper A function that takes the original result and returns a modified result.
     */
    fun invokeAndReplaceResult(
        originalHandler: InvocationHandler,
        originalProxy: Any,
        method: Method,
        args: Array<Any?>,
        resultMapper: (result: Any) -> Any
    ): Any? {
        // Kotlin suspend functions are compiled to static methods (or member methods) 
        // that take a Continuation as the *last* argument.
        val isSuspendFun =
            args.isNotEmpty() && continuationInterface.isAssignableFrom(args.last()!!.javaClass)

        if (!isSuspendFun) {
            // Synchronous call: Invoke and modify result immediately.
            val result = originalHandler.invoke(originalProxy, method, args)
            return resultMapper(result as Any)
        }

        // Asynchronous (Suspend) call:
        // 1. Wrap the Continuation logic to intercept the callback.
        val newContinuation = wrapContinuation(args.last()!!, resultMapper)

        // 2. Replace the original Continuation in the arguments with our wrapper.
        val newArgs = args.clone()
        newArgs[newArgs.lastIndex] = newContinuation

        // 3. Invoke the original method with the wrapped continuation.
        // The method will return COROUTINE_SUSPENDED (usually), and eventually call 
        // newContinuation.resumeWith(result), which triggers our mapper.
        return originalHandler.invoke(originalProxy, method, newArgs)
    }

}