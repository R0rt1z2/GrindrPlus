package com.grindrplus.core

import android.app.Activity

interface ActivityCallback {
    fun onActivityAvailable(activity: Activity)
}