package com.grindrplus.manager.utils

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import timber.log.Timber
import java.io.File
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * Helper class for installing APK files using the PackageInstaller API
 */
class SessionInstaller {
    companion object {
        private const val TAG = "SessionInstaller"
        private const val ACTION_INSTALL_COMPLETE = "com.grindrplus.INSTALL_COMPLETE"
        private const val DEFAULT_BUFFER_SIZE = 8192
    }

    /**
     * Install multiple APK files (split APKs) using PackageInstaller
     *
     * @param context The application context
     * @param apks List of APK files to install
     * @param silent Whether to install silently (requires privileged permissions)
     * @param callback Optional callback to report success/failure
     * @return True if installation was successful, false otherwise
     */
    suspend fun installApks(
        context: Context,
        apks: List<File>,
        silent: Boolean = false,
        callback: ((success: Boolean, message: String) -> Unit)? = null,
        log: (String) -> Unit,
    ): Boolean = suspendCoroutine { continuation ->
        if (apks.isEmpty()) {
            val message = "No APK files provided."
            Timber.Forest.tag(TAG).e(message)
            callback?.invoke(false, message)
            continuation.resumeWithException(IOException(message))
            return@suspendCoroutine
        }

        // Validate all APK files exist
        val missingApks = apks.filter { !it.exists() || it.length() <= 0 }
        if (missingApks.isNotEmpty()) {
            val message =
                "Missing or empty APK files: ${missingApks.joinToString { it.absolutePath }}"
            Timber.Forest.tag(TAG).e(message)
            log("ERROR: $message")
            callback?.invoke(false, message)
            continuation.resumeWithException(IOException(message))
            return@suspendCoroutine
        }

        val packageInstaller = context.packageManager.packageInstaller

        // Create installation session
        val params =
            PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
                setInstallReason(PackageManager.INSTALL_REASON_USER)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    setInstallScenario(PackageManager.INSTALL_SCENARIO_FAST)
                    if (silent) {
                        setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
                    }
                }
            }

        // Create the session
        val sessionId = try {
            packageInstaller.createSession(params)
        } catch (e: IOException) {
            val message = "Failed to create install session: ${e.message}"
            Timber.Forest.tag(TAG).e(e, message)
            log("ERROR: $message")
            callback?.invoke(false, message)
            continuation.resumeWithException(e)
            return@suspendCoroutine
        }

        // Process for completion
        val installCompleteReceiver = object : BroadcastReceiver() {
            @SuppressLint("UnsafeIntentLaunch")
            override fun onReceive(context: Context, intent: Intent) {
                try {
                    val status = intent.getIntExtra(
                        PackageInstaller.EXTRA_STATUS,
                        PackageInstaller.STATUS_FAILURE
                    )

                    val message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
                        ?: "Unknown status"

                    Timber.Forest.tag(TAG).d("Installation status: $status, message: $message")
                    log("DEBUG: $message")

                    when (status) {
                        PackageInstaller.STATUS_SUCCESS -> {
                            callback?.invoke(true, "Installation successful")
                            log("Installed!")
                            context.unregisterReceiver(this)
                            continuation.resume(true)
                        }

                        PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                            Timber.Forest.tag(TAG).d("Installation requires user confirmation")
                            log("DEBUG: Installation requires user confirmation")
                            val confirmationIntent =
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    intent.getParcelableExtra(
                                        Intent.EXTRA_INTENT,
                                        Intent::class.java
                                    )
                                } else {
                                    @Suppress("DEPRECATION")
                                    intent.getParcelableExtra(Intent.EXTRA_INTENT)
                                }
                            if (confirmationIntent != null) {
                                confirmationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                try {
                                    context.startActivity(confirmationIntent)
                                    // Don't complete the coroutine yet - wait for final result
                                } catch (e: Exception) {
                                    val errorMsg =
                                        "Failed to start installer activity: ${e.message}"
                                    log("ERROR: $errorMsg")
                                    Timber.Forest.tag(TAG).e(e, errorMsg)
                                    context.unregisterReceiver(this)
                                    callback?.invoke(false, errorMsg)
                                    continuation.resumeWithException(IOException(errorMsg))
                                }
                            } else {
                                val errorMsg = "Missing confirmation intent"
                                log("ERROR: $errorMsg")
                                Timber.Forest.tag(TAG).e(errorMsg)
                                context.unregisterReceiver(this)
                                callback?.invoke(false, errorMsg)
                                continuation.resumeWithException(IOException(errorMsg))
                            }
                        }

                        PackageInstaller.STATUS_FAILURE,
                        PackageInstaller.STATUS_FAILURE_ABORTED,
                        PackageInstaller.STATUS_FAILURE_BLOCKED,
                        PackageInstaller.STATUS_FAILURE_CONFLICT,
                        PackageInstaller.STATUS_FAILURE_INCOMPATIBLE,
                        PackageInstaller.STATUS_FAILURE_INVALID,
                        PackageInstaller.STATUS_FAILURE_STORAGE,
                            -> {
                            val errorMsg = "Installation failed: $message (code: $status)"
                            Timber.Forest.tag(TAG).e(errorMsg)
                            context.unregisterReceiver(this)
                            callback?.invoke(false, errorMsg)
                            continuation.resumeWithException(IOException(errorMsg))
                        }

                        else -> {
                            val errorMsg = "Unknown status code: $status - $message"
                            Timber.Forest.tag(TAG).e(errorMsg)
                            context.unregisterReceiver(this)
                            callback?.invoke(false, errorMsg)
                            continuation.resumeWithException(IOException(errorMsg))
                        }
                    }
                } catch (e: Exception) {
                    Timber.Forest.tag(TAG).e(e, "Error in broadcast receiver")
                    context.unregisterReceiver(this)
                    callback?.invoke(false, "Error processing installation result: ${e.message}")
                    continuation.resumeWithException(e)
                }
            }
        }

        try {
            val intent = Intent(ACTION_INSTALL_COMPLETE).apply {
                setPackage(context.packageName)
                addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
            }

            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }

            ContextCompat.registerReceiver(
                context,
                installCompleteReceiver,
                IntentFilter(ACTION_INSTALL_COMPLETE),
                ContextCompat.RECEIVER_EXPORTED
            )

            val pendingIntent = PendingIntent.getBroadcast(context, sessionId, intent, flags)

            packageInstaller.openSession(sessionId).use { session ->
                for (apk in apks) {
                    Timber.Forest.tag(TAG)
                        .d("Writing APK to session: ${apk.name} (${apk.length()} bytes)")

                    apk.inputStream().use { inputStream ->
                        session.openWrite(apk.name, 0, apk.length()).use { outputStream ->
                            inputStream.copyTo(outputStream, DEFAULT_BUFFER_SIZE)
                            session.fsync(outputStream)
                        }
                    }
                }

                Timber.Forest.tag(TAG).d("Committing installation session...")
                session.commit(pendingIntent.intentSender)
            }
        } catch (e: Exception) {
            try {
                packageInstaller.abandonSession(sessionId)
            } catch (_: Exception) {
            }

            try {
                context.unregisterReceiver(installCompleteReceiver)
            } catch (_: Exception) {
            }

            val message = "Installation failed: ${e.message}"
            Timber.Forest.tag(TAG).e(e, message)
            callback?.invoke(false, message)
            continuation.resumeWithException(e)
        }
    }
}