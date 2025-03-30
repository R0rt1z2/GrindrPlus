package com.grindrplus.manager.installation

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

interface Step {
    val name: String

    suspend fun execute(context: Context, print: (String) -> Unit, progress: (Float) -> Unit)
}

abstract class BaseStep : Step {
    override suspend fun execute(context: Context, print: (String) -> Unit, progress: (Float) -> Unit) {
        try {
            print("===== STEP: $name =====")
            withContext(Dispatchers.IO) {
                doExecute(context, print, progress)
            }
            print("===== COMPLETED: $name =====")
        } catch (e: Exception) {
            print("===== FAILED: $name =====")
            throw IOException("$name failed: ${e.localizedMessage}")
        }
    }

    protected abstract suspend fun doExecute(context: Context, print: (String) -> Unit, progress: (Float) -> Unit)
}