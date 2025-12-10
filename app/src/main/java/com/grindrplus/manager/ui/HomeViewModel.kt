package com.grindrplus.manager.ui

import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grindrplus.core.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.time.Instant

data class Release(
    val name: String,
    val description: String,
    val author: String,
    val avatarUrl: String,
    val publishedAt: Instant,
)

class HomeViewModel : ViewModel() {
    val contributors = mutableStateMapOf<String, String>()
    val releases = mutableStateMapOf<String, Release>()
    val isLoading = mutableStateOf(true)
    val errorMessage = mutableStateOf<String?>(null)

    // Flag to avoid multiple fetches
    private var hasFetched = false

    companion object {
        private val TAG = HomeViewModel::class.simpleName

        private const val CONTRIBUTORS_URL = "https://api.github.com/repos/R0rt1z2/GrindrPlus/contributors"
        private const val RELEASES_URL = "https://api.github.com/repos/R0rt1z2/GrindrPlus/releases"
        // Reuse the HTTP client across requests
        private val client = OkHttpClient()
    }

    private suspend fun fetchUrlContent(url: String): String {
        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/vnd.github.v3+json")
            .build()

        return withContext(Dispatchers.IO) {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw Exception("Failed to fetch data: ${response.message}")
                }
                response.body?.string() ?: throw Exception("Empty response body")
            }
        }
    }

    private suspend fun parseContributors(jsonContent: String) = withContext(Dispatchers.Default) {
        val jsonArray = JSONArray(jsonContent)
        // Accumulate new data in a temporary map
        val newContributors = mutableMapOf<String, String>()
        for (i in 0 until jsonArray.length()) {
            val contributor = jsonArray.getJSONObject(i)
            if (contributor.getString("login").contains("bot")) continue
            newContributors[contributor.getString("login")] = contributor.getString("avatar_url")
        }
        // Update the state in bulk
        contributors.clear()
        contributors.putAll(newContributors)
    }

    private suspend fun parseReleases(jsonContent: String) = withContext(Dispatchers.Default) {
        val jsonArray = JSONArray(jsonContent)
        val newReleases = mutableMapOf<String, Release>()
        for (i in 0 until jsonArray.length()) {
            val release = jsonArray.getJSONObject(i)
            val id = release.getString("id")
            val name = if (!release.isNull("name"))
                release.getString("name") else release.getString("tag_name")
            val description = if (!release.isNull("body"))
                release.getString("body") else "No description provided"
            val author = release.getJSONObject("author").getString("login")
            val avatarUrl = release.getJSONObject("author").getString("avatar_url")
            val publishedAt = Instant.parse(release.getString("published_at"))

            newReleases[id] = Release(name, description, author, avatarUrl, publishedAt)
        }
        releases.clear()
        releases.putAll(newReleases)
    }

    fun fetchData(forceRefresh: Boolean = false) {
        if (hasFetched && !forceRefresh) return
        if (forceRefresh) {
            hasFetched = false
            errorMessage.value = null
        }
        hasFetched = true
        isLoading.value = true

        viewModelScope.launch {
            try {
                // Both requests are made in parallel
                val contributorsDeferred = async { fetchUrlContent(CONTRIBUTORS_URL) }
                val releasesDeferred = async { fetchUrlContent(RELEASES_URL) }

                parseContributors(contributorsDeferred.await())
                parseReleases(releasesDeferred.await())
            } catch (e: Exception) {
                Logger.e("$TAG: Error fetching data: ${e.message}")
                errorMessage.value = "An error occurred: ${e.message}"
            } finally {
                isLoading.value = false
            }
        }
    }
}
