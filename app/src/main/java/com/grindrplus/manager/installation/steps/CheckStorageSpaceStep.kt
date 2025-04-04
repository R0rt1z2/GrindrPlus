package com.grindrplus.manager.installation.steps

import android.app.ActivityManager
import android.content.Context
import com.grindrplus.core.Logger
import com.grindrplus.manager.installation.BaseStep
import com.grindrplus.manager.installation.Print
import com.grindrplus.manager.utils.StorageUtils
import java.io.File
import java.io.IOException

// 1st
class CheckStorageSpaceStep(private val installFolder: File) : BaseStep() {
    override val name = "Checking system resources"

    override suspend fun doExecute(
        context: Context,
        print: Print,
    ) {
        val required = 200 * 1024 * 1024
        val availableStorage = StorageUtils.getAvailableSpace(installFolder)

        print("Available storage space: ${availableStorage / 1024 / 1024}MB")

        if (availableStorage < required) {
            throw IOException("Not enough storage space. Need ${required / 1024 / 1024}MB, but only ${availableStorage / 1024 / 1024}MB available.")
        }

        val availableRam = try {
            val activityManager =
                context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val memoryInfo = ActivityManager.MemoryInfo()
            activityManager.getMemoryInfo(memoryInfo)
            memoryInfo.availMem
        } catch (e: Exception) {
            Logger.e("Error getting available RAM: ${e.message}")
            0L
        }

        print("Available RAM: ${availableRam / 1024 / 1024}MB")

        if (availableRam < required) {
            throw IOException("Not enough RAM. Need ${required / 1024 / 1024}MB, but only ${availableRam / 1024 / 1024}MB available.")
        }

        print("System resource checks passed")
    }
}