package com.grindrplus.core.http

import android.content.ContentValues
import android.widget.Toast
import com.grindrplus.GrindrPlus
import com.grindrplus.GrindrPlus.showToast
import com.grindrplus.core.DatabaseHelper
import okhttp3.*
import okhttp3.RequestBody.Companion.toRequestBody

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
                    GrindrPlus.logger.log("Error removing user from block list: ${e.message}")
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
                    GrindrPlus.logger.log("Error removing user from favorites list: ${e.message}")
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
