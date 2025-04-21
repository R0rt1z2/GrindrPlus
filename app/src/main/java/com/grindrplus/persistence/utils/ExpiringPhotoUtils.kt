package com.grindrplus.persistence.utils

import android.util.Base64
import com.grindrplus.GrindrPlus
import com.grindrplus.core.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import kotlin.coroutines.resumeWithException

object ExpiringPhotoUtils {
    fun ByteArray.toBase64(): String {
        return Base64.encodeToString(this, Base64.DEFAULT)
    }

    fun String.fromBase64(): ByteArray {
        return Base64.decode(this, Base64.DEFAULT)
    }

    fun ByteArray.saveToFile(file: File): Boolean {
        return try {
            FileOutputStream(file).use { it.write(this) }
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun downloadImageAsByteArray(url: String): ByteArray? {
        return try {
            val client = OkHttpClient.Builder()
                .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .build()

            val request = Request.Builder()
                .url(url)
                .get()
                .build()

            suspendCancellableCoroutine { continuation ->
                val call = client.newCall(request)

                continuation.invokeOnCancellation {
                    call.cancel()
                }

                call.enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        continuation.resumeWithException(e)
                    }

                    override fun onResponse(call: Call, response: Response) {
                        if (!response.isSuccessful) {
                            continuation.resumeWithException(
                                IOException("Unexpected HTTP response: ${response.code}")
                            )
                            return
                        }

                        try {
                            val bytes = response.body?.bytes()
                            if (bytes != null) {
                                continuation.resume(bytes) { cause, _, _ -> null?.let { it(cause) } }
                            } else {
                                continuation.resumeWithException(
                                    IOException("Empty response body")
                                )
                            }
                        } catch (e: Exception) {
                            continuation.resumeWithException(e)
                        } finally {
                            response.close()
                        }
                    }
                })
            }
        } catch (e: Exception) {
            null
        }
    }

    fun downloadImageAsByteArraySync(url: String): ByteArray? {
        return try {
            val client = OkHttpClient.Builder()
                .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .build()

            val request = Request.Builder()
                .url(url)
                .get()
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return null

            response.body?.bytes()
        } catch (e: Exception) {
            null
        }
    }

    suspend fun createTmpFile(mediaId: Long, imageData: ByteArray):
            String? = withContext(Dispatchers.IO) {
        try {
            val context = GrindrPlus.context
            val tempDir = File(context.cacheDir, "expiring_photos")
            if (!tempDir.exists()) {
                tempDir.mkdirs()
            }
            val tempFile = File(tempDir, "$mediaId.jpg")
            imageData.saveToFile(tempFile)
            return@withContext "file://${tempFile.absolutePath}"
        } catch (e: Exception) {
            Logger.e("Error creating temporary file: ${e.message}")
            return@withContext null
        }
    }

    fun tmpFileExists(mediaId: Long): Boolean {
        val context = GrindrPlus.context
        val tempDir = File(context.cacheDir, "expiring_photos")
        val tempFile = File(tempDir, "$mediaId.jpg")
        return tempFile.exists()
    }

    fun getTmpFileUrl(mediaId: Long): String? {
        val context = GrindrPlus.context
        val tempDir = File(context.cacheDir, "expiring_photos")
        val tempFile = File(tempDir, "$mediaId.jpg")

        return if (tempFile.exists()) {
            "file://${tempFile.absolutePath}"
        } else {
            null
        }
    }

    fun deleteTmpFile(mediaId: Long): Boolean {
        try {
            val context = GrindrPlus.context
            val tempDir = File(context.cacheDir, "expiring_photos")
            val tempFile = File(tempDir, "$mediaId.jpg")

            return if (tempFile.exists()) {
                val result = tempFile.delete()
                Logger.d("Deleted temp file for mediaId $mediaId: $result")
                result
            } else {
                true
            }
        } catch (e: Exception) {
            Logger.e("Error deleting temp file: ${e.message}")
            return false
        }
    }

    fun cleanupTmpFiles() {
        try {
            val context = GrindrPlus.context
            val tempDir = File(context.cacheDir, "expiring_photos")
            if (tempDir.exists()) {
                tempDir.listFiles()?.forEach { it.delete() }
            }
        } catch (e: Exception) {
            Logger.e("Error cleaning up temporary files: ${e.message}")
        }
    }
}