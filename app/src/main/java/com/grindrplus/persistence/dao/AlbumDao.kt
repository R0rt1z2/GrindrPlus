package com.grindrplus.persistence.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.grindrplus.persistence.model.AlbumContentEntity
import com.grindrplus.persistence.model.AlbumEntity

@Dao
interface AlbumDao {

    /**
     * Gets all albums from the database
     * @return List of all albums
     */
    @Query("SELECT * FROM AlbumEntity ORDER BY updatedAt DESC")
    suspend fun getAlbums(): List<AlbumEntity>

    /**
     * Gets all albums for a specific profile
     * @param profileId The profile ID to filter by
     * @return List of albums belonging to the specified profile
     */
    @Query("SELECT * FROM AlbumEntity WHERE profileId = :profileId ORDER BY updatedAt DESC")
    suspend fun getAlbums(profileId: Long): List<AlbumEntity>

    /**
     * Gets a single album by ID
     * @param id The album ID
     * @return The album entity or null if not found
     */
    @Query("SELECT * FROM AlbumEntity WHERE id = :id")
    suspend fun getAlbum(id: Long): AlbumEntity?

    /**
     * Upserts (insert or update) an album
     * @param album The album entity to upsert
     */
    @Upsert
    suspend fun upsertAlbum(album: AlbumEntity)

    /**
     * Batch upsert for multiple albums
     * @param albums The list of album entities to upsert
     */
    @Upsert
    suspend fun upsertAlbums(albums: List<AlbumEntity>)

    /**
     * Inserts an album from album brief information, ignoring conflicts
     * @param albumEntity The album entity to insert
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAlbumFromAlbumBrief(albumEntity: AlbumEntity)

    /**
     * Gets all album content for a specific album
     * @param albumId The album ID
     * @return List of album content entities
     */
    @Query("SELECT * FROM AlbumContentEntity WHERE albumId = :albumId")
    suspend fun getAlbumContent(albumId: Long): List<AlbumContentEntity>

    /**
     * Upserts an album content entity
     * @param dbAlbumContent The album content entity to upsert
     */
    @Upsert
    suspend fun upsertAlbumContent(dbAlbumContent: AlbumContentEntity)

    /**
     * Batch upsert for multiple album content entities
     * @param contents The list of album content entities to upsert
     */
    @Upsert
    suspend fun upsertAlbumContents(contents: List<AlbumContentEntity>)

    /**
     * Deletes an album and its contents
     * @param id The album ID to delete
     */
    @Query("DELETE FROM AlbumEntity WHERE id = :id")
    suspend fun deleteAlbum(id: Long)

    /**
     * Checks if an album exists
     * @param id The album ID to check
     * @return True if the album exists, false otherwise
     */
    @Query("SELECT EXISTS(SELECT 1 FROM AlbumEntity WHERE id = :id)")
    suspend fun albumExists(id: Long): Boolean

    /**
     * Deletes all albums and their content for a profile
     * @param profileId The profile ID
     */
    @Query("DELETE FROM AlbumEntity WHERE profileId = :profileId")
    suspend fun deleteProfileAlbums(profileId: Long)

    /**
     * Complete transaction to save an album and its content in one atomic operation
     * @param album The album entity
     * @param contents The list of album content entities
     */
    @Transaction
    suspend fun saveAlbumWithContent(album: AlbumEntity, contents: List<AlbumContentEntity>) {
        upsertAlbum(album)
        upsertAlbumContents(contents)
    }
}