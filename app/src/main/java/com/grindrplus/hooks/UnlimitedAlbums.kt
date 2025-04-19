package com.grindrplus.hooks

import android.widget.Toast
import com.grindrplus.GrindrPlus
import com.grindrplus.core.Logger
import com.grindrplus.persistence.model.AlbumContentEntity
import com.grindrplus.persistence.toGrindrAlbumContent
import com.grindrplus.utils.Hook
import com.grindrplus.utils.HookStage
import com.grindrplus.utils.RetrofitUtils
import com.grindrplus.utils.RetrofitUtils.isGET
import com.grindrplus.utils.hookConstructor
import com.grindrplus.utils.withSuspendResult
import de.robv.android.xposed.XposedHelpers.getObjectField
import de.robv.android.xposed.XposedHelpers.setObjectField
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

class UnlimitedAlbums : Hook(
    "Unlimited albums",
    "Allow to be able to view unlimited albums"
) {
    private val albumsService = "w4.a" // search for 'v1/albums/red-dot'
    private val albumModel = "com.grindrapp.android.model.Album"

    override fun init() {
        val albumsService = findClass(albumsService)

        RetrofitUtils.hookService(
            albumsService,
        ) { originalHandler, proxy, method, args ->
            val result = originalHandler.invoke(proxy, method, args)
            when {
                method.isGET("v2/albums/{albumId}") -> handleGetAlbum(args, result)
                else -> result
            }
        }

        findClass(albumModel).hookConstructor(HookStage.AFTER) { param ->
            setObjectField(param.thisObject(), "albumViewable", true)
            setObjectField(param.thisObject(), "viewableUntil", Long.MAX_VALUE)
        }
    }

    private fun handleGetAlbum(args: Array<Any?>, result: Any) =
        withSuspendResult(args, result) { _, result ->
            val albumId = args[0] as Long

            try {
                val res = GrindrPlus.httpClient.sendRequest(
                    url = "https://grindr.mobi/v1/albums/$albumId",
                    method = "GET"
                )

                res.body?.string()?.let { responseBody ->
                    parseAlbumContent(albumId, responseBody, result)
                }
            } catch (e: Exception) {
                Logger.e("Failed to fetch album $albumId: ${e.message}")
                Logger.writeRaw(e.stackTraceToString())
                GrindrPlus.showToast(Toast.LENGTH_LONG, "Failed to load album")
            }

            result
        }

    private fun parseAlbumContent(albumId: Long, responseBody: String, result: Any) {
        try {
            val jsonResponse = JSONObject(responseBody)
            jsonResponse.optJSONArray("content")?.let { contentArray ->
                val albumContentEntities = (0 until contentArray.length()).mapNotNull { i ->
                    val contentJson = contentArray.getJSONObject(i)
                    AlbumContentEntity(
                        id = contentJson.optLong("contentId"),
                        albumId = albumId,
                        contentType = contentJson.optString("contentType"),
                        coverUrl = contentJson.optString("coverUrl"),
                        thumbUrl = contentJson.optString("thumbUrl"),
                        url = contentJson.optString("url")
                    )
                }

                getObjectField(result, "a")?.also { albumObject ->
                    setObjectField(albumObject, "content", albumContentEntities.map { it.toGrindrAlbumContent() })
                } ?: Logger.e("Album object not found in result for album ID: $albumId")
            } ?: run {
                Logger.e("Failed to parse content array for album ID: $albumId")
                GrindrPlus.showToast(Toast.LENGTH_LONG, "Failed to parse album content")
            }
        } catch (e: Exception) {
            Logger.e("Error parsing album content: ${e.message}")
            Logger.writeRaw(e.stackTraceToString())
        }
    }
}