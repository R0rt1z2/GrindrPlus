package com.grindrplus.manager

import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.PrintWriter

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val coroutineScope = rememberCoroutineScope()
            val loadedData = remember { mutableStateListOf<Data>() }

            LaunchedEffect(coroutineScope) {
                launch {
                    withContext(Dispatchers.IO) {
                        val client = OkHttpClient()
                        val request = Request.Builder().url(
                            "https://raw.githubusercontent.com/R0rt1z2/GrindrPlus/refs/heads/master/manager-data.json"
                        ).build()

                        val result =
                            client.newCall(request).execute().body?.string() ?: TODO("handle error")

                        val obj = JSONObject(result)


                        obj.keys().forEach {
                            loadedData.add(
                                Data(
                                    it,
                                    obj.getJSONArray(it)[0].toString(),
                                    obj.getJSONArray(it)[1].toString(),
                                )
                            )
                        }
                    }
                }
            }

            var chosenVer by remember { mutableStateOf<Data?>(null) }
            var consoleOutput by remember { mutableStateOf("") }
            var isRunning by remember { mutableStateOf(false) }

            MaterialTheme(
                colorScheme = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) dynamicDarkColorScheme(
                    this
                ) else darkColorScheme()
            ) {
                Scaffold(
                    topBar = {
                    },
                    content = { innerPadding ->
                        Column(
                            Modifier.padding(innerPadding)
                        ) {
                            for (data in loadedData) {
                                Row {
                                    Checkbox(
                                        checked = chosenVer == data,
                                        onCheckedChange = {
                                            chosenVer =
                                                if (chosenVer == data) null else data
                                        },
                                        enabled = chosenVer == null || chosenVer == data
                                    )

                                    Text(data.modVer)
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .height(30.dp)
                                    .fillMaxWidth()
                            ) {
                                Text(consoleOutput)
                            }

                            Button(
                                modifier = Modifier
                                    .fillMaxSize(),
                                onClick = {
                                    coroutineScope.launch {
                                        isRunning = true
                                        Installation(
                                            this@MainActivity,
                                            chosenVer!!.modVer,
                                            chosenVer!!.modUrl,
                                            chosenVer!!.grindrUrl
                                        ).install {
                                            Log.i("GrindrPlus", it)
                                            consoleOutput = it
                                        }
                                        isRunning = false
                                    }
                                },
                                enabled = chosenVer != null && !isRunning
                            ) {
                                Text("patch grindr")
                            }
                        }
                    },
                    bottomBar = {

                    }
                )
            }
        }
    }
}

data class Data(
    val modVer: String,
    val grindrUrl: String,
    val modUrl: String,
)