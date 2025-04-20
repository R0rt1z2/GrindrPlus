package com.grindrplus.hooks

import com.grindrplus.GrindrPlus
import com.grindrplus.persistence.model.ExpiringPhotoEntity
import com.grindrplus.utils.Hook
import com.grindrplus.utils.HookStage
import com.grindrplus.utils.hook
import de.robv.android.xposed.XposedHelpers.getObjectField
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ExpiringPhotos : Hook(
    "Expiring photos",
    "Allow unlimited photo viewing"
) {
    private val expiringImageBody = "com.grindrapp.android.chat.model.ExpiringImageBody"
    private val expiringImageBodyUiData =
        "com.grindrapp.android.chat.presentation.model.BodyUiData\$ExpiringImageBodyUiData"
    private val expiringStatusResponse =
        "com.grindrapp.android.chat.api.model.ExpiringPhotoStatusResponse"

    private val coroutineScope = CoroutineScope(Dispatchers.Main)

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
                val url = getObjectField(param.thisObject(), "url")?.toString()

                coroutineScope.launch {
                    if (url != null) {
                        val existingPhoto = getPhoto(mediaId)
                        if (existingPhoto == null) {
                            addPhoto(mediaId, url)
                        }
                    } else {
                        val photoUrl = getPhoto(mediaId)
                        if (photoUrl != null) {
                            param.setResult(photoUrl)
                        }
                    }
                }
            }
    }

    private suspend fun getPhoto(mediaId: Long): String? = withContext(Dispatchers.IO) {
        val photoDao = GrindrPlus.database.expiringPhotoDao()
        val photo = photoDao.getPhoto(mediaId)
        return@withContext photo?.imageURL
    }

    private suspend fun addPhoto(mediaId: Long, imageURL: String) = withContext(Dispatchers.IO) {
        val photoDao = GrindrPlus.database.expiringPhotoDao()
        val photo = ExpiringPhotoEntity(
            mediaId = mediaId,
            imageURL = imageURL
        )
        photoDao.addPhoto(photo)
    }
}