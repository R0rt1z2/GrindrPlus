package com.grindrplus.persistence

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.grindrplus.persistence.converters.DateConverter
import com.grindrplus.persistence.dao.AlbumDao
import com.grindrplus.persistence.model.AlbumContentEntity
import com.grindrplus.persistence.model.AlbumEntity

@Database(
    entities = [
        AlbumEntity::class,
        AlbumContentEntity::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(DateConverter::class)
abstract class NewDatabase : RoomDatabase() {
    abstract fun albumDao(): AlbumDao

    companion object {
        private const val DATABASE_NAME = "grindrplus.db"

        fun create(context: Context): NewDatabase {
            return Room.databaseBuilder(context, NewDatabase::class.java, DATABASE_NAME)
                .fallbackToDestructiveMigration(false)
                .setJournalMode(JournalMode.WRITE_AHEAD_LOGGING)
                .build()
        }
    }
}