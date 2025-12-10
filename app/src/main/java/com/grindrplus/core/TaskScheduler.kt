package com.grindrplus.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class TaskScheduler(private val scope: CoroutineScope) {
    private val runningJobs = mutableMapOf<String, Job>()

    fun periodic(
        name: String,
        intervalMs: Long,
        action: suspend () -> Unit
    ): Job {
        val job = scope.launch {
            while (true) {
                try {
                    action()
                    delay(intervalMs)
                } catch (e: Exception) {
                    Logger.e("$name failed: ${e.message}", LogSource.MODULE)
                    Logger.writeRaw(e.stackTraceToString())
                    delay(intervalMs)
                }
            }
        }
        runningJobs[name] = job
        return job
    }

    fun once(name: String, action: suspend () -> Unit): Job {
        val job = scope.launch {
            try {
                action()
            } catch (e: Exception) {
                Logger.e("$name failed: ${e.message}", LogSource.MODULE)
                Logger.writeRaw(e.stackTraceToString())
            } finally {
                runningJobs.remove(name)
            }
        }
        runningJobs[name] = job
        return job
    }

    fun withRetry(
        name: String,
        retries: Int = 3,
        delayMs: Long = 1000,
        action: suspend () -> Unit
    ): Job {
        val job = scope.launch {
            try {
                repeat(retries) { attempt ->
                    try {
                        action()
                        return@launch
                    } catch (e: Exception) {
                        if (attempt == retries - 1) {
                            Logger.e("$name failed after $retries attempts", LogSource.MODULE)
                            Logger.writeRaw(e.stackTraceToString())
                            throw e
                        } else {
                            Logger.w("$name retry ${attempt+1}/$retries", LogSource.MODULE)
                            delay(delayMs)
                        }
                    }
                }
            } finally {
                runningJobs.remove(name)
            }
        }
        runningJobs[name] = job
        return job
    }

    fun isTaskRunning(name: String): Boolean {
        return runningJobs[name]?.isActive == true
    }

    fun cancelTask(name: String) {
        runningJobs[name]?.cancel()
        runningJobs.remove(name)
    }

    fun cancelAllTasks() {
        runningJobs.values.forEach { it.cancel() }
        runningJobs.clear()
    }
}