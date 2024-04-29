package com.grindrplus.hooks

import android.view.Window
import android.view.WindowManager.LayoutParams.FLAG_SECURE
import com.grindrplus.utils.Hook
import com.grindrplus.utils.HookStage
import com.grindrplus.utils.hook

class AllowScreenshots : Hook(
    "Allow screenshots",
    "Allow screenshots everywhere in the app"
) {

    override fun init() {
        Window::class.java.hook("setFlags", HookStage.BEFORE) { param ->
            var flags = param.arg<Int>(0)
            flags = flags and FLAG_SECURE.inv()
            param.setArg(0, flags)
        }
    }
}