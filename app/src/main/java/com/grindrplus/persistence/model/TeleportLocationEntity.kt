package com.grindrplus.persistence.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class TeleportLocationEntity(
    @PrimaryKey val name: String,
    val latitude: Double,
    val longitude: Double
)