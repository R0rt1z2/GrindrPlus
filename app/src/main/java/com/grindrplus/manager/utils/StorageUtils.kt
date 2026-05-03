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

            deleteOldDirectory(File(folder, "splitApks/"), threeDaysAgo)
            deleteOldDirectory(File(folder, "LSPatchOutput/"), threeDaysAgo)
            deleteVersionedFiles(folder, "grindr-", ".xapk", keepLatestVersion, latestVersion, threeDaysAgo)
            deleteVersionedFiles(folder, "mod-", ".zip", keepLatestVersion, latestVersion, threeDaysAgo)
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error during cleanup")
        }
    }

    private fun deleteOldDirectory(dir: File, olderThan: Long) {
        if (dir.exists() && dir.isDirectory && dir.lastModified() < olderThan) {
            dir.deleteRecursively()
        }
    }

    private fun deleteVersionedFiles(
        folder: File,
        prefix: String,
        suffix: String,
        keepLatestVersion: Boolean,
        latestVersion: String?,
        olderThan: Long,
    ) {
        folder.listFiles()?.forEach { file ->
            if (file.name.startsWith(prefix) && file.name.endsWith(suffix)) {
                val version = file.name.removePrefix(prefix).removeSuffix(suffix)
                if ((!keepLatestVersion || version != latestVersion) && file.lastModified() < olderThan) {
                    file.delete()
                }
            }
        }
    }
}