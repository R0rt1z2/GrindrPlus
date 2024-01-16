package com.grindrplus.core

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.grindrplus.decorated.persistence.model.AlbumContent

class GlobalCache(context: Context) {
    private val preferences: SharedPreferences = context.getSharedPreferences("album_cache", Context.MODE_PRIVATE)
    private val gson = Gson()
    private var albumContentsCache: MutableMap<Long, List<AlbumContent>> = mutableMapOf()

    init {
        loadFromPersistentStorage()
    }

    fun saveAlbumContents(albumId: Long, contents: List<AlbumContent>) {
        albumContentsCache[albumId] = contents
        saveToPersistentStorage()
    }

    fun getAlbumContents(albumId: Long): List<AlbumContent>? {
        return albumContentsCache[albumId]
    }

    fun clearCache() {
        albumContentsCache.clear()
        saveToPersistentStorage()
        preferences.edit().clear().apply()
    }

    private fun saveToPersistentStorage() {
        val typeToken = object : TypeToken<MutableMap<Long, List<AlbumContent>>>() {}.type
        val json = gson.toJson(albumContentsCache, typeToken)
        preferences.edit().putString("cache", json).apply()
    }

    private fun loadFromPersistentStorage() {
        val json = preferences.getString("cache", null)
        if (json != null) {
            val typeToken = object : TypeToken<MutableMap<Long, List<AlbumContent>>>() {}.type
            albumContentsCache = gson.fromJson<MutableMap<Long, List<AlbumContent>>>(json, typeToken)
        }
    }
}