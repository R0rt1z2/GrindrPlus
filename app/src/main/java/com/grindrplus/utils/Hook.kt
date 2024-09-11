package com.grindrplus.utils

import com.grindrplus.GrindrPlus
import com.grindrplus.core.Config

abstract class Hook(
    val hookName: String,
    val hookDesc: String = "",
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

    protected fun findClass(name: String): Class<*> {
        return GrindrPlus.loadClass(name)
    }

    protected fun getResource(name: String, type: String): Int {
        return GrindrPlus.context.resources.getIdentifier(
            name, type, GrindrPlus.context.packageName
        )
    }

    protected fun getAttribute(name: String): Int {
        return GrindrPlus.context.resources.getIdentifier(name, "attr"
            , GrindrPlus.context.packageName)
    }
}