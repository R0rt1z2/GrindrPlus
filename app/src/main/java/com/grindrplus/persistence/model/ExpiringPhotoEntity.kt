package com.grindrplus.persistence.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class ExpiringPhotoEntity(
    @PrimaryKey val mediaId: Long,
    val imageURL: String
)