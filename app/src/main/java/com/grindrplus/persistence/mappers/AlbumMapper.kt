package com.grindrplus.persistence.mappers

import com.grindrplus.GrindrPlus
import com.grindrplus.core.Logger
import com.grindrplus.persistence.model.AlbumContentEntity
import com.grindrplus.persistence.model.AlbumEntity
import de.robv.android.xposed.XposedHelpers.getObjectField
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.lang.Long.parseLong

private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)

private const val ALBUM_CLASS = "com.grindrapp.android.model.Album"
private const val ALBUM_BRIEF_CLASS = "com.grindrapp.android.model.albums.AlbumBrief"
private const val ALBUM_CONTENT_CLASS = "com.grindrapp.android.model.AlbumContent"

fun Any.asAlbumToAlbumEntity(): AlbumEntity {
    return try {
        val id = getObjectField(this, "albumId") as Long
        val name = getObjectField(this, "albumName") as String?
        val profileId = getObjectField(this, "profileId") as Long
        val createdAt = getObjectField(this, "createdAt") as String
        val updatedAt = getObjectField(this, "updatedAt") as String

        AlbumEntity(
            id = id,
            albumName = name,
            createdAt = createdAt,
            profileId = profileId,
            updatedAt = updatedAt
        )
    } catch (e: Throwable) {
        Logger.e("Error converting Album to AlbumEntity: ${e.message}")
        val currentTime = dateFormat.format(Date())
        AlbumEntity(
            id = System.currentTimeMillis(),
            albumName = "Unknown Album",
            createdAt = currentTime,
            profileId = 0L,
            updatedAt = currentTime
        )
    }
}

fun Any.asAlbumBriefToAlbumEntity(): AlbumEntity {
    return try {
        val id = getObjectField(this, "albumId") as Long
        val profileIdObj = getObjectField(this, "profileId")

        val profileId = when (profileIdObj) {
            is String -> parseLong(profileIdObj)
            is Long -> profileIdObj
            else -> 0L
        }

        val name = try { getObjectField(this, "albumName") as String? } catch (_: Throwable) { null }
        val createdAt = try { getObjectField(this, "createdAt") as String } catch (_: Throwable) { "" }
        val updatedAt = try { getObjectField(this, "updatedAt") as String } catch (_: Throwable) { "" }

        AlbumEntity(
            id = id,
            albumName = name,
            createdAt = createdAt,
            profileId = profileId,
            updatedAt = updatedAt
        )
    } catch (e: Throwable) {
        Logger.e("Error converting AlbumBrief to AlbumEntity: ${e.message}")
        val currentTime = dateFormat.format(Date())
        AlbumEntity(
            id = System.currentTimeMillis(),
            albumName = "Unknown Album",
            createdAt = currentTime,
            profileId = 0L,
            updatedAt = currentTime
        )
    }
}

fun Any.toAlbumContentEntity(albumId: Long): AlbumContentEntity {
    return try {
        val id = getObjectField(this, "contentId") as Long
        val contentType = getObjectField(this, "contentType") as String?
        val coverUrl = getObjectField(this, "coverUrl") as String?
        val thumbUrl = getObjectField(this, "thumbUrl") as String?
        val url = getObjectField(this, "url") as String?

        AlbumContentEntity(
            id = id,
            albumId = albumId,
            contentType = contentType,
            coverUrl = coverUrl,
            thumbUrl = thumbUrl,
            url = url
        )
    } catch (e: Throwable) {
        Logger.e("Error converting AlbumContent to AlbumContentEntity: ${e.message}")
        AlbumContentEntity(
            id = System.currentTimeMillis() + albumId,
            albumId = albumId,
            contentType = "unknown",
            coverUrl = null,
            thumbUrl = null,
            url = null
        )
    }
}

fun AlbumEntity.toGrindrAlbum(dbContent: List<AlbumContentEntity>): Any {
    try {
        val albumConstructor = GrindrPlus.loadClass(ALBUM_CLASS).constructors.first()
        return albumConstructor.newInstance(
            id, // albumId
            profileId, // profileId
            0, // sharedCount
            dbContent.map { it.toGrindrAlbumContent() }, // content
            false, // isSelected
            false, // isPromoAlbum
            null, // promoAlbumName
            null, // promoAlbumProfileImage
            null, // promoAlbumData
            false, // hasUnseenContent
            true, // albumViewable
            true, // isShareable
            Long.MAX_VALUE, // viewableUntil
            albumName, // albumName
            0, // albumNumber
            0, // totalAlbumsShared
            null, // contentCount
            createdAt, // createdAt
            updatedAt, // updatedAt
            emptyList<Any>() // sharedWithProfileIds
        )
    } catch (e: Throwable) {
        Logger.e("Error creating Album instance: ${e.message}")
        Logger.writeRaw(e.stackTraceToString())

        try {
            val albumClass = GrindrPlus.loadClass(ALBUM_CLASS)
            val constructors = albumClass.constructors
            Logger.d("Available constructors for Album: ${constructors.size}")
            constructors.forEachIndexed { index, constructor ->
                Logger.d("Constructor $index: ${constructor.parameterTypes.joinToString()}")
            }
        } catch (e2: Throwable) {
            Logger.e("Failed to inspect Album constructors: ${e2.message}")
        }

        throw e
    }
}

fun AlbumEntity.toGrindrAlbumBrief(dbContent: AlbumContentEntity): Any {
    try {
        val albumBriefConstructor = GrindrPlus.loadClass(ALBUM_BRIEF_CLASS).constructors.first()
        return albumBriefConstructor.newInstance(
            id, // albumId
            profileId.toString(), // profileId
            dbContent.toGrindrAlbumContent(), // content
            false, // hasUnseenContent
            true, // albumViewable
            0, // albumNumber
            0, // totalAlbumsShared
            null // contentCount
        )
    } catch (e: Throwable) {
        Logger.e("Error creating AlbumBrief instance: ${e.message}")

        try {
            val albumBriefClass = GrindrPlus.loadClass(ALBUM_BRIEF_CLASS)
            val constructors = albumBriefClass.constructors
            Logger.d("Available constructors for AlbumBrief: ${constructors.size}")
            constructors.forEachIndexed { index, constructor ->
                Logger.d("Constructor $index: ${constructor.parameterTypes.joinToString()}")
            }
        } catch (e2: Throwable) {
            Logger.e("Failed to inspect AlbumBrief constructors: ${e2.message}")
        }

        throw e
    }
}

fun AlbumContentEntity.toGrindrAlbumContent(): Any {
    try {
        val albumContentConstructor = GrindrPlus.loadClass(ALBUM_CONTENT_CLASS).constructors.first()
        return albumContentConstructor.newInstance(
            id, // contentId
            contentType, // contentType
            url, // url
            false, // isProcessing
            thumbUrl, // thumbUrl
            coverUrl, // coverUrl
            -1 // remainingViews
        )
    } catch (e: Throwable) {
        Logger.e("Error creating AlbumContent instance: ${e.message}")

        try {
            val albumContentClass = GrindrPlus.loadClass(ALBUM_CONTENT_CLASS)
            val constructors = albumContentClass.constructors
            Logger.d("Available constructors for AlbumContent: ${constructors.size}")
            constructors.forEachIndexed { index, constructor ->
                Logger.d("Constructor $index: ${constructor.parameterTypes.joinToString()}")
            }
        } catch (e2: Throwable) {
            Logger.e("Failed to inspect AlbumContent constructors: ${e2.message}")
        }

        throw e
    }
}

fun AlbumEntity.toGrindrAlbumWithoutContent(): Any {
    try {
        val albumConstructor = GrindrPlus.loadClass(ALBUM_CLASS).constructors.first()
        return albumConstructor.newInstance(
            id, // albumId
            profileId, // profileId
            0, // sharedCount
            emptyList<Any>(), // content
            false, // isSelected
            false, // isPromoAlbum
            null, // promoAlbumName
            null, // promoAlbumProfileImage
            null, // promoAlbumData
            false, // hasUnseenContent
            true, // albumViewable
            true, // isShareable
            Long.MAX_VALUE, // viewableUntil
            albumName, // albumName
            0, // albumNumber
            0, // totalAlbumsShared
            null, // contentCount
            createdAt, // createdAt
            updatedAt, // updatedAt
            emptyList<Any>() // sharedWithProfileIds
        )
    } catch (e: Throwable) {
        Logger.e("Error creating Album instance without content: ${e.message}")
        Logger.writeRaw(e.stackTraceToString())
        throw e
    }
}
