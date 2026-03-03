package com.grindrplus.manager.installation

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import kotlin.system.measureTimeMillis

interface Step {
    val name: String

    suspend fun execute(context: Context, print: Print)
}

abstract class BaseStep : Step {
    override suspend fun execute(context: Context, print: Print) {
        try {
            print("===== STEP: $name =====")

            val time = measureTimeMillis {
                withContext(Dispatchers.IO) {
                    doExecute(context, print)
                }
            }

            print("Step $name completed in ${time / 1000} seconds")
            print("===== COMPLETED: $name =====")
        } catch (e: Exception) {
            print("===== FAILED: $name =====")
            throw IOException("$name failed: ${e.localizedMessage}")
        }
    }

    protected abstract suspend fun doExecute(context: Context, print: Print)
}