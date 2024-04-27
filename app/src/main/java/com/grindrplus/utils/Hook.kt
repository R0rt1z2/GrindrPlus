package com.grindrplus.utils

import com.grindrplus.GrindrPlus
import com.grindrplus.core.Config

abstract class Hook(
    val hookName : String,
    val hookDesc : String = "",
) {
    /**
     * Hook specific initialization.
     */
    open fun init() {}

    /**
     * Hook specific cleanup.
     */
    open fun cleanup() {}

    protected fun isHookEnabled(): Boolean {
        return Config.isHookEnabled(hookName)
    }

    protected fun findClass(name: String): Class<*>? {
        return try {
            GrindrPlus.loadClass(name)
        } catch (e: ClassNotFoundException) {
            GrindrPlus.logger.log("Failed to find class: $name")
            null
        }
    }

    protected fun loadClass(name: String): Class<*>? {
        return try {
            GrindrPlus.classLoader.loadClass(name)
        } catch (e: ClassNotFoundException) {
            GrindrPlus.logger.log("Failed to load class: $name")
            null
        }
    }

    protected fun getResource(name: String, type: String): Int {
        return GrindrPlus.context.resources.getIdentifier(
            name, type, GrindrPlus.context.packageName)
    }
}