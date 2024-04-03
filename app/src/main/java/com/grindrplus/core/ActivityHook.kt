package com.grindrplus.core

import android.annotation.SuppressLint
import android.app.Activity
import de.robv.android.xposed.XC_MethodHook

class ActivityHook : XC_MethodHook() {
    override fun afterHookedMethod(param: MethodHookParam) {
        val resultActivity = param.result as? Activity ?: return
        val excludedActivities = setOf("ChatRoomPhotosActivity", "ProfilesActivity")

        if (_currentActivity?.javaClass?.simpleName == "ChatActivityV2" &&
            resultActivity.javaClass.simpleName in excludedActivities)
            return

        _currentActivity = resultActivity
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