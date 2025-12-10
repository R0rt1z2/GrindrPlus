package com.grindrplus.persistence.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.grindrplus.persistence.model.TeleportLocationEntity

@Dao
interface TeleportLocationDao {
    /**
     * Get all teleport locations
     * @return A list of teleport locations
     */
    @Query("SELECT * FROM TeleportLocationEntity")
    suspend fun getLocations(): List<TeleportLocationEntity>

    /**
     * Get a location by name
     * @param name The name of the location
     * @return The location entity or null if not found
     */
    @Query("SELECT * FROM TeleportLocationEntity WHERE name = :name")
    suspend fun getLocation(name: String): TeleportLocationEntity?

    /**
     * Add a new location
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addLocation(location: TeleportLocationEntity): Long

    /**
     * Update an existing location
     */
    @Upsert
    suspend fun upsertLocation(location: TeleportLocationEntity)

    /**
     * Delete a location
     */
    @Query("DELETE FROM TeleportLocationEntity WHERE name = :name")
    suspend fun deleteLocation(name: String): Int
}

