package com.grindrplus.persistence.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.grindrplus.persistence.model.AlbumContentEntity
import com.grindrplus.persistence.model.AlbumEntity

@Dao
interface AlbumDao {

    @Query("SELECT * FROM AlbumEntity")
    suspend fun getAlbums(): List<AlbumEntity>

    @Query("SELECT * FROM AlbumEntity WHERE profileId = :profileId")
    suspend fun getAlbums(profileId: Long): List<AlbumEntity>

    @Query("SELECT * FROM AlbumEntity WHERE id = :id")
    suspend fun getAlbum(id: Long): AlbumEntity?

    @Upsert
    suspend fun upsertAlbum(album: AlbumEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAlbumFromAlbumBrief(albumEntity: AlbumEntity)

    @Query("SELECT * FROM AlbumContentEntity WHERE albumId = :albumId")
    suspend fun getAlbumContent(albumId: Long): List<AlbumContentEntity>

    @Upsert
    suspend fun upsertAlbumContent(dbAlbumContent: AlbumContentEntity)

    @Query("DELETE FROM AlbumEntity WHERE id = :id")
    suspend fun deleteAlbum(id: Long)
}