package com.grindrplus.persistence

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.grindrplus.persistence.converters.DateConverter
import com.grindrplus.persistence.dao.AlbumDao
import com.grindrplus.persistence.dao.SavedPhraseDao
import com.grindrplus.persistence.dao.TeleportLocationDao
import com.grindrplus.persistence.model.AlbumContentEntity
import com.grindrplus.persistence.model.AlbumEntity
import com.grindrplus.persistence.model.SavedPhraseEntity
import com.grindrplus.persistence.model.TeleportLocationEntity

@Database(
    entities = [
        AlbumEntity::class,
        AlbumContentEntity::class,
        TeleportLocationEntity::class,
        SavedPhraseEntity::class
    ],
    version = 5,
    exportSchema = false
)
@TypeConverters(DateConverter::class)
abstract class GPDatabase : RoomDatabase() {
    abstract fun albumDao(): AlbumDao
    abstract fun teleportLocationDao(): TeleportLocationDao
    abstract fun savedPhraseDao(): SavedPhraseDao

    companion object {
        private const val DATABASE_NAME = "grindrplus.db"

        fun create(context: Context): GPDatabase {
            return Room.databaseBuilder(context, GPDatabase::class.java, DATABASE_NAME)
                .fallbackToDestructiveMigration(false)
                .setJournalMode(JournalMode.WRITE_AHEAD_LOGGING)
                .build()
        }
    }
}