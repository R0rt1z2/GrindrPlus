package com.eljaviluki.grindrplus

import android.util.Log
import de.robv.android.xposed.XposedBridge

object Logger {
    const val TAG = "GrindrPlus"
    fun xLog(msg: String) {
        XposedBridge.log("$TAG: $msg")
    }

    fun log(msg: String) {
        Log.i(TAG, msg)
    }
}