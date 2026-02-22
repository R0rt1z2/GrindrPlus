package com.grindrplus.manager.utils

import android.content.Context
import android.os.StatFs
import com.grindrplus.manager.installation.Installation
import timber.log.Timber
import java.io.File

object StorageUtils {
    private const val TAG = "StorageUtils"

    fun getAvailableSpace(path: File): Long {
        return try {
            if (!path.exists()) {
                path.mkdirs()
            }

            val stat = StatFs(path.path)
            val blockSize = stat.blockSizeLong
            val availableBlocks = stat.availableBlocksLong
            blockSize * availableBlocks
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error checking available space")
            0L
        }
    }

    fun cleanupOldInstallationFiles(
        context: Context,
        keepLatestVersion: Boolean = true,
        latestVersion: String? = null
    ) {
        try {
            val inputDir = Installation.Directories(context.externalCacheDir).input
            val threeDaysAgo = System.currentTimeMillis() - (3 * 24 * 60 * 60 * 1000)

            inputDir.listFiles()
                ?.filter { it.name.startsWith("grindr-") && it.name.endsWith(".zip") }
                ?.forEach { file ->
                    val version = file.name.removePrefix("grindr-").removeSuffix(".zip")
                    if (!keepLatestVersion || version != latestVersion) {
                        if (file.lastModified() < threeDaysAgo) {
                            file.delete()
                        }
                    }
                }

            inputDir.listFiles()
                ?.filter { it.name.startsWith("mod-") && it.name.endsWith(".zip") }
                ?.forEach { file ->
                    val version = file.name.removePrefix("mod-").removeSuffix(".zip")
                    if (!keepLatestVersion || version != latestVersion) {
                        if (file.lastModified() < threeDaysAgo) {
                            file.delete()
                        }
                    }
                }

        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error during cleanup")
        }
    }
}