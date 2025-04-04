package com.grindrplus.hooks

import androidx.room.withTransaction
import com.grindrplus.GrindrPlus
import com.grindrplus.core.Logger
import com.grindrplus.persistence.asAlbumBriefToAlbumEntity
import com.grindrplus.persistence.asAlbumToAlbumEntity
import com.grindrplus.persistence.model.AlbumContentEntity
import com.grindrplus.persistence.toAlbumContentEntity
import com.grindrplus.persistence.toGrindrAlbum
import com.grindrplus.persistence.toGrindrAlbumBrief
import com.grindrplus.persistence.toGrindrAlbumContent
import com.grindrplus.utils.Hook
import com.grindrplus.utils.RetrofitUtils
import com.grindrplus.utils.RetrofitUtils.createSuccess
import com.grindrplus.utils.RetrofitUtils.getSuccessValue
import com.grindrplus.utils.RetrofitUtils.isFail
import com.grindrplus.utils.RetrofitUtils.isGET
import com.grindrplus.utils.RetrofitUtils.isPUT
import com.grindrplus.utils.RetrofitUtils.isSuccess
import com.grindrplus.utils.withSuspendResult
import de.robv.android.xposed.XposedHelpers.getObjectField
import de.robv.android.xposed.XposedHelpers.setObjectField
import kotlinx.coroutines.runBlocking
import org.json.JSONObject

class UnlimitedAlbums : Hook(
    "Unlimited albums",
    "Allow to be able to view unlimited albums"
) {
    private val albumsService = "i4.a" // search for 'v1/albums/red-dot'

    override fun init() {
        val albumsService = findClass(albumsService)

        RetrofitUtils.hookService(
            albumsService,
        ) { originalHandler, proxy, method, args ->
            val result = originalHandler.invoke(proxy, method, args)
            when {
                method.isGET("v2/albums/{albumId}") -> handleGetAlbum(args, result)
                method.isGET("v1/albums") -> handleGetAlbums(args, result)
                method.isGET("v2/albums/shares") -> handleGetAlbumsShares(args, result)
                method.isGET("v2/albums/shares/{profileId}") -> handleGetAlbumsSharesProfileId(args, result)
                method.isPUT("v1/albums/{albumId}/shares/remove") -> handleRemoveAlbumShares(args, result)
                else -> result
            }
        }
    }

    private suspend fun saveAlbum(grindrAlbum: Any) {
        val dao = GrindrPlus.newDatabase.albumDao()

        val dbAlbum = grindrAlbum.asAlbumToAlbumEntity()
        dao.upsertAlbum(dbAlbum)
        val grindrAlbumContent = getObjectField(grindrAlbum, "content") as List<Any>
        grindrAlbumContent.forEach {
            val dbAlbumContent = it.toAlbumContentEntity(dbAlbum.id)
            dao.upsertAlbumContent(dbAlbumContent)
        }
    }

    private fun handleRemoveAlbumShares(args: Array<Any?>, result: Any) =
        withSuspendResult(args, result) { args, result ->
            val albumId = args[0] as Long
            if (result.isFail()) {
                runBlocking {
                    GrindrPlus.newDatabase.withTransaction {
                        val dao = GrindrPlus.newDatabase.albumDao()
                        val albumToDelete = dao.getAlbum(albumId)

                        if (albumToDelete != null) {
                            dao.deleteAlbum(albumId)
                            createSuccess(albumId)
                        } else {
                            Logger.e("Album with ID $albumId not found in the database")
                            result
                        }
                    }
                }
            }
            result
        }

    private fun handleGetAlbum(args: Array<Any?>, result: Any) =
        withSuspendResult(args, result) { args, result ->
            val albumId = args[0] as Long

            val res = GrindrPlus.httpClient.sendRequest(
                url = "https://grindr.mobi/v1/albums/$albumId",
                method = "GET"
            )

            val responseBody = res.body?.string()
            if (!responseBody.isNullOrEmpty()) {
                val jsonResponse = JSONObject(responseBody)

                val contentArray = jsonResponse.optJSONArray("content")
                if (contentArray != null) {
                    val albumContentEntities = mutableListOf<AlbumContentEntity>()

                    for (i in 0 until contentArray.length()) {
                        val contentJson = contentArray.getJSONObject(i)
                        val albumContentEntity = AlbumContentEntity(
                            id = contentJson.optLong("contentId"),
                            albumId = albumId,
                            contentType = contentJson.optString("contentType"),
                            coverUrl = contentJson.optString("coverUrl"),
                            thumbUrl = contentJson.optString("thumbUrl"),
                            url = contentJson.optString("url")
                        )
                        albumContentEntities.add(albumContentEntity)
                    }

                    val grindrAlbumContentList = albumContentEntities.map { it.toGrindrAlbumContent() }
                    val albumObject = getObjectField(result, "a")
                    setObjectField(albumObject, "content", grindrAlbumContentList)
                } else {
                    Logger.e("Failed to parse content array from response")
                }
            }

            result
        }

    private fun handleGetAlbums(args: Array<Any?>, result: Any) =
        withSuspendResult(args, result) { args, result ->
            if (result.isSuccess()) {
                val albums = getObjectField(result.getSuccessValue(), "albums") as List<Any>
                runBlocking {
                    GrindrPlus.newDatabase.withTransaction {
                        albums.forEach { album ->
                            saveAlbum(album)
                        }
                    }
                }
            }

            val albums = runBlocking {
                GrindrPlus.newDatabase.withTransaction {
                    val dao = GrindrPlus.newDatabase.albumDao()
                    val dbAlbums = dao.getAlbums()
                    dbAlbums.map {
                        val dbContent = dao.getAlbumContent(it.id)
                        it.toGrindrAlbum(dbContent)
                    }
                }
            }

            val newValue = findClass("com.grindrapp.android.model.AlbumsList")
                .getConstructor(List::class.java)
                .newInstance(albums)

            createSuccess(newValue)
        }

    private fun handleGetAlbumsShares(args: Array<Any?>, result: Any) =
        withSuspendResult(args, result) { args, result ->
            if (result.isSuccess()) {
                runBlocking {
                    GrindrPlus.newDatabase.withTransaction {
                        val dao = GrindrPlus.newDatabase.albumDao()
                        val albumBriefs =
                            getObjectField(result.getSuccessValue(), "albums") as List<Any>
                        albumBriefs.forEach { albumBrief ->
                            val albumEntity = albumBrief.asAlbumBriefToAlbumEntity()
                            dao.insertAlbumFromAlbumBrief(albumEntity)
                            val grindrAlbumContent = getObjectField(albumBrief, "content") as Any
                            val dbAlbumContent =
                                grindrAlbumContent.toAlbumContentEntity(albumEntity.id)
                            dao.upsertAlbumContent(dbAlbumContent)
                        }
                    }
                }
            }

            val albumBriefs = runBlocking {
                GrindrPlus.newDatabase.withTransaction {
                    val dao = GrindrPlus.newDatabase.albumDao()
                    val dbAlbums = dao.getAlbums()
                    dbAlbums.map {
                        val dbContent = dao.getAlbumContent(it.id)
                        it.toGrindrAlbumBrief(dbContent.first())
                    }
                }
            }

            val newValue = findClass("com.grindrapp.android.model.albums.SharedAlbumsBrief")
                .getConstructor(List::class.java)
                .newInstance(albumBriefs)

            createSuccess(newValue)
        }

    private fun handleGetAlbumsSharesProfileId(args: Array<Any?>, result: Any) =
        withSuspendResult(args, result) { args, result ->
            val profileId = args[0] as Long

            if (result.isSuccess()) {
                runBlocking {
                    GrindrPlus.newDatabase.withTransaction {
                        val dao = GrindrPlus.newDatabase.albumDao()
                        val albumBriefs =
                            getObjectField(result.getSuccessValue(), "albums") as List<Any>
                        albumBriefs.forEach { albumBrief ->
                            val albumEntity = albumBrief.asAlbumBriefToAlbumEntity()
                            dao.insertAlbumFromAlbumBrief(albumEntity)
                            val grindrAlbumContent = getObjectField(albumBrief, "content") as Any
                            val dbAlbumContent =
                                grindrAlbumContent.toAlbumContentEntity(albumEntity.id)
                            dao.upsertAlbumContent(dbAlbumContent)
                        }
                    }
                }
            }

            val albumBriefs = runBlocking {
                GrindrPlus.newDatabase.withTransaction {
                    val dao = GrindrPlus.newDatabase.albumDao()
                    val dbAlbums = dao.getAlbums(profileId)
                    dbAlbums.map {
                        val dbContent = dao.getAlbumContent(it.id)
                        it.toGrindrAlbumBrief(dbContent.first())
                    }
                }
            }

            val newValue = findClass("com.grindrapp.android.model.albums.SharedAlbumsBrief")
                .getConstructor(List::class.java)
                .newInstance(albumBriefs)

            createSuccess(newValue)
        }
}