package com.grindrplus.hooks

import com.grindrplus.GrindrPlus
import com.grindrplus.utils.Hook
import com.grindrplus.utils.HookStage
import com.grindrplus.utils.hook
import de.robv.android.xposed.XposedHelpers.getObjectField

class ExpiringPhotos: Hook("Expiring photos",
    "Allow unlimited photo viewing") {
    private val expiringImageBody = "com.grindrapp.android.model.ExpiringImageBody"
    private val expiringImageBodyUiData = "com.grindrapp.android.ui.chat.model.BodyUiData\$ExpiringImageBodyUiData"
    private val expiringStatusResponse = "com.grindrapp.android.chat.api.model.ExpiringPhotoStatusResponse"

    override fun init() {
        findClass(expiringImageBodyUiData)
            ?.hook("hasViewsRemaining", HookStage.AFTER) { param ->
                param.setResult(true)
            }

        findClass(expiringImageBody)
            ?.hook("getDuration", HookStage.AFTER) { param ->
                param.setResult(Long.MAX_VALUE)
            }

        findClass(expiringImageBody)
            ?.hook("getViewsRemaining", HookStage.AFTER) { param ->
                param.setResult(Int.MAX_VALUE)
            }

        findClass(expiringStatusResponse)
            ?.hook("getAvailable", HookStage.AFTER) { param ->
                param.setResult(Int.MAX_VALUE)
            }

        findClass(expiringStatusResponse)
            ?.hook("getTotal", HookStage.AFTER) { param ->
                param.setResult(Int.MAX_VALUE)
            }

        findClass(expiringImageBody)
            ?.hook("getUrl", HookStage.AFTER) { param ->
                val mediaId = getObjectField(param.thisObject(), "mediaId") as Long
                val url = getObjectField(param.thisObject(), "url")?.toString()

                if (url != null) {
                    if (GrindrPlus.database.getPhoto(mediaId) == null) {
                        GrindrPlus.database.addPhoto(mediaId, url)
                    }
                } else {
                    param.setResult(GrindrPlus.database.getPhoto(mediaId))
                }
            }
    }
}