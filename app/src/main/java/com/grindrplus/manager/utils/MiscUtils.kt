package com.grindrplus.manager.utils

import android.content.Context
import android.content.Intent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.Socket

suspend fun uploadAndShare(text: String, context: Context) {
    val response = withContext(Dispatchers.IO) {
        Socket("termbin.com", 9999).use { socket ->
            socket.getOutputStream().write(text.toByteArray())
            socket.getInputStream().bufferedReader().readText()
        }.trim().replace("\n", "")
    }

    val sendIntent = Intent().apply {
        action = Intent.ACTION_SEND
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, response)
    }

    val shareIntent =
        Intent.createChooser(sendIntent, "Share installation logs")
    context.startActivity(shareIntent)
}