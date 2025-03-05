package com.grindrplus.hooks

import com.grindrplus.BuildConfig
import com.grindrplus.utils.Hook
import com.grindrplus.utils.HookStage
import com.grindrplus.utils.hookConstructor
import de.robv.android.xposed.XposedHelpers.getObjectField
import de.robv.android.xposed.XposedHelpers.setObjectField

class TimberLogging : Hook(
    "Timber Logging",
    "Hook into Timber logging"
) {
    private val appConfiguration = "com.grindrapp.android.platform.config.AppConfiguration"

    // To find this, look for: "Timber.INSTANCE.plant(new ?());"
    // There is only 1 method that contains a call to plant()
    private val logClass = "P6.g"

    // To find this, look at the condition referencing AppConfiguration 1 line before the call to plant()
    private val appConfigurationDebugField = "d"

    override fun init() {
        if (!BuildConfig.DEBUG) {
            return
        }

        findClass(appConfiguration).hookConstructor(HookStage.AFTER) { param ->

            // Store the original value
            val originalVal = getObjectField(param.thisObject(), appConfigurationDebugField)

            // Override the value
            setObjectField(param.thisObject(), appConfigurationDebugField, true)

            // Create a new logger & initialize it.
            val loggerClass = findClass(logClass);
            val loggerInstance = loggerClass.constructors.first().newInstance(param.thisObject())
            loggerClass.getMethod("init").invoke(loggerInstance)

            // Restore the original value
            setObjectField(param.thisObject(), appConfigurationDebugField, originalVal)
        }
    }

}