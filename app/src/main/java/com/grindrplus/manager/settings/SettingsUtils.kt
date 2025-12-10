package com.grindrplus.manager.settings

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object SettingsUtils {
    fun testMapsApiKey(
        context: Context,
        viewModelScope: CoroutineScope,
        apiKey: String,
        showTestDialog: (Boolean, String, String, String) -> Unit
    ) {
        showTestDialog(true, "Testing", "Testing your API key...", "")

        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    val url = URL("https://maps.googleapis.com/maps/api/geocode/json?address=USA&key=$apiKey")
                    val connection = url.openConnection() as HttpURLConnection
                    connection.requestMethod = "GET"
                    connection.connectTimeout = 10000
                    connection.readTimeout = 10000

                    val responseCode = connection.responseCode
                    val input = if (responseCode >= 400) {
                        connection.errorStream
                    } else {
                        connection.inputStream
                    }

                    val response = input.bufferedReader().use { it.readText() }
                    val jsonResponse = JSONObject(response)
                    val status = jsonResponse.optString("status", "")
                    val errorMessage = if (jsonResponse.has("error_message"))
                        jsonResponse.getString("error_message") else ""

                    Triple(status, errorMessage, response)
                }

                val (status, errorMessage, rawResponse) = result
                when {
                    status == "OK" -> {
                        showTestDialog(
                            false,
                            "Success!",
                            "Your Google Maps API key is working correctly. You can use it with GrindrPlus.",
                            rawResponse
                        )
                    }
                    status == "REQUEST_DENIED" && errorMessage.isNotEmpty() -> {
                        if (errorMessage.contains("API key is invalid")) {
                            showTestDialog(
                                false,
                                "Invalid API Key",
                                "Your API key is invalid. Please double-check that you've copied it correctly.",
                                rawResponse
                            )
                        } else if (errorMessage.contains("not authorized")) {
                            showTestDialog(
                                false,
                                "API Not Enabled",
                                "Your API key is valid but you need to enable the Geocoding API in the Google Cloud Console.",
                                rawResponse
                            )
                        } else {
                            showTestDialog(
                                false,
                                "API Error",
                                "Error: $errorMessage",
                                rawResponse
                            )
                        }
                    }
                    else -> {
                        showTestDialog(
                            false,
                            "Warning",
                            "API returned $status status",
                            rawResponse
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                showTestDialog(
                    false,
                    "Connection Error",
                    "Failed to connect to Google Maps API: ${e.message}",
                    e.stackTraceToString()
                )
            }
        }
    }
}

@Composable
fun ApiKeyTestDialog(
    isLoading: Boolean,
    title: String,
    message: String,
    rawResponse: String,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(48.dp)
                            .padding(bottom = 16.dp)
                    )
                } else {
                    when {
                        title.contains("Success") -> {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Success",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(48.dp)
                            )
                        }
                        title.contains("Warning") -> {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Warning",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(48.dp)
                            )
                        }
                        else -> {
                            Icon(
                                imageVector = Icons.Default.Error,
                                contentDescription = "Error",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(48.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )

                if (rawResponse.isNotEmpty() && !isLoading) {
                    Spacer(modifier = Modifier.height(16.dp))

                    var showResponse by remember { mutableStateOf(false) }

                    Button(
                        onClick = { showResponse = !showResponse },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Text(if (showResponse) "Hide Details" else "Show Details")
                    }

                    if (showResponse) {
                        Spacer(modifier = Modifier.height(12.dp))

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 200.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .padding(12.dp)
                                    .verticalScroll(rememberScrollState())
                            ) {
                                Text(
                                    text = rawResponse,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text("Close")
                }
            }
        }
    }
}