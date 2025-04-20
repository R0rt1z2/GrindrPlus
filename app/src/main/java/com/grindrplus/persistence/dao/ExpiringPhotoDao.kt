package com.grindrplus.persistence.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.grindrplus.persistence.model.ExpiringPhotoEntity

@Dao
interface ExpiringPhotoDao {
    /**
     * Get a photo by media ID
     * @param mediaId The media ID of the photo
     * @return The photo entity or null if not found
     */
    @Query("SELECT * FROM ExpiringPhotoEntity WHERE mediaId = :mediaId")
    suspend fun getPhoto(mediaId: Long): ExpiringPhotoEntity?

    /**
     * Insert a new photo. If a photo with the same
     * media ID already exists, it will be replaced.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addPhoto(photo: ExpiringPhotoEntity): Long

    /**
     * Delete a photo
     * @param mediaId The media ID of the photo to delete
     */
    @Query("DELETE FROM ExpiringPhotoEntity WHERE mediaId = :mediaId")
    suspend fun deletePhoto(mediaId: Long): Int
}