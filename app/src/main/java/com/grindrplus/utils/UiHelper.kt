package com.grindrplus.utils

import android.widget.Toast
import com.grindrplus.GrindrPlus.runOnMainThread
import com.grindrplus.core.LogSource
import com.grindrplus.core.Logger

object UiHelper {
    fun showToast(message: CharSequence, duration: Int) {
        runOnMainThread { ctx ->
            runCatching {
                Toast.makeText(ctx, message, duration).show()
            }.onFailure { error ->
                Logger.e("Failed to show toast: ${error.message}", LogSource.MODULE)
            }
        }
    }
}
