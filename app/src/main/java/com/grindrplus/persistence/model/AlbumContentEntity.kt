package com.grindrplus.persistence.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    foreignKeys = [
        ForeignKey(
            entity = AlbumEntity::class,
            parentColumns = ["id"],
            childColumns = ["albumId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class AlbumContentEntity(
    @PrimaryKey val id: Long,
    val albumId: Long,
    val contentType: String?,
    val coverUrl: String?,
    val thumbUrl: String?,
    val url: String?
)
