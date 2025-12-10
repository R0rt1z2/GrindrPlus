package com.grindrplus.manager

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.google.gson.JsonParser
import com.grindrplus.R
import com.grindrplus.core.Config
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import java.io.IOException
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

class GPlusMessage(
    val id: String,
    val content: String,
    val timestamp: Long
)

const val CHANNEL_PING_URL = "https://github.com/R0rt1z2/GrindrPlus/raw/refs/heads/master/news.json"
val tgMessages = MutableStateFlow<List<GPlusMessage>>(listOf())

suspend fun fetchNotifs(context: Context) = withContext(Dispatchers.IO) {
    val client = OkHttpClient.Builder()
        .callTimeout(1000.seconds.toJavaDuration()).build()

    val request = okhttp3.Request.Builder()
        .url(CHANNEL_PING_URL)
        .header(
            "User-Agent",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Safari/537.36"
        )
        .build()

    client.newCall(request).execute().use { response ->
        if (!response.isSuccessful) throw IOException("Unexpected code $response")

        tgMessages.value =
            response.body!!.string().replace("\n", "").split("|{").asSequence().drop(1)
                .map { "{$it" }
                .map {JsonParser.parseString(it).asJsonObject }
                .map { obj ->
                    GPlusMessage(
                        obj.get("message_id").asString,
                        obj.get("text").asString,
                        obj.get("date").asLong
                    )
                }
                .filterNot { it.content.isBlank() }
                .sortedBy { it.id }.toList()

        val msg = tgMessages.value.lastOrNull() ?: return@use
        if (Config.get("last_push_id", "") != msg.id) {
            Config.put("last_push_id", msg.id)
            if (msg.content.contains("#push"))
                sendNotification(context, msg.content.replace("#push", "").trim())
            else sendNotification(context)
        }
    }
}

fun sendNotification(
    context: Context,
    msg: String = "New message from GrindrPlus! Open News tab to read."
) {
    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    val channel = android.app.NotificationChannel(
        "update_gplus",
        "GPlus Updates",
        NotificationManager.IMPORTANCE_HIGH
    ).apply {
        description = "Notifications for GPlus communications"
    }

    nm.createNotificationChannel(channel)

    NotificationCompat.Builder(context, "update_gplus").apply {
        setSmallIcon(R.drawable.ic_launcher_foreground)
        setContentTitle("GrindrPlus News")
        setContentText(msg)
        setContentIntent(
            PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )
        setAutoCancel(true)
        setPriority(NotificationCompat.PRIORITY_MAX)
    }.also { nm.notify(1, it.build()) }
}
