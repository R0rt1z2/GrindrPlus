package com.grindrplus.hooks

import com.grindrplus.BuildConfig
import com.grindrplus.utils.Hook
import com.grindrplus.utils.HookStage
import com.grindrplus.utils.hook
import de.robv.android.xposed.XposedBridge

class RequestLogger: Hook(
    "Request logger",
    "Log Grindr's requests to Logcat, PS: Must have debug apk to work"
) {
    override fun init() {
        if (!BuildConfig.DEBUG) return

        findClass("Z5.a\$a").hook("onResponse", HookStage.BEFORE) { param ->
            XposedBridge.log(buildString {
                append("New request came!")
                append("")
                param.args().filterNotNull().forEach {
                    when (it.javaClass.simpleName) {
                        "Response" -> {
                            append("Response: " + it.javaClass.getMethod("body").invoke(it))
                        }

                        else -> {
                            append("Request: " + it.javaClass.getMethod("request").invoke(it))
                        }
                    }
                }
            })
        }
    }
}