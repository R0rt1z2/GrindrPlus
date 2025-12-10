package com.grindrplus.persistence.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class SavedPhraseEntity(
    @PrimaryKey val phraseId: Long,
    val text: String,
    val frequency: Int,
    val timestamp: Long
)