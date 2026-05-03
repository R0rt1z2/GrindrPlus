package com.grindrplus.manager.utils

import android.content.Context
import android.widget.Toast
import com.grindrplus.core.LogSource
import com.grindrplus.core.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

// Errors are routed through Logger → BridgeService so they appear in the unified grindrplus.log.
// BridgeService already handles log rotation; no separate file management needed here.
object ErrorHandler {
    private const val TAG = "ErrorHandler"

    fun logError(context: Context, tag: String, message: String, error: Throwable?) {
        Timber.tag(tag).e(error, message)
        Logger.e("[$tag] $message", LogSource.MANAGER)
        if (error != null) {
            Logger.writeRaw(error.stackTraceToString())
        }
    }

    fun showToast(context: Context, message: String, length: Int = Toast.LENGTH_SHORT) {
        CoroutineScope(Dispatchers.Main).launch {
            Toast.makeText(context, message, length).show()
        }
    }
}