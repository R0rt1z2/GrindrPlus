package com.grindrplus.manager.utils

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageInstaller
import android.content.pm.PackageInstaller.SessionParams
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo
import org.bouncycastle.cert.X509v3CertificateBuilder
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import java.io.File
import java.io.IOException
import java.math.BigInteger
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.SecureRandom
import java.security.cert.Certificate
import java.security.cert.X509Certificate
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import androidx.core.net.toUri
import com.tonyodev.fetch2.DefaultFetchNotificationManager
import com.tonyodev.fetch2.Download
import com.tonyodev.fetch2.Error
import com.tonyodev.fetch2.Fetch
import com.tonyodev.fetch2.FetchConfiguration
import com.tonyodev.fetch2.FetchListener
import com.tonyodev.fetch2.NetworkType
import com.tonyodev.fetch2.Priority
import com.tonyodev.fetch2.Request
import com.tonyodev.fetch2core.DownloadBlock
import com.tonyodev.fetch2okhttp.OkHttpDownloader

/**
 * Helper class for installing APK files using the PackageInstaller API
 */
class SessionInstaller {
    companion object {
        private const val TAG = "SessionInstaller"
        private const val ACTION_INSTALL_COMPLETE = "com.grindrplus.INSTALL_COMPLETE"
        private const val DEFAULT_BUFFER_SIZE = 8192
        private const val INSTALL_TIMEOUT_MS = 120000L // 2 minutes
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
            Log.e(TAG, message)
            callback?.invoke(false, message)
            continuation.resumeWithException(IOException(message))
            return@suspendCoroutine
        }

        // Validate all APK files exist
        val missingApks = apks.filter { !it.exists() || it.length() <= 0 }
        if (missingApks.isNotEmpty()) {
            val message =
                "Missing or empty APK files: ${missingApks.joinToString { it.absolutePath }}"
            Log.e(TAG, message)
            log("ERROR: $message")
            callback?.invoke(false, message)
            continuation.resumeWithException(IOException(message))
            return@suspendCoroutine
        }

        val packageInstaller = context.packageManager.packageInstaller

        // Create installation session
        val params = SessionParams(SessionParams.MODE_FULL_INSTALL).apply {
            setInstallReason(PackageManager.INSTALL_REASON_USER)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                setInstallScenario(PackageManager.INSTALL_SCENARIO_FAST)
                if (silent) {
                    setRequireUserAction(SessionParams.USER_ACTION_NOT_REQUIRED)
                }
            }
        }

        // Create the session
        val sessionId = try {
            packageInstaller.createSession(params)
        } catch (e: IOException) {
            val message = "Failed to create install session: ${e.message}"
            Log.e(TAG, message, e)
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
                    context.unregisterReceiver(this)

                    val status = intent.getIntExtra(
                        PackageInstaller.EXTRA_STATUS,
                        PackageInstaller.STATUS_FAILURE
                    )
                    val message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
                        ?: "Unknown status"

                    Log.d(TAG, "Installation status: $status, message: $message")
                    log("DEBUG: $message")

                    when (status) {
                        PackageInstaller.STATUS_SUCCESS -> {
                            callback?.invoke(true, "Installation successful")
                            log("Installed!")
                            continuation.resume(true)
                        }

                        PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                            Log.d(TAG, "Installation requires user confirmation")
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
                                context.startActivity(confirmationIntent)
                            } else {
                                val errorMsg = "Missing confirmation intent"
                                log("ERROR: $errorMsg")
                                Log.e(TAG, errorMsg)
                                callback?.invoke(false, errorMsg)
                                continuation.resumeWithException(IOException(errorMsg))
                            }
                        }

                        else -> {
                            val errorMsg = "Installation failed: $message (code: $status)"
                            Log.e(TAG, errorMsg)
                            callback?.invoke(false, errorMsg)
                            continuation.resumeWithException(IOException(errorMsg))
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error in broadcast receiver", e)
                    callback?.invoke(false, "Error processing installation result: ${e.message}")
                    continuation.resumeWithException(e)
                }
            }
        }

        try {
            val intent = Intent(ACTION_INSTALL_COMPLETE).apply {
                setPackage(context.packageName)
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
                    Log.d(TAG, "Writing APK to session: ${apk.name} (${apk.length()} bytes)")

                    apk.inputStream().use { inputStream ->
                        session.openWrite(apk.name, 0, apk.length()).use { outputStream ->
                            inputStream.copyTo(outputStream, DEFAULT_BUFFER_SIZE)
                            session.fsync(outputStream)
                        }
                    }
                }

                Log.d(TAG, "Committing installation session...")
                session.commit(pendingIntent.intentSender)
            }
        } catch (e: Exception) {
            try {
                packageInstaller.abandonSession(sessionId)
            } catch (ignored: Exception) {
            }

            try {
                context.unregisterReceiver(installCompleteReceiver)
            } catch (ignored: Exception) {
            }

            val message = "Installation failed: ${e.message}"
            Log.e(TAG, message, e)
            callback?.invoke(false, message)
            continuation.resumeWithException(e)
            return@suspendCoroutine
        }
    }
}

/**
 * Downloads a file using the Fetch download manager
 * with proper error handling and progress monitoring
 *
 * @param context Android context
 * @param out Destination file
 * @param url URL to download from
 * @param onProgressUpdate Callback to report download progress
 * @return True if download succeeded, false otherwise
 */

data class DownloadResult(val success: Boolean, val reason: String?) {
    companion object {
        fun success() = DownloadResult(true, null)
        fun failure(reason: String) = DownloadResult(false, reason)
    }
}

suspend fun download(
    context: Context,
    out: File,
    url: String,
    onProgressUpdate: (Float?, Long?) -> Unit,
): DownloadResult = suspendCoroutine { continuation ->
    out.parentFile?.mkdirs()

    val fetchConfiguration = FetchConfiguration.Builder(context)
        .setDownloadConcurrentLimit(1)
        .enableAutoStart(true)
        .setHttpDownloader(OkHttpDownloader())
        .build()

    val fetch = Fetch.getInstance(fetchConfiguration)

    val request = Request(url, out.absolutePath).apply {
        priority = Priority.HIGH
        networkType = NetworkType.ALL
    }

    val fetchListener = object : FetchListener {
        override fun onStarted(
            download: Download,
            downloadBlocks: List<DownloadBlock>,
            totalBlocks: Int,
        ) {
            onProgressUpdate(0f, null)
        }

        override fun onProgress(
            download: Download,
            etaInMilliSeconds: Long,
            downloadedBytesPerSecond: Long,
        ) {
            val progress = download.downloaded.toFloat() / download.total
            onProgressUpdate(progress, etaInMilliSeconds)
        }

        override fun onPaused(download: Download) {
            onProgressUpdate(null, null)
        }

        override fun onResumed(download: Download) {
            // Resume tracking progress
        }

        override fun onCancelled(download: Download) {
            fetch.removeListener(this)
            if (out.exists()) out.delete()
            continuation.resume(DownloadResult.failure("Download cancelled"))
        }

        override fun onCompleted(download: Download) {
            fetch.removeListener(this)
            if (validateDownload(out)) {
                continuation.resume(DownloadResult.success())
            } else {
                if (out.exists()) out.delete()
                continuation.resume(DownloadResult.failure("Downloaded file validation failed"))
            }
        }

        override fun onError(download: Download, error: Error, throwable: Throwable?) {
            fetch.removeListener(this)
            Log.e("Download", "Download failed", throwable)
            if (out.exists()) out.delete()
            continuation.resume(DownloadResult.failure(throwable?.message ?: error.name))
        }

        override fun onWaitingNetwork(download: Download) {
            onProgressUpdate(null, null)
        }

        override fun onAdded(download: Download) {}
        override fun onQueued(download: Download, waitingOnNetwork: Boolean) {}
        override fun onRemoved(download: Download) {}
        override fun onDeleted(download: Download) {}
        override fun onDownloadBlockUpdated(
            download: Download,
            downloadBlock: DownloadBlock,
            totalBlocks: Int,
        ) {
        }
    }

    try {
        fetch.addListener(fetchListener)
        fetch.enqueue(
            request,
            { Log.d("Download", "Download started: $url") },
            { Log.e("Download", "Failed to start download", it.throwable) }
        )
    } catch (e: Exception) {
        Log.e("Download", "Failed to start download", e)
        fetch.removeListener(fetchListener)
        if (out.exists()) out.delete()
        continuation.resume(DownloadResult.failure(e.message ?: "Unknown error"))
    }
}

/**
 * Validates that a downloaded file is complete and not corrupted
 */
private fun validateDownload(file: File): Boolean {
    if (!file.exists() || file.length() <= 0) {
        return false
    }

    if (file.name.endsWith(".zip") || file.name.endsWith(".xapk")) {
        try {
            ZipFile(file).close()
            return true
        } catch (e: Exception) {
            Log.e("Download", "Invalid ZIP file: ${e.localizedMessage}")
            file.delete()
            return false
        }
    }

    // TODO: Add more validation checks for other file types
    return true
}

/**
 * Unzips a file to the specified directory with proper error handling
 *
 * @param unzipLocationRoot The target directory (or null to use same directory)
 * @throws IOException If extraction fails
 */
fun File.unzip(unzipLocationRoot: File? = null) {
    if (!exists() || length() <= 0) {
        throw IOException("ZIP file doesn't exist or is empty: $absolutePath")
    }

    val rootFolder =
        unzipLocationRoot ?: File(parentFile.absolutePath + File.separator + nameWithoutExtension)

    if (!rootFolder.exists()) {
        if (!rootFolder.mkdirs()) {
            throw IOException("Failed to create output directory: ${rootFolder.absolutePath}")
        }
    }

    try {
        ZipFile(this).use { zip ->
            val entries = zip.entries().asSequence().toList()

            if (entries.isEmpty()) {
                throw IOException("ZIP file is empty: $absolutePath")
            }

            for (entry in entries) {
                val outputFile = File(rootFolder.absolutePath + File.separator + entry.name)

                // cute zip slip vulnerability
                if (!outputFile.canonicalPath.startsWith(rootFolder.canonicalPath + File.separator)) {
                    throw SecurityException("ZIP entry is outside of target directory: ${entry.name}")
                }

                if (entry.isDirectory) {
                    if (!outputFile.exists() && !outputFile.mkdirs()) {
                        throw IOException("Failed to create directory: ${outputFile.absolutePath}")
                    }
                } else {
                    outputFile.parentFile?.let {
                        if (!it.exists() && !it.mkdirs()) {
                            throw IOException("Failed to create parent directory: ${it.absolutePath}")
                        }
                    }

                    zip.getInputStream(entry).use { input ->
                        outputFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                }
            }
        }
    } catch (e: Exception) {
        if (unzipLocationRoot != null && unzipLocationRoot.exists()) {
            unzipLocationRoot.deleteRecursively()
        }

        when (e) {
            is SecurityException -> throw e
            else -> throw IOException("Failed to extract ZIP file: ${e.localizedMessage}", e)
        }
    }
}

/**
 * Creates a new keystore file with a self-signed certificate for APK signing
 *
 * @param out The output keystore file
 * @throws Exception If keystore creation fails
 */
@SuppressLint("NewApi")
fun newKeystore(out: File) {
    try {
        val key = createKey()

        KeyStore.getInstance(KeyStore.getDefaultType()).apply {
            load(null, "password".toCharArray())
            setKeyEntry(
                "alias",
                key.privateKey,
                "password".toCharArray(),
                arrayOf<Certificate>(key.publicKey)
            )
            store(out.outputStream(), "password".toCharArray())
        }
    } catch (e: Exception) {
        if (out.exists()) out.delete()
        throw IOException("Failed to create keystore: ${e.localizedMessage}", e)
    }
}

/**
 * Creates a key pair for signing APKs
 */
private fun createKey(): KeySet {
    try {
        var serialNumber: BigInteger

        do serialNumber = SecureRandom().nextInt().toBigInteger()
        while (serialNumber < BigInteger.ZERO)

        val x500Name = X500Name("CN=GrindrPlus")
        val pair = KeyPairGenerator.getInstance("RSA").run {
            initialize(2048)
            generateKeyPair()
        }

        // Valid for 30 years
        val notBefore = Date(System.currentTimeMillis() - 1000L * 60L * 60L * 24L * 30L)
        val notAfter = Date(System.currentTimeMillis() + 1000L * 60L * 60L * 24L * 366L * 30L)

        val builder = X509v3CertificateBuilder(
            x500Name,
            serialNumber,
            notBefore,
            notAfter,
            Locale.ENGLISH,
            x500Name,
            SubjectPublicKeyInfo.getInstance(pair.public.encoded)
        )

        val signer = JcaContentSignerBuilder("SHA256withRSA").build(pair.private)

        return KeySet(
            JcaX509CertificateConverter().getCertificate(builder.build(signer)),
            pair.private
        )
    } catch (e: Exception) {
        throw IOException("Failed to create signing key: ${e.localizedMessage}", e)
    }
}

/**
 * Data class to hold a key pair for APK signing
 */
class KeySet(val publicKey: X509Certificate, val privateKey: PrivateKey)

/**
 * Data class for ZIP operations
 */
data class ZipIO(val entry: ZipEntry, val output: File)