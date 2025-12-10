package com.grindrplus.utils

import android.util.Base64
import android.webkit.MimeTypeMap
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
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object MediaUtils {
    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    private val mediaDir: File by lazy {
        File(GrindrPlus.context.filesDir, "saved_media").apply {
            if (!exists()) mkdirs()
        }
    }

    private val imageDir: File by lazy {
        File(mediaDir, "images").apply {
            if (!exists()) mkdirs()
        }
    }

    private val videoDir: File by lazy {
        File(mediaDir, "videos").apply {
            if (!exists()) mkdirs()
        }
    }

    /**
     * Determines media type from URL or content type
     * @param url The media URL
     * @param contentType Optional content type header
     * @return MediaType.IMAGE, MediaType.VIDEO, or MediaType.UNKNOWN
     */
    fun getMediaType(url: String, contentType: String? = null): MediaType {
        if (!contentType.isNullOrBlank()) {
            if (contentType.startsWith("image/")) return MediaType.IMAGE
            if (contentType.startsWith("video/")) return MediaType.VIDEO
        }

        val extension = MimeTypeMap.getFileExtensionFromUrl(url)?.lowercase() ?: ""
        return when {
            extension in imageExtensions -> MediaType.IMAGE
            extension in videoExtensions -> MediaType.VIDEO
            url.contains("image", ignoreCase = true) -> MediaType.IMAGE
            url.contains("video", ignoreCase = true) -> MediaType.VIDEO
            else -> MediaType.UNKNOWN
        }
    }

    /**
     * Converts a ByteArray to a Base64 encoded string
     */
    fun ByteArray.toBase64(): String = Base64.encodeToString(this, Base64.DEFAULT)

    /**
     * Decodes a Base64 string back to ByteArray
     */
    fun String.fromBase64(): ByteArray = Base64.decode(this, Base64.DEFAULT)

    /**
     * Saves a ByteArray to the specified file
     * @return Result containing true on success or an exception on failure
     */
    fun ByteArray.saveToFile(file: File): Result<Boolean> = runCatching {
        FileOutputStream(file).use { it.write(this) }
        true
    }

    /**
     * Downloads media from a URL as a ByteArray using coroutines
     * @param url The URL of the media to download
     * @return Result containing the downloaded ByteArray or an exception on failure
     */
    suspend fun downloadMedia(url: String): Result<ByteArray> = runCatching {
        suspendCancellableCoroutine { continuation ->
            val request = Request.Builder()
                .url(url)
                .get()
                .build()

            val call = httpClient.newCall(request)

            continuation.invokeOnCancellation {
                call.cancel()
            }

            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    continuation.resumeWithException(e)
                }

                override fun onResponse(call: Call, response: Response) {
                    try {
                        if (!response.isSuccessful) {
                            continuation.resumeWithException(
                                IOException("Unexpected HTTP response: ${response.code}")
                            )
                            return
                        }

                        response.body?.bytes()?.let {
                            continuation.resume(it)
                        } ?: continuation.resumeWithException(
                            IOException("Empty response body")
                        )
                    } catch (e: Exception) {
                        continuation.resumeWithException(e)
                    } finally {
                        response.close()
                    }
                }
            })
        }
    }

    /**
     * Synchronously downloads media from a URL as a ByteArray
     * @param url The URL of the media to download
     * @return Result containing the downloaded ByteArray or an exception on failure
     */
    fun downloadMediaSync(url: String): Result<ByteArray> = runCatching {
        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Unexpected HTTP response: ${response.code}")
            }

            response.body?.bytes() ?: throw IOException("Empty response body")
        }
    }

    /**
     * Saves media permanently and returns its file URL
     *
     * @param mediaId The ID of the media
     * @param mediaData The media data as ByteArray
     * @param mediaType The type of media (image or video)
     * @param extension Optional file extension (defaults to jpg for images, mp4 for videos)
     * @return Result containing the file URI as a string or an exception on failure
     */
    suspend fun saveMedia(
        mediaId: Long,
        mediaData: ByteArray,
        mediaType: MediaType = MediaType.IMAGE,
        extension: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val directory = when (mediaType) {
                MediaType.IMAGE -> imageDir
                MediaType.VIDEO -> videoDir
                MediaType.UNKNOWN -> mediaDir
            }

            val fileExtension = extension ?: when (mediaType) {
                MediaType.IMAGE -> "jpg"
                MediaType.VIDEO -> "mp4"
                MediaType.UNKNOWN -> "bin"
            }

            val mediaFile = File(directory, "$mediaId.$fileExtension")

            if (!mediaFile.exists()) {
                mediaData.saveToFile(mediaFile).getOrThrow()
                Logger.d("Saved ${mediaType.name.lowercase()} for mediaId $mediaId to ${mediaFile.absolutePath}")
            }

            "file://${mediaFile.absolutePath}"
        }.onFailure {
            Logger.e("Error saving ${mediaType.name.lowercase()} file: ${it.message}")
        }
    }

    /**
     * Gets the file URL for a saved media
     *
     * @param mediaId The ID of the media
     * @param mediaType The type of media (image or video)
     * @param extension Optional file extension to check (if null, defaults to jpg for images, mp4 for videos)
     * @return The file URI as a string, or null if the file doesn't exist
     */
    fun getMediaFileUrl(
        mediaId: Long,
        mediaType: MediaType = MediaType.IMAGE,
        extension: String? = null
    ): String? {
        val directory = when (mediaType) {
            MediaType.IMAGE -> imageDir
            MediaType.VIDEO -> videoDir
            MediaType.UNKNOWN -> mediaDir
        }

        if (extension != null) {
            val specificFile = File(directory, "$mediaId.$extension")
            if (specificFile.exists()) {
                return "file://${specificFile.absolutePath}"
            }
            return null
        }

        val defaultExt = when (mediaType) {
            MediaType.IMAGE -> "jpg"
            MediaType.VIDEO -> "mp4"
            MediaType.UNKNOWN -> null
        }

        if (defaultExt != null) {
            val defaultFile = File(directory, "$mediaId.$defaultExt")
            if (defaultFile.exists()) {
                return "file://${defaultFile.absolutePath}"
            }
        }

        if (mediaType == MediaType.UNKNOWN) {
            val extensions = imageExtensions + videoExtensions
            for (ext in extensions) {
                val file = File(directory, "$mediaId.$ext")
                if (file.exists()) {
                    return "file://${file.absolutePath}"
                }
            }
        }

        return null
    }

    /**
     * Checks if media exists for the given ID
     *
     * @param mediaId The ID of the media
     * @param mediaType The type of media to check for
     * @param extension Optional file extension to check (if null, checks default extensions)
     * @return True if the file exists, false otherwise
     */
    fun mediaExists(
        mediaId: Long,
        mediaType: MediaType = MediaType.UNKNOWN,
        extension: String? = null
    ): Boolean {
        val directories = when (mediaType) {
            MediaType.IMAGE -> listOf(imageDir)
            MediaType.VIDEO -> listOf(videoDir)
            MediaType.UNKNOWN -> listOf(imageDir, videoDir, mediaDir)
        }

        if (extension != null) {
            for (dir in directories) {
                if (File(dir, "$mediaId.$extension").exists()) {
                    return true
                }
            }
            return false
        }

        when (mediaType) {
            MediaType.IMAGE -> {
                if (File(imageDir, "$mediaId.jpg").exists()) {
                    return true
                }
            }
            MediaType.VIDEO -> {
                if (File(videoDir, "$mediaId.mp4").exists()) {
                    return true
                }
            }
            MediaType.UNKNOWN -> {
                if (File(imageDir, "$mediaId.jpg").exists() ||
                    File(videoDir, "$mediaId.mp4").exists()) {
                    return true
                }
            }
        }

        if (mediaType == MediaType.UNKNOWN) {
            val extensions = imageExtensions + videoExtensions
            for (dir in directories) {
                for (ext in extensions) {
                    if (File(dir, "$mediaId.$ext").exists()) {
                        return true
                    }
                }
            }
        }

        return false
    }

    /**
     * Gets the list of all saved media IDs of a specific type
     *
     * @param mediaType The type of media to list
     * @return List of media IDs that have been saved
     */
    fun getAllSavedMediaIds(mediaType: MediaType = MediaType.UNKNOWN): List<Long> {
        val directories = when (mediaType) {
            MediaType.IMAGE -> listOf(imageDir)
            MediaType.VIDEO -> listOf(videoDir)
            MediaType.UNKNOWN -> listOf(imageDir, videoDir, mediaDir)
        }

        return directories.flatMap { dir ->
            dir.listFiles()
                ?.filter { it.isFile }
                ?.mapNotNull { file ->
                    file.nameWithoutExtension.toLongOrNull()
                } ?: emptyList()
        }.distinct()
    }

    enum class MediaType {
        IMAGE, VIDEO, UNKNOWN
    }

    // I'm pretty sure Grindr defaults to both JPG and MP4 but let's keep this
    // flexible in case they change it in the future.
    private val imageExtensions = listOf("jpg", "jpeg", "png", "gif", "webp")
    private val videoExtensions = listOf("mp4", "mkv", "mov", "avi", "webm")
}

/**
 * Utility object specifically for expiring photos, which uses the more generic MediaUtils
 * underneath. This allows legacy code to keep working without changes.
 */
object ExpiringPhotoUtils {
    /**
     * Downloads an image from a URL using coroutines
     * @param url The URL of the image to download
     * @return Result containing the downloaded ByteArray or an exception on failure
     */
    suspend fun downloadImage(url: String): Result<ByteArray> =
        MediaUtils.downloadMedia(url)

    /**
     * Saves an image and returns its file URL
     *
     * @param mediaId The ID of the media
     * @param imageData The image data as ByteArray
     * @param extension Optional file extension (defaults to jpg)
     * @return Result containing the file URI as a string or an exception on failure
     */
    suspend fun saveImage(mediaId: Long, imageData: ByteArray, extension: String? = null): Result<String> =
        MediaUtils.saveMedia(mediaId, imageData, MediaUtils.MediaType.IMAGE, extension)

    /**
     * Gets the file URL for a saved image
     *
     * @param mediaId The ID of the media
     * @param extension Optional file extension (defaults to jpg)
     * @return The file URI as a string, or null if the file doesn't exist
     */
    fun getImageFileUrl(mediaId: Long, extension: String? = null): String? =
        MediaUtils.getMediaFileUrl(mediaId, MediaUtils.MediaType.IMAGE, extension)

    /**
     * Checks if an image file exists for the given media ID
     *
     * @param mediaId The ID of the media
     * @param extension Optional file extension (defaults to jpg)
     * @return True if the file exists, false otherwise
     */
    fun imageFileExists(mediaId: Long, extension: String? = null): Boolean =
        MediaUtils.mediaExists(mediaId, MediaUtils.MediaType.IMAGE, extension)

    /**
     * Gets the list of all saved image IDs
     *
     * @return List of media IDs that have been saved
     */
    fun getAllSavedImageIds(): List<Long> =
        MediaUtils.getAllSavedMediaIds(MediaUtils.MediaType.IMAGE)
}