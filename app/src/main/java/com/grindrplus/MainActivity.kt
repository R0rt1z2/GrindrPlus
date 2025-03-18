package com.grindrplus

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.lsposed.patch.LSPatch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Scaffold(
                    topBar = {
                    },
                    content = { innerPadding ->
                        Button(
                            modifier = Modifier
                                .padding(innerPadding)
                                .fillMaxSize(),
                            onClick = {
                                // https://d.apkpure.com/b/XAPK/com.grindrapp.android?version=latest
                                // todo: https://github.com/revenge-mod/revenge-manager/raw/refs/tags/v1.1.0/app/src/main/java/app/revenge/manager/installer/step/StepRunner.kt
                            }
                        ) {
                            Text("patch grindr")
                        }
                    },
                    bottomBar = {

                    }
                )
            }
        }
    }
}