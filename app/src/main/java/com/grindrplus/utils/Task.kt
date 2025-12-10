package com.grindrplus.utils

import com.grindrplus.GrindrPlus
import com.grindrplus.core.Config
import com.grindrplus.core.Logger
import com.grindrplus.core.LogSource
import kotlinx.coroutines.Job

abstract class Task(
    val id: String,
    val description: String,
    val initialDelayMillis: Long = 30 * 1000, // 30 seconds
    val intervalMillis: Long = 10 * 60 * 1000 // 10 minutes
) {
    private var job: Job? = null

    /**
     * Check if the task is enabled in config
     */
    fun isTaskEnabled(): Boolean {
        return Config.isTaskEnabled(id)
    }

    /**
     * Override this method to implement task-specific logic
     */
    abstract suspend fun execute()

    /**
     * Start the task if it's enabled in config
     */
    fun start() {
        if (!isTaskEnabled()) {
            Logger.i("Task $id is disabled", LogSource.MODULE)
            return
        }

        job = GrindrPlus.taskManager.startPeriodicTask(
            taskId = id,
            initialDelayMillis = initialDelayMillis,
            intervalMillis = intervalMillis,
            action = {
                try {
                    execute()
                    Logger.i("Task $id executed successfully", LogSource.MODULE)
                } catch (e: Exception) {
                    Logger.e("Task $id failed: ${e.message}", LogSource.MODULE)
                    Logger.writeRaw(e.stackTraceToString())
                }
            }
        )

        Logger.i("Task $id started", LogSource.MODULE)
    }

    /**
     * Stop the task
     */
    fun stop() {
        job?.let {
            if (GrindrPlus.taskManager.isTaskRunning(id)) {
                GrindrPlus.taskManager.cancelTask(id)
                Logger.i("Task $id stopped", LogSource.MODULE)
            }
        }
        job = null
    }

    /**
     * Called when task is first registered
     */
    open fun register() {
        Config.initTaskSettings(
            id,
            description,
            false // disabled by default
        )
    }
}