package com.grindrplus.core

import android.util.Log
import de.robv.android.xposed.XposedBridge
import java.io.File

class Logger(logFile: String) {
    private val LOG_FILE = File(logFile)
    private val LOG_TAG = "GrindrPlus"
    private val MAX_LOG_SIZE = 1024 * 1024 * 5 // 5 MB

    init {
        try {
            LOG_FILE.createNewFile()
        } catch (e: Exception) {
            Log.wtf(LOG_TAG, "Failed to create log file ($logFile): ${e.message}")
        }
    }

    fun log(msg: String) {
        try {
            if (checkAndManageSize()) {
                LOG_FILE.appendText("$msg\n")
                XposedBridge.log("$LOG_TAG: $msg")
            }
        } catch (e: Exception) {
            Log.wtf(LOG_TAG, "Failed to log message: ${e.message}")
        }
    }

    private fun checkAndManageSize(): Boolean {
        if (LOG_FILE.length() > MAX_LOG_SIZE) {
            manageLogOverflow()
            return false
        }
        return true
    }

    private fun manageLogOverflow() {
        LOG_FILE.delete()
        LOG_FILE.createNewFile()
        XposedBridge.log("$LOG_TAG: Log file was reset due to size limit.")
    }
}
