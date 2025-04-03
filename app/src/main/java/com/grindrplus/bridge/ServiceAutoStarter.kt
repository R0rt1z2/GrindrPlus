package com.grindrplus.bridge

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import timber.log.Timber

/**
 * BroadcastReceiver to auto-start the Bridge Service on boot
 * and handle service restarts if needed
 */
class ServiceAutoStarter : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Timber.tag(TAG).d("Received broadcast action: $action")

        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == "com.grindrplus.START_BRIDGE_SERVICE") {
            Timber.tag(TAG).d("Starting BridgeService...")

            try {
                context.startForegroundService(Intent(context, BridgeService::class.java))
                Timber.tag(TAG).d("BridgeService started successfully")
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Failed to start BridgeService")
            }
        }
    }

    companion object {
        private const val TAG = "ServiceAutoStarter"
    }
}