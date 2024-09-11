package com.grindrplus.hooks

import com.grindrplus.utils.Hook
import com.grindrplus.utils.HookStage
import com.grindrplus.utils.hook

class UnlimitedTaps : Hook(
    "Unlimited taps",
    "Allow unlimited taps"
) {
    private val tapsAnimLayout = "com.grindrapp.android.view.TapsAnimLayout"

    override fun init() {
        val tapsAnimLayoutClass = findClass(tapsAnimLayout)

        tapsAnimLayoutClass.hook(
            "getCanSelectVariants", HookStage.BEFORE
        ) { param ->
            param.setResult(true)
        }

        tapsAnimLayoutClass.hook(
            "getDisableVariantSelection", HookStage.BEFORE
        ) { param ->
            param.setResult(false)
        }
    }
}