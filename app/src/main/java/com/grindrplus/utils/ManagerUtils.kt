package com.grindrplus.utils

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
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.core.net.toUri
import kotlinx.coroutines.delay
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
import kotlin.coroutines.cancellation.CancellationException

class SessionInstaller(private val context: Context) {
    fun installApks(
        context: Context,
        apks: List<File>,
        silent: Boolean = false,
        callback: ((success: Boolean, message: String) -> Unit)? = null,
    ) {
        if (apks.isEmpty()) {
            val message = "No APK files provided."
            Log.e(TAG, message)
            callback?.invoke(false, message)
            return
        }


        val missingApks = apks.filter { !it.exists() }
        if (missingApks.isNotEmpty()) {
            val message = "Missing APK files: ${missingApks.joinToString { it.absolutePath }}"
            Log.e(TAG, message)
            callback?.invoke(false, message)
            return
        }

        val packageInstaller = context.packageManager.packageInstaller


        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
            setInstallReason(PackageManager.INSTALL_REASON_USER)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                setInstallScenario(PackageManager.INSTALL_SCENARIO_FAST)
                if (silent) {
                    setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
                }
            }
        }


        val sessionId = try {
            packageInstaller.createSession(params)
        } catch (e: IOException) {
            val message = "Failed to create install session: ${e.message}"
            Log.e(TAG, message, e)
            callback?.invoke(false, message)
            return
        }

        try {
            packageInstaller.openSession(sessionId).use { session ->
                for (apk in apks) {
                    Log.d(TAG, "Installing APK: ${apk.name}")

                    apk.inputStream().use { inputStream ->
                        session.openWrite(apk.name, 0, apk.length()).use { outputStream ->
                            inputStream.copyTo(outputStream, DEFAULT_BUFFER_SIZE)
                            session.fsync(outputStream)
                        }
                    }
                }

                val receiver = object : BroadcastReceiver() {
                    override fun onReceive(context: Context, intent: Intent) {
                        try {
                            context.unregisterReceiver(this)

                            val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)
                            val message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE) ?: "Unknown status"

                            when (status) {
                                PackageInstaller.STATUS_SUCCESS -> {
                                    Log.d(TAG, "Installation succeeded")
                                    callback?.invoke(true, "Installation successful")
                                }
                                PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                                    Log.d(TAG, "Requesting user confirmation")
                                    val confirmationIntent = intent.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)
                                    if (confirmationIntent != null) {
                                        confirmationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        context.startActivity(confirmationIntent)
                                    } else {
                                        Log.e(TAG, "Missing confirmation intent")
                                        callback?.invoke(false, "Failed to request user confirmation")
                                    }
                                }
                                else -> {
                                    Log.e(TAG, "Installation failed: $message (code: $status)")
                                    callback?.invoke(false, "Installation failed: $message")
                                }
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error in broadcast receiver", e)
                            callback?.invoke(false, "Error processing installation result: ${e.message}")
                        }
                    }
                }

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
                    receiver,
                    IntentFilter(ACTION_INSTALL_COMPLETE),
                    ContextCompat.RECEIVER_EXPORTED
                )
                val pendingIntent = PendingIntent.getBroadcast(context, sessionId, intent, flags)

                Log.d(TAG, "Committing session...")
                session.commit(pendingIntent.intentSender)
            }
        } catch (e: Exception) {
            try {
                packageInstaller.abandonSession(sessionId)
            } catch (ignored: Exception) {}

            val message = "Installation failed: ${e.message}"
            Log.e(TAG, message, e)
            callback?.invoke(false, message)
        }
    }

    companion object {
        private const val TAG = "SplitAPK"
        private const val ACTION_INSTALL_COMPLETE = "com.grindrplus.INSTALL_COMPLETE"
        private const val DEFAULT_BUFFER_SIZE = 8192
    }
}

fun newKeystore(out: File) {
    val key = createKey()

    with(KeyStore.getInstance(KeyStore.getDefaultType())) {
        load(null, "password".toCharArray())
        setKeyEntry(
            "alias",
            key.privateKey,
            "password".toCharArray(),
            arrayOf<Certificate>(key.publicKey)
        )
        store(out.outputStream(), "password".toCharArray())
    }
}

private fun createKey(): KeySet {
    var serialNumber: BigInteger

    do serialNumber = SecureRandom().nextInt().toBigInteger()
    while (serialNumber < BigInteger.ZERO)

    val x500Name = X500Name("CN=GrindrPlus")
    val pair = KeyPairGenerator.getInstance("RSA").run {
        initialize(2048)
        generateKeyPair()
    }
    val builder = X509v3CertificateBuilder(
        /* p0 = */ x500Name,
        /* p1 = */
        serialNumber,
        /* p2 = */
        Date(System.currentTimeMillis() - 1000L * 60L * 60L * 24L * 30L),
        /* p3 = */
        Date(System.currentTimeMillis() + 1000L * 60L * 60L * 24L * 366L * 30L),
        /* p4 = */
        Locale.ENGLISH,
        /* p5 = */
        x500Name,
        /* p6 = */
        SubjectPublicKeyInfo.getInstance(pair.public.encoded)
    )
    val signer = JcaContentSignerBuilder("SHA1withRSA").build(pair.private)

    return KeySet(
        JcaX509CertificateConverter().getCertificate(builder.build(signer)),
        pair.private
    )
}

class KeySet(val publicKey: X509Certificate, val privateKey: PrivateKey)

suspend fun download(
    context: Context,
    out: File,
    url: String,
    onProgressUpdate: (Float?) -> Unit,
): Boolean {

    val downloadManager = context.getSystemService<DownloadManager>()
        ?: throw IllegalStateException("DownloadManager service is not available")

    val downloadId = DownloadManager.Request(url.toUri())
        .setTitle("GrindrPlus")
        .setDescription("Downloading ${out.name}...")
        .setDestinationUri(Uri.fromFile(out))
        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
        .setAllowedOverMetered(true)
        .setAllowedOverRoaming(true)
        .let(downloadManager::enqueue)

    val lastProgressTime = System.currentTimeMillis()

    while (true) {
        try {
            // Hand over control to a suspend function to check for cancellation
            delay(100)
        } catch (_: CancellationException) {
            // If the running CoroutineScope has been cancelled, then gracefully cancel download
            downloadManager.remove(downloadId)
            return false
        }

        // Request download status
        val cursor = DownloadManager.Query()
            .setFilterById(downloadId)
            .let(downloadManager::query)

        // No results in cursor, download was cancelled
        if (!cursor.moveToFirst()) {
            cursor.close()
            return false
        }

        val statusColumn = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
        val status = cursor.getInt(statusColumn)

        cursor.use {
            when (status) {
                DownloadManager.STATUS_PENDING, DownloadManager.STATUS_PAUSED -> {
                    val currentTime = System.currentTimeMillis()
                    val elapsedTime = currentTime - lastProgressTime

                    if (elapsedTime >= 5000L) {
                        return false
                    }

                    onProgressUpdate(null)
                }

                DownloadManager.STATUS_RUNNING ->
                    onProgressUpdate(getDownloadProgress(cursor))

                DownloadManager.STATUS_SUCCESSFUL ->
                    return true

                DownloadManager.STATUS_FAILED -> {
                    val reasonColumn = cursor.getColumnIndex(DownloadManager.COLUMN_REASON)
                    val reason = cursor.getInt(reasonColumn)

                    return false
                }
            }
        }

    }
}

private fun getDownloadProgress(queryCursor: Cursor): Float? {
    val bytesColumn = queryCursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
    val bytes = queryCursor.getLong(bytesColumn)

    val totalBytesColumn = queryCursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
    val totalBytes = queryCursor.getLong(totalBytesColumn)

    if (totalBytes <= 0) return null
    return bytes.toFloat() / totalBytes
}

data class ZipIO(val entry: ZipEntry, val output: File)

fun File.unzip(unzipLocationRoot: File? = null) {

    val rootFolder =
        unzipLocationRoot ?: File(parentFile.absolutePath + File.separator + nameWithoutExtension)
    if (!rootFolder.exists()) {
        rootFolder.mkdirs()
    }

    ZipFile(this).use { zip ->
        zip
            .entries()
            .asSequence()
            .map {
                val outputFile = File(rootFolder.absolutePath + File.separator + it.name)
                ZipIO(it, outputFile)
            }
            .map {
                it.output.parentFile?.run {
                    if (!exists()) mkdirs()
                }
                it
            }
            .filter { !it.entry.isDirectory }
            .forEach { (entry, output) ->
                zip.getInputStream(entry).use { input ->
                    output.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            }
    }

}
