package com.grindrplus.bridge

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.grindrplus.BuildConfig
import com.grindrplus.core.LogSource
import com.grindrplus.core.Logger
import timber.log.Timber

/**
 * Activity that force starts the Bridge Service
 * This is used as a lightweight mechanism to ensure the service is running
 * without requiring foreground service notifications
 */
class ForceStartActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.tag(TAG).d("ForceStartActivity created")

        try {
            val serviceIntent = Intent().apply {
                setClassName(
                    BuildConfig.APPLICATION_ID,
                    "${BuildConfig.APPLICATION_ID}.bridge.BridgeService"
                )
            }
            startService(serviceIntent)

            val pkg = intent.getStringExtra("pkg")
            if (pkg != null) {
                val launchIntent = packageManager.getLaunchIntentForPackage(pkg)
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(launchIntent)
                    Timber.tag(TAG).d("Launched package: $pkg")
                }
            }

            Timber.tag(TAG).d("Bridge service started successfully")
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Failed to start bridge service")
        }

        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.tag(TAG).d("ForceStartActivity destroyed")
    }

    companion object {
        private const val TAG = "ForceStartActivity"

        fun createIntent(context: android.content.Context, packageToLaunch: String? = null): Intent {
            return Intent().apply {
                setClassName(
                    BuildConfig.APPLICATION_ID,
                    "${BuildConfig.APPLICATION_ID}.bridge.ForceStartActivity"
                )
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                if (packageToLaunch != null) {
                    putExtra("pkg", packageToLaunch)
                }
            }
        }
    }
}