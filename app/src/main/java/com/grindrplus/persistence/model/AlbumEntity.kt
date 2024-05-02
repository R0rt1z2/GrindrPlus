package com.grindrplus.persistence.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class AlbumEntity(
    @PrimaryKey val id: Long,
    val albumName: String?,
    val createdAt: String,
    val profileId: Long,
    val updatedAt: String,
)
