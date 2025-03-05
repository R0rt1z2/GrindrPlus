package com.grindrplus.utils

import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import java.lang.reflect.Member
import java.lang.reflect.Method

enum class HookStage {
    BEFORE,
    AFTER
}

object Hooker {
    inline fun <T> newMethodHook(
        stage: HookStage,
        crossinline consumer: (HookAdapter<T>) -> Unit,
        crossinline filter: ((HookAdapter<T>) -> Boolean) = { true }
    ): XC_MethodHook {
        return when (stage) {
            HookStage.BEFORE -> object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam<*>) {
                    HookAdapter<T>(param).takeIf(filter)?.also(consumer)
                }
            }
            HookStage.AFTER -> object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam<*>) {
                    HookAdapter<T>(param).takeIf(filter)?.also(consumer)
                }
            }
        }
    }

    inline fun <T> hook(
        clazz: Class<T>,
        methodName: String,
        stage: HookStage,
        crossinline filter: (HookAdapter<T>) -> Boolean,
        noinline consumer: (HookAdapter<T>) -> Unit
    ): Set<XC_MethodHook.Unhook> = XposedBridge.hookAllMethods(clazz, methodName, newMethodHook(stage, consumer, filter))

    inline fun <T> hook(
        member: Member,
        stage: HookStage,
        crossinline filter: ((HookAdapter<T>) -> Boolean),
        crossinline consumer: (HookAdapter<T>) -> Unit
    ): XC_MethodHook.Unhook {
        return XposedBridge.hookMethod(member, newMethodHook(stage, consumer, filter))
    }

    fun <T> hook(
        clazz: Class<T>,
        methodName: String,
        stage: HookStage,
        consumer: (HookAdapter<T>) -> Unit
    ): Set<XC_MethodHook.Unhook> = hook(clazz, methodName, stage, { true }, consumer)

    fun <T> hook(
        member: Member,
        stage: HookStage,
        consumer: (HookAdapter<T>) -> Unit
    ): XC_MethodHook.Unhook {
        return hook(member, stage, { true }, consumer)
    }

    fun <T> hookConstructor(
        clazz: Class<T>,
        stage: HookStage,
        consumer: (HookAdapter<T>) -> Unit
    ): Set<XC_MethodHook.Unhook> = XposedBridge.hookAllConstructors(clazz, newMethodHook(stage, consumer))

    fun <T> hookConstructor(
        clazz: Class<T>,
        stage: HookStage,
        filter: ((HookAdapter<T>) -> Boolean),
        consumer: (HookAdapter<T>) -> Unit
    ) {
        XposedBridge.hookAllConstructors(clazz, newMethodHook(stage, consumer, filter))
    }

    inline fun <T> hookObjectMethod(
        clazz: Class<T>,
        instance: Any,
        methodName: String,
        stage: HookStage,
        crossinline hookConsumer: (HookAdapter<T>) -> Unit
    ): List<() -> Unit> {
        val unhooks = mutableSetOf<XC_MethodHook.Unhook>()
        hook(clazz, methodName, stage) { param->
            if (param.nullableThisObject().let {
                    if (it == null) unhooks.forEach { u -> u.unhook() }
                    it != instance
                }) return@hook
            hookConsumer(param)
        }.also { unhooks.addAll(it) }
        return unhooks.map {
            { it.unhook() }
        }
    }

    inline fun <T> ephemeralHook(
        clazz: Class<T>,
        methodName: String,
        stage: HookStage,
        crossinline hookConsumer: (HookAdapter<T>) -> Unit
    ) {
        val unhooks: MutableSet<XC_MethodHook.Unhook> = HashSet()
        hook(clazz, methodName, stage) { param->
            hookConsumer(param)
            unhooks.forEach{ it.unhook() }
        }.also { unhooks.addAll(it) }
    }

    inline fun <T> ephemeralHookObjectMethod(
        clazz: Class<T>,
        instance: Any,
        methodName: String,
        stage: HookStage,
        crossinline hookConsumer: (HookAdapter<T>) -> Unit
    ) {
        val unhooks: MutableSet<XC_MethodHook.Unhook> = HashSet()
        hook(clazz, methodName, stage) { param->
            if (param.nullableThisObject() != instance) return@hook
            unhooks.forEach { it.unhook() }
            hookConsumer(param)
        }.also { unhooks.addAll(it) }
    }

    inline fun <T> ephemeralHookConstructor(
        clazz: Class<T>,
        stage: HookStage,
        crossinline hookConsumer: (HookAdapter<T>) -> Unit
    ) {
        val unhooks: MutableSet<XC_MethodHook.Unhook> = HashSet()
        hookConstructor(clazz, stage) { param->
            hookConsumer(param)
            unhooks.forEach{ it.unhook() }
        }.also { unhooks.addAll(it) }
    }
}

fun <T> Class<T>.hookConstructor(
    stage: HookStage,
    consumer: (HookAdapter<T>) -> Unit
) = Hooker.hookConstructor(this, stage, consumer)

fun <T> Class<T>.hookConstructor(
    stage: HookStage,
    filter: ((HookAdapter<T>) -> Boolean),
    consumer: (HookAdapter<T>) -> Unit
) = Hooker.hookConstructor(this, stage, filter, consumer)

fun <T> Class<T>.hook(
    methodName: String,
    stage: HookStage,
    consumer: (HookAdapter<T>) -> Unit
): Set<XC_MethodHook.Unhook> = Hooker.hook(this, methodName, stage, consumer)

fun <T> Class<T>.hook(
    methodName: String,
    stage: HookStage,
    filter: (HookAdapter<T>) -> Boolean,
    consumer: (HookAdapter<T>) -> Unit
): Set<XC_MethodHook.Unhook> = Hooker.hook(this, methodName, stage, filter, consumer)

fun Member.hook(
    stage: HookStage,
    consumer: (HookAdapter<Any>) -> Unit
): XC_MethodHook.Unhook = Hooker.hook(this, stage, consumer)

fun Member.hook(
    stage: HookStage,
    filter: ((HookAdapter<Any>) -> Boolean),
    consumer: (HookAdapter<Any>) -> Unit
): XC_MethodHook.Unhook = Hooker.hook(this, stage, filter, consumer)

fun Array<Method>.hookAll(stage: HookStage, param: (HookAdapter<Any>) -> Unit) {
    filter { it.declaringClass != Object::class.java }.forEach {
        it.hook(stage, param)
    }
}