package com.grindrplus.manager.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grindrplus.core.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.time.Instant

data class Contributor(
    val login: String,
    val avatarUrl: String,
)

data class Release(
    val id: String,
    val name: String,
    val description: String,
    val author: String,
    val avatarUrl: String,
    val publishedAt: Instant,
)

/**
 * Single state object for the Home screen. The UI consumes one StateFlow
 * instead of reading four independent `mutableStateOf` properties off the
 * ViewModel — that lets the screen react to consistent snapshots and lets
 * tests assert against a value rather than four moving fields.
 */
data class HomeUiState(
    val isLoading: Boolean = false,
    val contributors: List<Contributor> = emptyList(),
    val releases: List<Release> = emptyList(),
    val errorMessage: String? = null,
)

class HomeViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState(isLoading = true))
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var hasFetched = false

    companion object {
        private val TAG = HomeViewModel::class.simpleName

        private const val CONTRIBUTORS_URL = "https://api.github.com/repos/R0rt1z2/GrindrPlus/contributors"
        private const val RELEASES_URL = "https://api.github.com/repos/R0rt1z2/GrindrPlus/releases"
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

    private suspend fun parseContributors(jsonContent: String): List<Contributor> =
        withContext(Dispatchers.Default) {
            val jsonArray = JSONArray(jsonContent)
            val list = mutableListOf<Contributor>()
            for (i in 0 until jsonArray.length()) {
                val contributor = jsonArray.getJSONObject(i)
                val login = contributor.getString("login")
                if (login.contains("bot")) continue
                list += Contributor(login, contributor.getString("avatar_url"))
            }
            // Preserve the previous "last contributor first" display order.
            list.reversed()
        }

    private suspend fun parseReleases(jsonContent: String): List<Release> =
        withContext(Dispatchers.Default) {
            val jsonArray = JSONArray(jsonContent)
            val list = mutableListOf<Release>()
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
                list += Release(id, name, description, author, avatarUrl, publishedAt)
            }
            list.sortedByDescending { it.publishedAt }
        }

    fun fetchData(forceRefresh: Boolean = false) {
        if (hasFetched && !forceRefresh) return
        hasFetched = true
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            try {
                val contributorsDeferred = async { fetchUrlContent(CONTRIBUTORS_URL) }
                val releasesDeferred = async { fetchUrlContent(RELEASES_URL) }
                val contributors = parseContributors(contributorsDeferred.await())
                val releases = parseReleases(releasesDeferred.await())
                _uiState.value = HomeUiState(
                    isLoading = false,
                    contributors = contributors,
                    releases = releases,
                    errorMessage = null,
                )
            } catch (e: Exception) {
                Logger.e("$TAG: Error fetching data: ${e.message}")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "An error occurred: ${e.message}",
                    )
                }
            }
        }
    }
}
