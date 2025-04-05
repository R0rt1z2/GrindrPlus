package com.grindrplus.bridge

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import timber.log.Timber

/**
 * Activity that force starts the Bridge Service
 * Used as a mechanism to ensure the service is running
 * when needed by the client
 */
class ForceStartActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.tag(TAG).d("ForceStartActivity created")

        try {
            startService(Intent(this, BridgeService::class.java))
            Timber.tag(TAG).d("Bridge service started successfully")
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Failed to start bridge service")
        }

        finish()
    }

    companion object {
        private const val TAG = "ForceStartActivity"
    }
}