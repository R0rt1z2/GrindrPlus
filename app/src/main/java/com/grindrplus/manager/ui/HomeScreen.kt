package com.grindrplus.manager.ui

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.Red
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import coil3.compose.AsyncImage
import com.grindrplus.core.Logger
import dev.jeziellago.compose.markdowntext.MarkdownText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.time.Instant

@Composable
fun HomeScreen(innerPadding: PaddingValues) {
    data class Release(
        val name: String,
        val description: String,
        val author: String,
        val avatarUrl: String,
        val publishedAt: Instant,
    )

    var contributors = remember { mutableStateMapOf<String, String>() }
    var releases = remember { mutableStateMapOf<String, Release>() }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        val client = OkHttpClient()
        val contributorsRequest = Request.Builder()
            .url("https://api.github.com/repos/R0rt1z2/GrindrPlus/contributors")
            .header("Accept", "application/vnd.github.v3+json")
            .build()

        val releasesRequest = Request.Builder()
            .url("https://api.github.com/repos/R0rt1z2/GrindrPlus/releases")
            .header("Accept", "application/vnd.github.v3+json")
            .build()

        try {
            withContext(Dispatchers.IO) {
                val contributorsResponse = async { client.newCall(contributorsRequest).execute() }
                val releasesResponse = async { client.newCall(releasesRequest).execute() }

                contributorsResponse.await().use { response ->
                    if (response.isSuccessful) {
                        val jsonArray = JSONArray(response.body?.string())
                        for (i in 0 until jsonArray.length()) {
                            val contributor = jsonArray.getJSONObject(i)
                            if (contributor.getString("login").contains("bot")) continue
                            contributors[contributor.getString("login")] =
                                contributor.getString("avatar_url")
                        }
                    } else {
                        errorMessage = "Failed to fetch contributors: ${response.message}"
                    }
                }

                releasesResponse.await().use { response ->
                    if (response.isSuccessful) {
                        val jsonArray = JSONArray(response.body?.string())
                        for (i in 0 until jsonArray.length()) {
                            val release = jsonArray.getJSONObject(i)
                            val id = release.getString("id")
                            val name = release.getString("name") ?: release.getString("tag_name")
                            val description = release.getString("body") ?: "No description provided"
                            val author = release.getJSONObject("author").getString("login")
                            val avatarUrl = release.getJSONObject("author").getString("avatar_url")
                            val publishedAt = Instant.parse(release.getString("published_at"))

                            releases[id] =
                                Release(name, description, author, avatarUrl, publishedAt)
                        }
                    } else {
                        errorMessage = "Failed to fetch releases: ${response.message}"
                    }
                }
            }
        } catch (e: Exception) {
            Logger.e("Error fetching data: ${e.message}")
            errorMessage = "An error occurred: ${e.message}"
        } finally {
            isLoading = false
        }
    }

    Column(
        modifier = Modifier
            .padding(innerPadding)
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "GrindrPlus",
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Enhanced Features for Grindr",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }

        if (errorMessage != null) {
            Text(
                text = errorMessage!!,
                color = Red,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Contributors",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.padding(16.dp),
                    strokeWidth = 2.dp
                )
            } else {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(contributors.size) { index ->
                        val (login, avatarUrl) = contributors.entries.elementAt(contributors.size - index - 1)
                        AsyncImage(
                            model = avatarUrl,
                            contentDescription = "Avatar of $login",
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .clickable {
                                    Intent(
                                        Intent.ACTION_VIEW,
                                        "https://github.com/$login".toUri()
                                    ).also { intent ->
                                        context.startActivity(intent)
                                    }
                                },
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(top = 16.dp)
        ) {
            val sortedReleases = releases.entries.sortedByDescending { (_, release) -> release.publishedAt }

            items(sortedReleases.size) { index ->
                val (_, release) = sortedReleases[index]
                androidx.compose.material3.Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AsyncImage(
                                model = release.avatarUrl,
                                contentDescription = "Avatar of ${release.author}",
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                            Text(
                                text =
                                    "${release.author} • ${release.name}", fontSize = 14.sp, fontWeight = FontWeight.Bold
                            )
                        }

                        MarkdownText(
                            markdown = release.description,
                            syntaxHighlightColor = Color.Transparent,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}