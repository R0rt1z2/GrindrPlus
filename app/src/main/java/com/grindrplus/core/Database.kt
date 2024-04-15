package com.grindrplus.core

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.io.File

class Database(context: Context, databasePath: String?) : SQLiteOpenHelper(
    context,
    databasePath ?: DATABASE_NAME,
    null,
    DATABASE_VERSION
) {
    companion object {
        private const val DATABASE_NAME = "grindrplus.db"
        private const val DATABASE_VERSION = 1
        private const val TABLE_EXPIRING_PHOTOS = "ExpiringPhotos"
        private const val MEDIA_ID = "mediaId"
        private const val IMAGE_URL = "imageURL"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createExpiringPhotosTable = """
            CREATE TABLE IF NOT EXISTS $TABLE_EXPIRING_PHOTOS (
                $MEDIA_ID LONG PRIMARY KEY,
                $IMAGE_URL TEXT NOT NULL
            )
        """
        db.execSQL(createExpiringPhotosTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_EXPIRING_PHOTOS")
        onCreate(db)
    }

    fun addPhoto(mediaId: Long, imageURL: String): Long {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(MEDIA_ID, mediaId)
            put(IMAGE_URL, imageURL)
        }
        val id = db.insert(TABLE_EXPIRING_PHOTOS, null, values)
        db.close()
        return id
    }

    fun updatePhoto(mediaId: Long, newImageUrl: String): Int {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(IMAGE_URL, newImageUrl)
        }
        val count = db.update(TABLE_EXPIRING_PHOTOS, values, "$MEDIA_ID = ?", arrayOf(mediaId.toString()))
        db.close()
        return count
    }

    fun deletePhoto(mediaId: Long): Int {
        val db = this.writableDatabase
        val count = db.delete(TABLE_EXPIRING_PHOTOS, "$MEDIA_ID = ?", arrayOf(mediaId.toString()))
        db.close()
        return count
    }

    fun getPhoto(mediaId: Long): String? {
        val db = this.readableDatabase
        db.query(
            TABLE_EXPIRING_PHOTOS,
            arrayOf(IMAGE_URL),
            "$MEDIA_ID = ?",
            arrayOf(mediaId.toString()),
            null,
            null,
            null
        ).use { cursor ->
            if (cursor.moveToFirst()) {
                val columnIndex = cursor.getColumnIndex(IMAGE_URL)
                if (columnIndex != -1) {
                    return cursor.getString(columnIndex)
                }
            }
        }
        db.close()
        return null
    }
}
