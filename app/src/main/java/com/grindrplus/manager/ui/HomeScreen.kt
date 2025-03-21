package com.grindrplus.manager.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun HomeScreen(innerPadding: PaddingValues) {
    Box(
        modifier = Modifier.fillMaxSize()
            .padding(innerPadding)
    ) {
        Text("haiii from GrindrPlus")
    }
}