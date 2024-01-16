package com.grindrplus.decorated.persistence.model

import com.grindrplus.Hooker
import com.grindrplus.core.Utils.getBooleanPreference
import de.robv.android.xposed.XposedHelpers.findConstructorExact
import de.robv.android.xposed.XposedHelpers.getLongField
import de.robv.android.xposed.XposedHelpers.getObjectField
import java.lang.reflect.Constructor

class AlbumContent(
    contentId: Long,
    contentType: String?,
    url: String?,
    isProcessing: Boolean,
    thumbUrl: String?,
    coverUrl: String?,
    remainingViews: Int
) {

    private val contentId: Long?
    private val contentType: String?
    private val url: String?
    private val isProcessing: Boolean?
    private val thumbUrl: String?
    private val coverUrl: String?
    private val remainingViews: Int?

    init {
        this.contentId = contentId
        this.contentType = contentType
        this.url = url
        this.isProcessing = isProcessing
        this.thumbUrl = thumbUrl
        this.coverUrl = coverUrl
        this.remainingViews = remainingViews
    }

    companion object {
        fun create(input: Any): Any {
            return if (input is AlbumContent) {
                val ctor = findConstructorExact(
                    "com.grindrapp.android.model.AlbumContent",
                    Hooker.pkgParam.classLoader,
                    Long::class.javaPrimitiveType,
                    String::class.java,
                    String::class.java,
                    Boolean::class.javaPrimitiveType,
                    String::class.java,
                    String::class.java,
                    Int::class.javaPrimitiveType
                ) as Constructor<*>

                ctor.newInstance(
                    input.contentId,
                    input.contentType,
                    input.url,
                    input.isProcessing,
                    input.thumbUrl,
                    input.coverUrl,
                    input.remainingViews
                )
            } else {
                AlbumContent(
                    getLongField(input, "contentId"),
                    getObjectField(input, "contentType") as String?,
                    getObjectField(input, "url") as String?,
                    getBooleanPreference("isProcessing"),
                    getObjectField(input, "thumbUrl") as String?,
                    getObjectField(input, "coverUrl") as String?,
                    getLongField(input, "remainingViews").toInt()
                )
            }
        }
    }
}
