package com.grindrplus.manager.ui

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grindrplus.core.Logger
import com.grindrplus.manager.DATA_URL
import okhttp3.Request
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException

class InstallScreenViewModel : ViewModel() {

    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading.asStateFlow()

    private val _loadingText = MutableStateFlow("Loading available versions...")

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    val versionData = mutableStateListOf<Data>()

    fun loadVersionData(manifestUrl: String) {
        _isLoading.value = true
        _errorMessage.value = null
        viewModelScope.launch {
            // Update loading text after 10 seconds of waiting
            val textUpdateJob = launch {
                delay(10000)
                _loadingText.value = "Still loading... Check your internet connectivity."
            }

            val result = withContext(Dispatchers.IO) {
                fetchVersions(manifestUrl)
            }

            textUpdateJob.cancel() // Cancel the text update job once loading is done

            if (result.isSuccess) {
                versionData.clear()
                versionData.addAll(result.getOrThrow())
                Logger.d("Found ${versionData.size} available versions")
            } else {
                _errorMessage.value = result.exceptionOrNull()?.localizedMessage ?: "Unknown error"
                Logger.e("Error loading version data: ${_errorMessage.value}")
            }
            _isLoading.value = false
        }
    }

    private fun fetchVersions(manifestUrl: String): Result<List<Data>> {
        val client = HttpClient.instance
        val maxRetries = 3
        var lastException: Exception? = null

        for (attempt in 1..maxRetries) {
            try {
                Logger.d("Loading version data from $manifestUrl (Attempt $attempt/$maxRetries)")
                val request = Request.Builder().url(manifestUrl).build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) throw IOException("Server error: ${response.code}")
                    val body = response.body?.string() ?: throw IOException("Empty response body")
                    return Result.success(parseVersionData(body))
                }
            } catch (e: Exception) {
                lastException = e
                Logger.e("Attempt $attempt failed: ${e.message}")
                if (attempt < maxRetries) Thread.sleep(2000) // Use sleep for non-coroutine delay
            }
        }
        return Result.failure(lastException ?: IOException("Unknown error after $maxRetries retries"))
    }

    private fun parseVersionData(jsonData: String): List<Data> {
        return try {
            val result = mutableListOf<Data>()
            val jsonObject = JSONObject(jsonData)
            jsonObject.keys().forEach { key ->
                val jsonArray = jsonObject.getJSONArray(key)
                if (jsonArray.length() >= 2) {
                    result.add(
                        Data(
                            modVer = key,
                            grindrUrl = jsonArray.getString(0),
                            modUrl = jsonArray.getString(1)
                        )
                    )
                }
            }
            result.sortedByDescending { it.modVer }
        } catch (e: JSONException) {
            throw IOException("Invalid data format: ${e.localizedMessage}")
        }
    }
}