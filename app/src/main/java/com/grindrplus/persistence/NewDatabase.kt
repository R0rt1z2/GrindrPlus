package com.grindrplus.persistence

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.grindrplus.persistence.dao.AlbumDao
import com.grindrplus.persistence.model.AlbumContentEntity
import com.grindrplus.persistence.model.AlbumEntity

@Database(
    entities = [
        AlbumEntity::class,
        AlbumContentEntity::class
    ],
    version = 1
)
abstract class NewDatabase : RoomDatabase() {
    abstract fun albumDao(): AlbumDao

    companion object {
        fun create(context: Context): NewDatabase {
            return Room.databaseBuilder(context, NewDatabase::class.java, "grindrplus")
                .fallbackToDestructiveMigration()
                .build()
        }
    }
}