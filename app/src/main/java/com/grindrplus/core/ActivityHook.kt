package com.grindrplus.core

import android.annotation.SuppressLint
import android.app.Activity
import de.robv.android.xposed.XC_MethodHook

class ActivityHook : XC_MethodHook() {
    override fun afterHookedMethod(param: MethodHookParam) {
        _currentActivity = param.result as Activity
    }

    companion object {
        @SuppressLint("StaticFieldLeak")
        @Volatile
        var _currentActivity: Activity? = null
            private set
    }

    fun getCurrentActivity(): Activity? {
        return _currentActivity as Activity
    }
}