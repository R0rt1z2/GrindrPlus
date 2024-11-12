package com.grindrplus.core

import com.grindrplus.utils.HookStage
import com.grindrplus.utils.hookConstructor
import de.robv.android.xposed.XposedHelpers.findClass

class InstanceManager(private val classLoader: ClassLoader) {
    private val instances = mutableMapOf<String, Any>()

    fun hookClassConstructors(vararg classNames: String) {
        classNames.forEach { className ->
            val clazz = findClass(className, classLoader)
            clazz.hookConstructor(HookStage.AFTER) { param ->
                instances[className] = param.thisObject()
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> getInstance(className: String): T? {
        return instances[className] as? T
    }
}