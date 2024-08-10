package com.grindrplus.core

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.io.File

class Database(context: Context, databasePath: String?) : SQLiteOpenHelper(
    context,
    databasePath ?: "grindrplus.db",
    null,
    1
) {
    private val lock = Any()

    override fun onCreate(db: SQLiteDatabase) {
        synchronized(lock) {
            tables.forEach { table ->
                db.execSQL(table.createStatement)
            }
        }
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        synchronized(lock) {
            tables.forEach { table ->
                db.execSQL("DROP TABLE IF EXISTS ${table.name}")
            }
            onCreate(db)
        }
    }

    fun deleteDatabase() {
        synchronized(lock) {
            writableDatabase.close()
            File(writableDatabase.path).delete()
        }
    }

    fun addPhoto(mediaId: Long, imageURL: String): Long {
        synchronized(lock) {
            val values = ContentValues().apply {
                put("mediaId", mediaId)
                put("imageURL", imageURL)
            }
            return writableDatabase.use { db ->
                tables.find { it.name == "ExpiringPhotos" }?.insert(db, values) ?: -1
            }
        }
    }

    fun getPhoto(mediaId: Long): String? {
        synchronized(lock) {
            return readableDatabase.use { db ->
                tables.find { it.name == "ExpiringPhotos" }?.query(
                    db,
                    arrayOf("imageURL"),
                    "mediaId = ?",
                    arrayOf(mediaId.toString())
                )?.getAsString("imageURL")
            }
        }
    }

    fun addLocation(name: String, latitude: Double, longitude: Double): Long {
        synchronized(lock) {
            val values = ContentValues().apply {
                put("name", name)
                put("latitude", latitude)
                put("longitude", longitude)
            }
            return writableDatabase.use { db ->
                tables.find { it.name == "TeleportLocations" }?.insert(db, values) ?: -1
            }
        }
    }

    fun updateLocation(name: String, latitude: Double, longitude: Double): Int {
        synchronized(lock) {
            val values = ContentValues().apply {
                put("latitude", latitude)
                put("longitude", longitude)
            }
            return writableDatabase.use { db ->
                tables.find { it.name == "TeleportLocations" }?.update(db, values, name) ?: -1
            }
        }
    }

    fun deleteLocation(name: String): Int {
        synchronized(lock) {
            return writableDatabase.use { db ->
                tables.find { it.name == "TeleportLocations" }?.delete(db, name) ?: -1
            }
        }
    }

    fun getLocation(name: String): Pair<Double, Double>? {
        synchronized(lock) {
            return readableDatabase.use { db ->
                tables.find { it.name == "TeleportLocations" }?.query(
                    db,
                    arrayOf("latitude", "longitude"),
                    "name = ?",
                    arrayOf(name)
                )?.let { values ->
                    Pair(values.getAsDouble("latitude"), values.getAsDouble("longitude"))
                }
            }
        }
    }

    fun getLocations(): List<Pair<String, Pair<Double, Double>>> {
        synchronized(lock) {
            return readableDatabase.use { db ->
                val locations = mutableListOf<Pair<String, Pair<Double, Double>>>()
                db.query(
                    "TeleportLocations",
                    arrayOf("name", "latitude", "longitude"),
                    null, null, null, null, null
                ).use { cursor ->
                    while (cursor.moveToNext()) {
                        locations.add(
                            Pair(
                                cursor.getString(0),
                                Pair(cursor.getDouble(1), cursor.getDouble(2))
                            )
                        )
                    }
                }
                locations
            }
        }
    }

    fun addPhrase(phraseId: Long, text: String, frequency: Int, timestamp: Long): Long {
        synchronized(lock) {
            val values = ContentValues().apply {
                put("phraseId", phraseId)
                put("text", text)
                put("frequency", frequency)
                put("timestamp", timestamp)
            }
            return writableDatabase.use { db ->
                tables.find { it.name == "SavedPhrases" }?.insert(db, values) ?: -1
            }
        }
    }

    fun updatePhrase(phraseId: Long, text: String, frequency: Int, timestamp: Long): Int {
        synchronized(lock) {
            val values = ContentValues().apply {
                put("text", text)
                put("frequency", frequency)
                put("timestamp", timestamp)
            }
            return writableDatabase.use { db ->
                tables.find { it.name == "SavedPhrases" }?.update(db, values, phraseId.toString())
                    ?: -1
            }
        }
    }

    fun deletePhrase(phraseId: Long): Int {
        synchronized(lock) {
            return writableDatabase.use { db ->
                tables.find { it.name == "SavedPhrases" }?.delete(db, phraseId.toString()) ?: -1
            }
        }
    }

    fun getPhrase(phraseId: Long): ContentValues {
        synchronized(lock) {
            return readableDatabase.use { db ->
                tables.find { it.name == "SavedPhrases" }?.query(
                    db,
                    arrayOf("phraseId", "text", "frequency", "timestamp"),
                    "phraseId = ?",
                    arrayOf(phraseId.toString())
                ) ?: ContentValues()
            }
        }
    }

    fun getCurrentPhraseIndex(): Long {
        synchronized(lock) {
            return readableDatabase.use { db ->
                db.query(
                    "SavedPhrases",
                    arrayOf("MAX(phraseId)"),
                    null, null,
                    null, null, null
                ).use { cursor ->
                    if (cursor.moveToFirst()) {
                        cursor.getLong(0)
                    } else {
                        -1
                    }
                }
            }
        }
    }

    fun getPhraseList(): List<ContentValues> {
        synchronized(lock) {
            return readableDatabase.use { db ->
                val phrases = mutableListOf<ContentValues>()
                db.query(
                    "SavedPhrases",
                    arrayOf("phraseId", "text", "frequency", "timestamp"),
                    null, null, null, null, null
                ).use { cursor ->
                    while (cursor.moveToNext()) {
                        val values = ContentValues()
                        values.put("phraseId", cursor.getLong(0))
                        values.put("text", cursor.getString(1))
                        values.put("frequency", cursor.getInt(2))
                        values.put("timestamp", cursor.getLong(3))
                        phrases.add(values)
                    }
                }
                phrases
            }
        }
    }

    data class Table(
        val name: String,
        val createStatement: String,
        val primaryKey: String
    ) {
        fun insert(db: SQLiteDatabase, values: ContentValues): Long {
            return db.insert(name, null, values)
        }

        fun update(db: SQLiteDatabase, values: ContentValues, primaryKeyValue: String): Int {
            return db.update(name, values, "$primaryKey = ?", arrayOf(primaryKeyValue))
        }

        fun delete(db: SQLiteDatabase, primaryKeyValue: String): Int {
            return db.delete(name, "$primaryKey = ?", arrayOf(primaryKeyValue))
        }

        fun query(
            db: SQLiteDatabase,
            columns: Array<String>,
            selection: String,
            selectionArgs: Array<String>
        ): ContentValues? {
            db.query(name, columns, selection, selectionArgs, null, null, null).use { cursor ->
                if (cursor.moveToFirst()) {
                    val values = ContentValues()
                    columns.forEach { column ->
                        val index = cursor.getColumnIndex(column)
                        if (index != -1) {
                            values.put(column, cursor.getString(index))
                        }
                    }
                    return values
                }
                return null
            }
        }
    }

    private val tables = listOf(
        Table(
            name = "ExpiringPhotos",
            createStatement = """
                CREATE TABLE IF NOT EXISTS ExpiringPhotos (
                    mediaId LONG PRIMARY KEY,
                    imageURL TEXT NOT NULL
                )
            """,
            primaryKey = "mediaId"
        ),
        Table(
            name = "TeleportLocations",
            createStatement = """
                CREATE TABLE IF NOT EXISTS TeleportLocations (
                    name TEXT PRIMARY KEY,
                    latitude DOUBLE NOT NULL,
                    longitude DOUBLE NOT NULL
                )
            """,
            primaryKey = "name"
        ),
        Table(
            name = "Albums",
            createStatement = """
                CREATE TABLE IF NOT EXISTS Albums (
                    contentId LONG PRIMARY KEY,
                    albumId LONG NOT NULL,
                    contentType TEXT NOT NULL,
                    url TEXT NOT NULL,
                    isProcessing BOOLEAN NOT NULL,
                    thumbUrl TEXT NOT NULL,
                    coverUrl TEXT NOT NULL,
                    remainingViews INT NOT NULL
                )
            """,
            primaryKey = "contentId"
        ),
        Table(
            name = "SavedPhrases",
            createStatement = """
                CREATE TABLE IF NOT EXISTS SavedPhrases (
                    phraseId LONG PRIMARY KEY,
                    text TEXT NOT NULL,
                    frequency INT NOT NULL,
                    timestamp LONG NOT NULL
                )
            """,
            primaryKey = "phraseId"
        )
    )
}
