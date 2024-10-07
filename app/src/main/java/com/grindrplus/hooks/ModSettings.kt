package com.grindrplus.hooks

import android.widget.TextView
import android.widget.Toast
import com.grindrplus.BuildConfig
import com.grindrplus.GrindrPlus
import com.grindrplus.utils.Hook
import com.grindrplus.utils.HookStage
import com.grindrplus.utils.hook

class ModSettings : Hook(
    "Mod settings",
    "GrindrPlus settings"
) {
    private val settingsActivity = "com.grindrapp.android.ui.settings.SettingsActivity"

    override fun init() {
        findClass(settingsActivity)
            .hook("onCreate", HookStage.AFTER) { param ->
                var settingsRoot =
                    param.thisObject()::class.java.getMethod("E").invoke(param.thisObject())
                settingsRoot::class.java.declaredFields.reversed().forEach { field ->
                    field.isAccessible = true
                    val fieldValue = field.get(settingsRoot)

                    if (fieldValue is TextView && fieldValue.text.isEmpty()) {
                        fieldValue.setOnClickListener {
                            GrindrPlus.showToast(
                                Toast.LENGTH_LONG,
                                "GrindrPlus v${BuildConfig.VERSION_NAME}"
                            )
                        }
                        return@forEach
                    }
                }
            }
    }
}