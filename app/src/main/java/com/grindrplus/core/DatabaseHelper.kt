package com.grindrplus.core

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import com.grindrplus.GrindrPlus

object DatabaseHelper {
    private fun getDatabase(): SQLiteDatabase {
        val context = GrindrPlus.context
        val databases = context.databaseList()
        val grindrUserDb = databases.firstOrNull {
            it.contains("grindr_user") && it.endsWith(".db") }
            ?: throw IllegalStateException("No Grindr user database found!").also {
                Logger.apply {
                    e(it.message!!)
                    writeRaw("Available databases:\n" +
                            "${databases.joinToString("\n") { "  $it" }}\n")
                }
            }
        return context.openOrCreateDatabase(grindrUserDb.also {
            Logger.d("Using database: $it") }, Context.MODE_PRIVATE, null)
    }

    fun query(query: String, args: Array<String>? = null): List<Map<String, Any>> {
        val database = getDatabase()
        val cursor = database.rawQuery(query, args)
        val results = mutableListOf<Map<String, Any>>()

        try {
            if (cursor.moveToFirst()) {
                do {
                    val row = mutableMapOf<String, Any>()
                    cursor.columnNames.forEach { column ->
                        row[column] = when (cursor.getType(cursor.getColumnIndexOrThrow(column))) {
                            Cursor.FIELD_TYPE_INTEGER -> cursor.getInt(cursor.getColumnIndexOrThrow(column))
                            Cursor.FIELD_TYPE_FLOAT -> cursor.getFloat(cursor.getColumnIndexOrThrow(column))
                            Cursor.FIELD_TYPE_STRING -> cursor.getString(cursor.getColumnIndexOrThrow(column))
                            Cursor.FIELD_TYPE_BLOB -> cursor.getBlob(cursor.getColumnIndexOrThrow(column))
                            Cursor.FIELD_TYPE_NULL -> "NULL"
                            else -> "UNKNOWN"
                        }
                    }
                    results.add(row)
                } while (cursor.moveToNext())
            }
        } finally {
            cursor.close()
            database.close()
        }

        return results
    }

    fun insert(table: String, values: ContentValues): Long {
        val database = getDatabase()
        val rowId = database.insert(table, null, values)
        database.close()
        return rowId
    }

    fun update(table: String, values: ContentValues, whereClause: String?, whereArgs: Array<String>?): Int {
        val database = getDatabase()
        val rowsAffected = database.update(table, values, whereClause, whereArgs)
        database.close()
        return rowsAffected
    }

    fun delete(table: String, whereClause: String?, whereArgs: Array<String>?): Int {
        val database = getDatabase()
        val rowsDeleted = database.delete(table, whereClause, whereArgs)
        database.close()
        return rowsDeleted
    }

    fun execute(sql: String) {
        val database = getDatabase()
        database.execSQL(sql)
        database.close()
    }
}
