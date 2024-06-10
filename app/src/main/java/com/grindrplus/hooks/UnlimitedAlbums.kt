package com.grindrplus.hooks

import androidx.room.withTransaction
import com.grindrplus.GrindrPlus
import com.grindrplus.persistence.asAlbumBriefToAlbumEntity
import com.grindrplus.persistence.asAlbumToAlbumEntity
import com.grindrplus.persistence.toAlbumContentEntity
import com.grindrplus.persistence.toGrindrAlbum
import com.grindrplus.persistence.toGrindrAlbumBrief
import com.grindrplus.utils.Hook
import com.grindrplus.utils.RetrofitUtils
import com.grindrplus.utils.RetrofitUtils.createSuccess
import com.grindrplus.utils.RetrofitUtils.getSuccessValue
import com.grindrplus.utils.RetrofitUtils.isGET
import com.grindrplus.utils.RetrofitUtils.isSuccess
import com.grindrplus.utils.withSuspendResult
import de.robv.android.xposed.XposedHelpers.getObjectField
import kotlinx.coroutines.runBlocking

class UnlimitedAlbums : Hook(
    "Unlimited albums",
    "Allow to be able to view unlimited albums"
) {
    private val albumsService = "w5.a"

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

    private fun handleGetAlbum(args: Array<Any?>, result: Any) =
        withSuspendResult(args, result) { args, result ->
            val albumId = args[0] as Long

            runBlocking {
                GrindrPlus.newDatabase.withTransaction {
                    if (result.isSuccess()) {
                        /**
                         * If the request was successful, we should add its
                         * content to the database.
                         */
                        saveAlbum(result.getSuccessValue())
                    }

                    val dao = GrindrPlus.newDatabase.albumDao()
                    val dbAlbum = dao.getAlbum(albumId)
                    if (dbAlbum != null) {
                        val dbContent = dao.getAlbumContent(dbAlbum.id)
                        createSuccess(dbAlbum.toGrindrAlbum(dbContent))
                    } else {
                        GrindrPlus.logger.log("UnlimitedAlbums: Album not found in database, returning original result")
                        result
                    }
                }
            }
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

            GrindrPlus.logger.log("UnlimitedAlbums: Handling getAlbumsSharesProfileId request (result: $result)")

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