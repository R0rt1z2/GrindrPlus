package com.grindrplus.utils

import com.grindrplus.GrindrPlus
import de.robv.android.xposed.XposedHelpers.callMethod
import de.robv.android.xposed.XposedHelpers.getObjectField
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy

object RetrofitUtils {
    const val FAIL_CLASS_NAME = "xb.a\$a"
    const val SUCCESS_CLASS_NAME = "xb.a\$b"
    const val SUCCESS_VALUE_NAME = "a"
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

    fun Any.getSuccessValue(): Any {
        return getObjectField(this, SUCCESS_VALUE_NAME)
    }

    fun Any.getFailValue(): Any {
        return getObjectField(this, SUCCESS_VALUE_NAME)
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

    fun hookService(
        serviceClass: Class<*>,
        invoke: (originalHandler: InvocationHandler, proxy: Any, method: Method, args: Array<Any?>) -> Any?
    ) {
        GrindrPlus.loadClass(RETROFIT_NAME)
            .hook("create", HookStage.AFTER) { param ->
                val serviceInstance = param.getResult()
                if (serviceInstance != null && serviceClass.isAssignableFrom(serviceInstance.javaClass)) {
                    val invocationHandler = Proxy.getInvocationHandler(serviceInstance)
                    param.setResult(Proxy.newProxyInstance(
                        serviceInstance.javaClass.classLoader,
                        arrayOf(serviceClass)
                    ) { proxy, method, args ->
                        invoke(invocationHandler, proxy, method, args)
                    })
                }
            }
    }
}