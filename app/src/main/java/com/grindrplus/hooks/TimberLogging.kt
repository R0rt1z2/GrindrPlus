package com.grindrplus.hooks

import com.grindrplus.GrindrPlus
import com.grindrplus.utils.Hook
import com.grindrplus.utils.HookStage
import com.grindrplus.utils.hook
import com.grindrplus.utils.hookConstructor
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers.getObjectField
import de.robv.android.xposed.XposedHelpers.setObjectField
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import kotlin.math.log


class TimberLogging : Hook(
    "Timber Logging",
    "Hook into Timber logging"
) {
    private val appConfiguration = "com.grindrapp.android.platform.config.AppConfiguration"

    // To find this, look for: "Timber.INSTANCE.plant(new ?());"
    // There is only 1 method that contains a call to plant()
    private val logClass = "D6.g"

    // To find this, look at the condition referencing AppConfiguration 1 line before the call to plant()
    private val appConfigurationDebugField = "d"

    override fun init() {

        findClass(appConfiguration).hookConstructor(HookStage.AFTER) { param ->

            // Store the original value
            val originalVal = getObjectField(param.thisObject(), appConfigurationDebugField)

            // Override the value
            setObjectField(param.thisObject(), appConfigurationDebugField, true)

            // Create a new logger & initialize it. Since we overwrote the debug field, the init will proceed
            val loggerClass = findClass(logClass);
            val loggerInstance = loggerClass.constructors.first().newInstance(param.thisObject())
            loggerClass.getMethod("init").invoke(loggerInstance)

            // Restore the original value
            setObjectField(param.thisObject(), appConfigurationDebugField, originalVal)
        }

    }

}