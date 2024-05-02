package com.grindrplus.persistence

import com.grindrplus.GrindrPlus
import com.grindrplus.persistence.model.AlbumContentEntity
import com.grindrplus.persistence.model.AlbumEntity
import de.robv.android.xposed.XposedHelpers.getObjectField
import java.lang.Long.parseLong

fun Any.asAlbumToAlbumEntity(): AlbumEntity {
    return AlbumEntity(
        id = getObjectField(this, "albumId") as Long,
        albumName = getObjectField(this, "albumName") as String?,
        createdAt = getObjectField(this, "createdAt") as String,
        profileId = getObjectField(this, "profileId") as Long,
        updatedAt = getObjectField(this, "updatedAt") as String
    )
}

fun AlbumEntity.toGrindrAlbum(dbContent: List<AlbumContentEntity>): Any {
    val albumConstructor =
        GrindrPlus.loadClass("com.grindrapp.android.model.Album").constructors.first()
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
        albumName, // albumName
        0, // albumNumber
        0, // totalAlbumsShared
        null, // contentCount
        createdAt, // createdAt
        updatedAt, // updatedAt
        emptyList<Any>() // sharedWithProfileIds
    )
}

fun Any.asAlbumBriefToAlbumEntity(): AlbumEntity {
    return AlbumEntity(
        id = getObjectField(this, "albumId") as Long,
        albumName = null,
        createdAt = "",
        profileId = parseLong(getObjectField(this, "profileId") as String),
        updatedAt = ""
    )
}

fun AlbumEntity.toGrindrAlbumBrief(dbContent: AlbumContentEntity): Any {
    val albumBriefConstructor =
        GrindrPlus.loadClass("com.grindrapp.android.model.albums.AlbumBrief").constructors.first()
    return albumBriefConstructor.newInstance(
        id, // albumId
        profileId.toString(), // profileId,
        dbContent.toGrindrAlbumContent(), // content
        false, // hasUnseenContent
        true, // albumViewable
        0, // albumNumber
        0, // totalAlbumsShared
        null // contentCount
    )
}

fun Any.toAlbumContentEntity(albumId: Long): AlbumContentEntity {
    return AlbumContentEntity(
        id = getObjectField(this, "contentId") as Long,
        albumId = albumId,
        contentType = getObjectField(this, "contentType") as String?,
        coverUrl = getObjectField(this, "coverUrl") as String?,
        thumbUrl = getObjectField(this, "thumbUrl") as String?,
        url = getObjectField(this, "url") as String?
    )
}

fun AlbumContentEntity.toGrindrAlbumContent(): Any {
    val albumContentConstructor =
        GrindrPlus.loadClass("com.grindrapp.android.model.AlbumContent").constructors.first()
    return albumContentConstructor.newInstance(
        id, // contentId
        contentType, // contentType
        url, // url
        false, // isProcessing
        thumbUrl, // thumbUrl
        coverUrl, // coverUrl
        -1 // remainingViews
    )
}