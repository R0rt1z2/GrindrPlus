package com.grindrplus.core.http

import android.content.ContentValues
import android.widget.Toast
import com.grindrplus.GrindrPlus
import com.grindrplus.GrindrPlus.showToast
import com.grindrplus.core.DatabaseHelper
import com.grindrplus.core.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class Client(interceptor: Interceptor) {
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(interceptor)
        .build()

    fun sendRequest(
        url: String,
        method: String = "GET",
        headers: Map<String, String>? = null,
        body: RequestBody? = null
    ): Response {
        val requestBuilder = Request.Builder().url(url)
        headers?.forEach { (key, value) ->
            requestBuilder.addHeader(key, value)
        }

        when (method.uppercase()) {
            "POST" -> requestBuilder.post(body ?: RequestBody.createEmpty())
            "PUT" -> requestBuilder.put(body ?: throw IllegalArgumentException("PUT requires a body"))
            "DELETE" -> {
                if (body != null) requestBuilder.delete(body) else requestBuilder.delete()
            }
            "PATCH" -> requestBuilder.patch(body ?: throw IllegalArgumentException("PATCH requires a body"))
            "GET" -> requestBuilder.get()
            else -> throw IllegalArgumentException("Unsupported HTTP method: $method")
        }

        return httpClient.newCall(requestBuilder.build()).execute()
    }

    fun blockUser(profileId: String, silent: Boolean = false, reflectInDb: Boolean = true) {
        GrindrPlus.shouldTriggerAntiblock = false
        GrindrPlus.executeAsync {
            val response = sendRequest(
                "https://grindr.mobi/v3/me/blocks/$profileId",
                "POST"
            )
            if (response.isSuccessful) {
                if (!silent) showToast(Toast.LENGTH_LONG, "User blocked successfully")
                if (reflectInDb) {
                    val order = DatabaseHelper.query(
                        "SELECT MAX(order_) AS order_ FROM blocks"
                    ).firstOrNull()?.get("order_") as? Int ?: 0
                    DatabaseHelper.insert(
                        "blocks",
                        ContentValues().apply {
                            put("profileId", profileId)
                            put("order_", order + 1)
                        }
                    )
                }
            } else {
                if (!silent) {
                    showToast(
                        Toast.LENGTH_LONG,
                        "Failed to block user: ${response.body?.string()}"
                    )
                }
            }
        }
        GrindrPlus.executeAsync {
            Thread.sleep(500) // Wait for WS to reply
            GrindrPlus.shouldTriggerAntiblock = true
        }
    }

    fun unblockUser(profileId: String, silent: Boolean = false, reflectInDb: Boolean = true) {
        GrindrPlus.shouldTriggerAntiblock = false
        GrindrPlus.executeAsync {
            val response = sendRequest(
                "https://grindr.mobi/v3/me/blocks/$profileId",
                "DELETE"
            )
            if (response.isSuccessful) {
                if (!silent) showToast(Toast.LENGTH_LONG, "User unblocked successfully")
                try {
                    if (reflectInDb) {
                        DatabaseHelper.delete(
                            "blocks",
                            "profileId = ?",
                            arrayOf(profileId)
                        )
                    }
                } catch (e: Exception) {
                    Logger.apply {
                        e("Error removing user from blocks list: ${e.message}")
                        writeRaw(e.stackTraceToString())
                    }
                }
            } else {
                if (!silent) {
                    showToast(
                        Toast.LENGTH_LONG,
                        "Failed to unblock user: ${response.body?.string()}"
                    )
                }
            }
        }
        GrindrPlus.executeAsync {
            Thread.sleep(500) // Wait for WS to reply
            GrindrPlus.shouldTriggerAntiblock = true
        }
    }

    fun favorite(
        profileId: String,
        silent: Boolean = false,
        reflectInDb: Boolean = true
    ) {
        GrindrPlus.executeAsync {
            val response = sendRequest(
                "https://grindr.mobi/v3/me/favorites/$profileId",
                "POST"
            )
            if (response.isSuccessful) {
                if (!silent) showToast(Toast.LENGTH_LONG, "User favorited successfully")
                if (reflectInDb) {
                    DatabaseHelper.insert(
                        "favorite_profile",
                        ContentValues().apply {
                            put("id", profileId)
                        }
                    )
                }
            } else {
                if (!silent) {
                    showToast(
                        Toast.LENGTH_LONG,
                        "Failed to favorite user: ${response.body?.string()}"
                    )
                }
            }
        }
    }

    fun unfavorite(
        profileId: String,
        silent: Boolean = false,
        reflectInDb: Boolean = true
    ) {
        GrindrPlus.executeAsync {
            val response = sendRequest(
                "https://grindr.mobi/v3/me/favorites/$profileId",
                "DELETE"
            )
            if (response.isSuccessful) {
                if (!silent) showToast(Toast.LENGTH_LONG, "User unfavorited successfully")
                try {
                    if (reflectInDb) {
                        DatabaseHelper.delete(
                            "favorite_profile",
                            "id = ?",
                            arrayOf(profileId)
                        )
                    }
                } catch (e: Exception) {
                    Logger.apply {
                        e("Error removing user from favorites list: ${e.message}")
                        writeRaw(e.stackTraceToString())
                    }
                }
            } else {
                if (!silent) {
                    showToast(
                        Toast.LENGTH_LONG,
                        "Failed to unfavorite user: ${response.body?.string()}"
                    )
                }
            }
        }
    }

    fun reportUser(
        profileId: String,
        reason: String = "SPAM",
        comment: String = ""
    ) {
        GrindrPlus.executeAsync {
            val body = """
                {
                    "reason": "$reason",
                    "comment": "$comment",
                    "locations": [
                        "CHAT_MESSAGE"
                    ]
                }
            """.trimIndent()

            val response = sendRequest(
                "https://grindr.mobi/v3.1/flags/$profileId",
                "POST",
                body = body.toRequestBody()
            )

            if (response.isSuccessful) {
                showToast(Toast.LENGTH_LONG, "User reported successfully")
            } else {
                showToast(
                    Toast.LENGTH_LONG,
                    "Failed to report user: ${response.body?.string()}"
                )
            }
        }
    }

    fun updateSettings(settings: String) {
        GrindrPlus.executeAsync {
            sendRequest(
                "https://grindr.mobi/v3/me/prefs/settings",
                "PUT",
                body = settings.toRequestBody()
            )
        }
    }

    suspend fun getBlocks(): List<String> = withContext(Dispatchers.IO) {
        try {
            val response = sendRequest("https://grindr.mobi/v3.1/me/blocks", "GET")
            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                if (!responseBody.isNullOrEmpty()) {
                    val jsonResponse = JSONObject(responseBody)
                    val blockingArray = jsonResponse.optJSONArray("blocking")
                    if (blockingArray != null) {
                        val blockedProfileIds = mutableListOf<String>()
                        for (i in 0 until blockingArray.length()) {
                            val blockingJson = blockingArray.getJSONObject(i)
                            val profileId = blockingJson.optLong("profileId")
                            blockedProfileIds.add(profileId.toString())
                        }
                        return@withContext blockedProfileIds
                    }
                }
            }
            emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun getFavorites(): List<Triple<String, String, String>> = withContext(Dispatchers.IO) {
        try {
            val response = sendRequest("https://grindr.mobi/v6/favorites", "GET")
            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                if (!responseBody.isNullOrEmpty()) {
                    val jsonResponse = JSONObject(responseBody)
                    val favoritesArray = jsonResponse.optJSONArray("favorites")
                    if (favoritesArray != null) {
                        val favoriteProfileIds = mutableListOf<Triple<String, String, String>>()
                        for (i in 0 until favoritesArray.length()) {
                            val favoriteJson = favoritesArray.getJSONObject(i)
                            val profileId = favoriteJson.optString("profileId")

                            val note = try {
                                DatabaseHelper.query(
                                    "SELECT note FROM profile_note WHERE profile_id = ?",
                                    arrayOf(profileId)
                                ).firstOrNull()?.get("note") as? String ?: ""
                            } catch (e: Exception) {
                                Logger.apply {
                                    log("Failed to fetch note for profileId $profileId: ${e.message}")
                                    writeRaw(e.stackTraceToString())
                                }
                                ""
                            }

                            val phoneNumber = try {
                                DatabaseHelper.query(
                                    "SELECT phone_number FROM profile_note WHERE profile_id = ?",
                                    arrayOf(profileId)
                                ).firstOrNull()?.get("phone_number") as? String ?: ""
                            } catch (e: Exception) {
                                Logger.apply {
                                    log("Failed to fetch phone number for profileId $profileId: ${e.message}")
                                    writeRaw(e.stackTraceToString())
                                }
                                ""
                            }

                            favoriteProfileIds.add(Triple(profileId, note, phoneNumber))
                        }
                        return@withContext favoriteProfileIds
                    }
                }
            }
            emptyList()
        } catch (e: Exception) {
            val message = "Failed to get favorites: ${e.message}"
            Logger.apply {
                log(message)
                writeRaw(e.stackTraceToString())
            }
            emptyList()
        }
    }

    fun addProfileNote(profileId: String, notes: String, phoneNumber: String, silent: Boolean = false) {
        if (notes.length > 250) {
            showToast(Toast.LENGTH_LONG, "Notes are too long")
            return
        }

        val body = """
            {
                "notes": "${notes.replace("\n", "\\n")}",
                "phoneNumber": "$phoneNumber"
            }
        """.trimIndent()

        GrindrPlus.executeAsync {
            val response = sendRequest(
                "https://grindr.mobi/v1/favorites/notes/$profileId",
                "PUT",
                body = body.toRequestBody(),
                headers = mapOf("Content-Type" to "application/json; charset=utf-8")
            )
            if (response.isSuccessful) {
                try {
                    val existingNote = DatabaseHelper.query(
                        "SELECT * FROM profile_note WHERE profile_id = ?",
                        arrayOf(profileId)
                    ).firstOrNull()
                    if (existingNote != null) {
                        DatabaseHelper.update(
                            "profile_note",
                            ContentValues().apply {
                                put("note", notes)
                                put("phone_number", phoneNumber)
                            },
                            "profile_id = ?",
                            arrayOf(profileId)
                        )
                    } else {
                        DatabaseHelper.insert(
                            "profile_note",
                            ContentValues().apply {
                                put("profile_id", profileId)
                                put("note", notes)
                                put("phone_number", phoneNumber)
                            }
                        )
                    }
                } catch (e: Exception) {
                    Logger.apply {
                        log("Failed to update profile note: ${e.message}")
                        writeRaw(e.stackTraceToString())
                    }
                }
                if (!silent) showToast(Toast.LENGTH_LONG, "Note added successfully")
            } else {
                if (!silent) {
                    showToast(
                        Toast.LENGTH_LONG,
                        "Failed to add note: ${response.body?.string()}"
                    )
                }
            }
        }
    }

    fun enableIncognito() {
        updateSettings("""{"settings":{"incognito":true}}""")
    }

    fun disableIncognito() {
        updateSettings("""{"settings":{"incognito":false}}""")
    }
}

fun RequestBody.Companion.createEmpty(): RequestBody {
    return "".toRequestBody()
}
