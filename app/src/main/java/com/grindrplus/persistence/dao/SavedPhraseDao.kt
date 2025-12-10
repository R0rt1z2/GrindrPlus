package com.grindrplus.persistence.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.grindrplus.persistence.model.SavedPhraseEntity

@Dao
interface SavedPhraseDao {
    /**
     * Get all saved phrases
     * @return A list of saved phrases, ordered by frequency and timestamp
     */
    @Query("SELECT * FROM SavedPhraseEntity ORDER BY frequency DESC, timestamp DESC")
    suspend fun getPhraseList(): List<SavedPhraseEntity>

    /**
     * Get a phrase by ID
     * @param phraseId The ID of the phrase
     */
    @Query("SELECT * FROM SavedPhraseEntity WHERE phraseId = :phraseId")
    suspend fun getPhrase(phraseId: Long): SavedPhraseEntity?

    /**
     * Add a new phrase
     * @param phrase The phrase entity to add
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addPhrase(phrase: SavedPhraseEntity): Long

    /**
     * Update an existing phrase
     * @param phrase The phrase entity to update
     */
    @Upsert
    suspend fun upsertPhrase(phrase: SavedPhraseEntity)

    /**
     * Delete a phrase
     * @param phraseId The ID of the phrase to delete
     */
    @Query("DELETE FROM SavedPhraseEntity WHERE phraseId = :phraseId")
    suspend fun deletePhrase(phraseId: Long): Int

    /**
     * Get the highest phrase ID
     * @return The highest phrase ID or null if no phrases exist
     */
    @Query("SELECT MAX(phraseId) FROM SavedPhraseEntity")
    suspend fun getCurrentPhraseIndex(): Long?
}