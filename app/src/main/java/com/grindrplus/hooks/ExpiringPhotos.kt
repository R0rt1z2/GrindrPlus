package com.grindrplus.hooks

import com.grindrplus.GrindrPlus
import com.grindrplus.core.Logger
import com.grindrplus.persistence.model.ExpiringPhotoEntity
import com.grindrplus.persistence.utils.ExpiringPhotoUtils.createTmpFile
import com.grindrplus.persistence.utils.ExpiringPhotoUtils.downloadImageAsByteArray
import com.grindrplus.persistence.utils.ExpiringPhotoUtils.getTmpFileUrl
import com.grindrplus.utils.Hook
import com.grindrplus.utils.HookStage
import com.grindrplus.utils.hook
import de.robv.android.xposed.XposedHelpers.getObjectField
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.ConcurrentHashMap

class ExpiringPhotos : Hook(
    "Expiring photos",
    "Allow unlimited photo viewing and save photos"
) {
    private val expiringImageBody = "com.grindrapp.android.chat.model.ExpiringImageBody"
    private val expiringImageBodyUiData =
        "com.grindrapp.android.chat.presentation.model.BodyUiData\$ExpiringImageBodyUiData"
    private val expiringStatusResponse =
        "com.grindrapp.android.chat.api.model.ExpiringPhotoStatusResponse"

    private val coroutineScope = CoroutineScope(Dispatchers.Main)
    private val filePathCache = ConcurrentHashMap<Long, String>()
    private val pendingDeletions = ConcurrentHashMap<Long, Boolean>()
    private val deletionDelay = 10000L

    override fun init() {
        findClass(expiringImageBodyUiData)
            .hook("hasViewsRemaining", HookStage.BEFORE) { param ->
                param.setResult(true)
            }

        findClass(expiringImageBody)
            .hook("getDuration", HookStage.BEFORE) { param ->
                param.setResult(Long.MAX_VALUE)
            }

        findClass(expiringImageBody)
            .hook("getViewsRemaining", HookStage.BEFORE) { param ->
                param.setResult(Int.MAX_VALUE)
            }

        findClass(expiringStatusResponse)
            .hook("getAvailable", HookStage.BEFORE) { param ->
                param.setResult(Int.MAX_VALUE)
            }

        findClass(expiringStatusResponse)
            .hook("getTotal", HookStage.BEFORE) { param ->
                param.setResult(Int.MAX_VALUE)
            }

        findClass(expiringImageBody)
            .hook("getUrl", HookStage.AFTER) { param ->
                val mediaId = getObjectField(param.thisObject(), "mediaId") as Long
                val originalUrl = getObjectField(param.thisObject(), "url")?.toString()

                val cachedPath = filePathCache[mediaId]
                if (cachedPath != null) {
                    Logger.d("Using cached file path: $cachedPath")
                    param.setResult(cachedPath)
                    scheduleDeletion(mediaId)
                    return@hook
                }

                val existingTempPath = getTmpFileUrl(mediaId)
                if (existingTempPath != null) {
                    Logger.d("Using existing temp file: $existingTempPath")
                    filePathCache[mediaId] = existingTempPath
                    param.setResult(existingTempPath)
                    scheduleDeletion(mediaId)
                    return@hook
                }

                coroutineScope.launch {
                    try {
                        if (originalUrl != null && !originalUrl.startsWith("file://")) {
                            Logger.d("Downloading image from URL: ${originalUrl.take(50)}...")
                            val imageData = withContext(Dispatchers.IO) {
                                downloadImageAsByteArray(originalUrl)
                            }

                            if (imageData != null) {
                                addPhoto(mediaId, imageData)
                                Logger.i("Image downloaded and saved to database: ${imageData.size} bytes")

                                val filePath = createTmpFile(mediaId, imageData)
                                if (filePath != null) {
                                    filePathCache[mediaId] = filePath
                                    param.setResult(filePath)
                                    scheduleDeletion(mediaId)
                                }
                            }
                        } else if (originalUrl == null || originalUrl.isEmpty()) {
                            Logger.w("URL is null/empty, trying to retrieve from database for mediaId: $mediaId")
                            val photoData = getPhotoData(mediaId)

                            if (photoData != null) {
                                Logger.i("Found photo in database: ${photoData.size} bytes")

                                val filePath = createTmpFile(mediaId, photoData)
                                if (filePath != null) {
                                    filePathCache[mediaId] = filePath
                                    param.setResult(filePath)
                                    scheduleDeletion(mediaId)
                                }
                            } else {
                                Logger.w("Photo not found in database for mediaId: $mediaId")
                            }
                        }
                    } catch (e: Exception) {
                        Logger.e("Error processing expiring photo: ${e.message}")
                        Logger.writeRaw(e.stackTraceToString())
                    }
                }
            }
    }

    private suspend fun getPhotoData(mediaId: Long): ByteArray? = withContext(Dispatchers.IO) {
        val photoDao = GrindrPlus.database.expiringPhotoDao()
        val photo = photoDao.getPhoto(mediaId)
        return@withContext photo?.imageData
    }

    private suspend fun addPhoto(mediaId: Long, imageData: ByteArray) = withContext(Dispatchers.IO) {
        val photoDao = GrindrPlus.database.expiringPhotoDao()
        val photo = ExpiringPhotoEntity(
            mediaId = mediaId,
            imageData = imageData
        )
        photoDao.addPhoto(photo)
    }

    // TODO: Do better than this. Probably hook Fresco's ImagePipeline and feed it with the
    //       image data directly. This is a hack to make it work for now.
    private fun scheduleDeletion(mediaId: Long) {
        if (pendingDeletions.putIfAbsent(mediaId, true) == null) {
            coroutineScope.launch {
                try {
                    delay(deletionDelay)
                    val filePath = filePathCache[mediaId]
                    if (filePath != null && filePath.startsWith("file://")) {
                        val actualPath = filePath.substring(7)
                        val file = File(actualPath)
                        if (file.exists()) {
                            val deleted = file.delete()
                            Logger.d("Deleted temp file for mediaId $mediaId after ${deletionDelay}ms: $deleted")
                        }
                        filePathCache.remove(mediaId)
                    }
                } catch (e: Exception) {
                    Logger.e("Error during scheduled deletion: ${e.message}")
                } finally {
                    pendingDeletions.remove(mediaId)
                }
            }

            Logger.d("Scheduled deletion of temp file for mediaId $mediaId in ${deletionDelay}ms")
        }
    }
}