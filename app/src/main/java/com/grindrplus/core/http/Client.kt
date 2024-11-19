package com.grindrplus.core.http

import android.widget.Toast
import com.grindrplus.GrindrPlus
import com.grindrplus.GrindrPlus.showToast
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

    fun blockUser(profileId: String) {
        GrindrPlus.executeAsync {
            val response = sendRequest(
                "https://grindr.mobi/v3/me/blocks/$profileId",
                "POST"
            )
            if (response.isSuccessful) {
                showToast(Toast.LENGTH_LONG, "User blocked successfully")
            } else {
                showToast(
                    Toast.LENGTH_LONG,
                    "Failed to block user: ${response.body?.string()}"
                )
            }
        }
    }

    fun unblockUser(profileId: String) {
        GrindrPlus.executeAsync {
            val response = sendRequest(
                "https://grindr.mobi/v3/me/blocks/$profileId",
                "DELETE"
            )
            if (response.isSuccessful) {
                showToast(Toast.LENGTH_LONG, "User unblocked successfully")
            } else {
                showToast(
                    Toast.LENGTH_LONG,
                    "Failed to unblock user: ${response.body?.string()}"
                )
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
                mapOf("Content-Type" to "application/json"),
                body.toRequestBody()
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
            val response = sendRequest(
                "https://grindr.mobi/v3/me/settings",
                "PATCH",
                mapOf("Content-Type" to "application/json"),
                settings.toRequestBody()
            )

            if (response.isSuccessful) {
                showToast(Toast.LENGTH_LONG, "Settings updated successfully")
            } else {
                showToast(
                    Toast.LENGTH_LONG,
                    "Failed to update settings: ${response.body?.string()}"
                )
            }
        }
    }

    fun enableIncognito() {
        updateSettings("""{"settings":{"incognito":true}}""")
    }
}

fun RequestBody.Companion.createEmpty(): RequestBody {
    return "".toRequestBody()
}
