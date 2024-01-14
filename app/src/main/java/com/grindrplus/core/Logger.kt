package com.grindrplus.core

import android.util.Log
import de.robv.android.xposed.XposedBridge

object Logger {
    private const val TAG = "GrindrPlus"

    fun xLog(msg: String) = XposedBridge.log("$TAG: $msg")
    fun log(msg: String) = Log.i(TAG, msg)
}