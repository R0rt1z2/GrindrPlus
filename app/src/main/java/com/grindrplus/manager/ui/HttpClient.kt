package com.grindrplus.manager.ui

import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

// A single, shared client for the entire app to use.
object HttpClient {
    val instance: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()
}