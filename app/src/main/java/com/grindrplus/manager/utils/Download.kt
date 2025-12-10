package com.grindrplus.manager.utils

import android.annotation.SuppressLint
import android.content.Context
import com.grindrplus.R
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
import com.tonyodev.fetch2core.Downloader
import com.tonyodev.fetch2okhttp.OkHttpDownloader
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import java.io.File
import java.security.KeyStore
import java.security.SecureRandom
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

data class DownloadResult(val success: Boolean, val reason: String?) {
    companion object {
        fun success() = DownloadResult(true, null)
        fun failure(reason: String) = DownloadResult(false, reason)
    }
}

@Suppress("ObjectPropertyName")
private var _fetch: Fetch? = null
private val fetch: Fetch get() = _fetch!!

/**
 * Downloads a file using Fetch2 library
 * with proper error handling and progress monitoring
 *
 * @param context Android context
 * @param out Destination file
 * @param url URL to download from
 * @param print Callback to report download progress
 * @return True if download succeeded, false otherwise
 */
suspend fun download(
    context: Context,
    out: File,
    url: String,
    printConsole: (String) -> Unit,
): DownloadResult = withContext(Dispatchers.IO) {
    try {
        out.parentFile?.mkdirs()

        var lastUpdateTime = System.currentTimeMillis()
        var lastBytesDownloaded = 0L
        var averageSpeed = 0.0

        if (_fetch == null || _fetch?.isClosed == true) {
            _fetch = Fetch.Impl.getInstance(
                FetchConfiguration.Builder(context)
                    .setDownloadConcurrentLimit(10)
                    .setAutoRetryMaxAttempts(3)
                    .enableLogging(true)
                    .enableAutoStart(true)
                    .enableRetryOnNetworkGain(true)
                    .setHttpDownloader(
                        OkHttpDownloader(
                            getCustomTrustedOkHttpClient(context),
                            Downloader.FileDownloaderType.PARALLEL
                        )
                    )
                    .setNotificationManager(
                        object : DefaultFetchNotificationManager(context) {
                            override fun getFetchInstanceForNamespace(namespace: String) = fetch
                        }
                    )
                    .build()
            )
        }

        val request = Request(url, out.absolutePath).apply {
            priority = Priority.HIGH
            networkType = NetworkType.ALL
        }

        return@withContext suspendCoroutine { continuation ->
            fetch.addListener(object : FetchListener {
                override fun onStarted(
                    download: Download,
                    downloadBlocks: List<DownloadBlock>,
                    totalBlocks: Int,
                ) {
                    printConsole("Starting download...")
                }

                @SuppressLint("DefaultLocale")
                override fun onProgress(
                    download: Download,
                    etaInMilliSeconds: Long,
                    downloadedBytesPerSecond: Long,
                ) {
                    val currentTime = System.currentTimeMillis()
                    val timeDelta = currentTime - lastUpdateTime

                    if (timeDelta > 0) {
                        val bytesDelta = download.downloaded - lastBytesDownloaded
                        val currentSpeed = bytesDelta.toDouble() / timeDelta / 1024 / 1024
                        averageSpeed = if (averageSpeed == 0.0) currentSpeed
                        else (averageSpeed * 0.7 + currentSpeed * 0.3)
                        val percentage = download.progress

                        val speedText = when {
                            averageSpeed >= 1.0 -> String.format("%.2f Mb/s", averageSpeed * 8)
                            averageSpeed >= 0.001 -> String.format(
                                "%.2f Kb/s",
                                averageSpeed * 1024 * 8
                            )

                            else -> String.format("%.2f b/s", averageSpeed * 1024 * 1024 * 8)
                        }

                        printConsole(
                            "Download status<>: " +
                                    "$percentage% $speedText " +
                                    "(ETA:${etaInMilliSeconds.div(60000)}m${
                                        (etaInMilliSeconds.rem(
                                            60000
                                        )).div(1000)
                                    }s)<progressBar:${(percentage / 100f).coerceIn(0f, 1f)}:>"
                        )

                        lastUpdateTime = currentTime
                        lastBytesDownloaded = download.downloaded
                    }
                }

                override fun onError(download: Download, error: Error, throwable: Throwable?) {
                    fetch.removeListener(this)
                    fetch.close()
                    if (out.exists()) out.delete()
                    continuation.resume(DownloadResult.failure(error.name))
                }

                override fun onCompleted(download: Download) {
                    fetch.removeListener(this)
                    printConsole("Completed download")

                    if (validateFile(out)) {
                        continuation.resume(DownloadResult.success())
                    } else {
                        if (out.exists()) out.delete()
                        continuation.resume(DownloadResult.failure("Downloaded file validation failed"))
                    }
                }

                override fun onCancelled(download: Download) {
                    fetch.removeListener(this)
                    if (out.exists()) out.delete()
                    continuation.resume(DownloadResult.failure("Download cancelled"))
                }

                override fun onPaused(download: Download) {
                    printConsole("Paused.")
                }

                override fun onQueued(download: Download, waitingOnNetwork: Boolean) {}
                override fun onRemoved(download: Download) {}
                override fun onDeleted(download: Download) {}
                override fun onResumed(download: Download) {}
                override fun onWaitingNetwork(download: Download) {}
                override fun onAdded(download: Download) {}
                override fun onDownloadBlockUpdated(
                    download: Download,
                    downloadBlock: DownloadBlock,
                    totalBlocks: Int,
                ) {
                }
            })

            try {
                fetch.removeAll()
                fetch.enqueue(request)
            } catch (e: Exception) {
                if (out.exists()) out.delete()
                continuation.resume(DownloadResult.failure(e.message ?: "Unknown error"))
            }
        }
    } catch (e: CancellationException) {
        if (out.exists()) out.delete()
        throw e
    } catch (e: Exception) {
        if (out.exists()) out.delete()
        return@withContext DownloadResult.failure(e.message ?: "Unknown error")
    }
}

fun getCustomTrustedOkHttpClient(context: Context): OkHttpClient {
    // Load the default trust manager
    val defaultTrustManager =
        TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm()).apply {
            init(null as KeyStore?)
        }.trustManagers[0] as X509TrustManager

    // Load custom certificate
    val certificateFactory = CertificateFactory.getInstance("X.509")
    val customCertificate = context.resources.openRawResource(R.raw.cert)
        .use { certificateFactory.generateCertificate(it) }

    // Create keystore with custom certificate
    val keyStore = KeyStore.getInstance(KeyStore.getDefaultType()).apply {
        load(null, null)
        setCertificateEntry("custom", customCertificate)
    }

    // Create custom trust manager
    val customTrustManager =
        TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm()).apply {
            init(keyStore)
        }.trustManagers[0] as X509TrustManager

    // Combine both trust managers
    val combinedTrustManager = @SuppressLint("CustomX509TrustManager")
    object : X509TrustManager {
        override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {
            try {
                defaultTrustManager.checkClientTrusted(chain, authType)
            } catch (_: Exception) {
                customTrustManager.checkClientTrusted(chain, authType)
            }
        }

        override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {
            try {
                defaultTrustManager.checkServerTrusted(chain, authType)
            } catch (_: Exception) {
                customTrustManager.checkServerTrusted(chain, authType)
            }
        }

        override fun getAcceptedIssuers(): Array<X509Certificate> =
            defaultTrustManager.acceptedIssuers + customTrustManager.acceptedIssuers
    }

    val sslContext = SSLContext.getInstance("TLS").apply {
        init(null, arrayOf(combinedTrustManager), SecureRandom())
    }

    return OkHttpClient.Builder()
        .sslSocketFactory(sslContext.socketFactory, combinedTrustManager)
        .hostnameVerifier { _, _ -> true }
        .build()
}