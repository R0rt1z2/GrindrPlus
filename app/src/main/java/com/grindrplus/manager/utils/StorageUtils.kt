package com.grindrplus.manager.utils

import android.content.Context
import android.os.StatFs
import android.util.Log
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
            val folder = context.getExternalFilesDir(null) ?: return
            val threeDaysAgo = System.currentTimeMillis() - (3 * 24 * 60 * 60 * 1000)

            val splitApksDir = File(folder, "splitApks/")
            if (splitApksDir.exists() && splitApksDir.isDirectory) {
                if (splitApksDir.lastModified() < threeDaysAgo) {
                    splitApksDir.deleteRecursively()
                }
            }

            val outputDir = File(folder, "LSPatchOutput/")
            if (outputDir.exists() && outputDir.isDirectory) {
                if (outputDir.lastModified() < threeDaysAgo) {
                    outputDir.deleteRecursively()
                }
            }

            folder.listFiles()?.forEach { file ->
                if (file.name.startsWith("grindr-") && file.name.endsWith(".xapk")) {
                    val version = file.name.removePrefix("grindr-").removeSuffix(".xapk")
                    if (!keepLatestVersion || version != latestVersion) {
                        if (file.lastModified() < threeDaysAgo) {
                            file.delete()
                        }
                    }
                }
            }

            folder.listFiles()?.forEach { file ->
                if (file.name.startsWith("mod-") && file.name.endsWith(".zip")) {
                    val version = file.name.removePrefix("mod-").removeSuffix(".zip")
                    if (!keepLatestVersion || version != latestVersion) {
                        if (file.lastModified() < threeDaysAgo) {
                            file.delete()
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error during cleanup")
        }
    }
}