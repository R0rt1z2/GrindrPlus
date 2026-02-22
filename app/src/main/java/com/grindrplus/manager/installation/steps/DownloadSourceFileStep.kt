package com.grindrplus.manager.installation.steps

import android.content.Context
import com.grindrplus.manager.installation.BaseStep
import com.grindrplus.manager.installation.Print
import com.grindrplus.manager.utils.download
import com.grindrplus.manager.utils.validateFile
import timber.log.Timber
import java.io.File
import java.io.IOException

/**
 * Download file to the app's cache folder
 */
class DownloadSourceFileStep(
    private val targetFile: File,
    private val url: String,
    private val fileType: String
) : BaseStep() {
    override val name = "Downloading $fileType"

    override suspend fun doExecute(context: Context, print: Print) {
        print("Downloading $fileType file...")

        if (targetFile.exists() && targetFile.length() > 0) {
            if (validateFile(targetFile)) {
                print("Existing $fileType file found, skipping download")
                return
            } else {
                Timber.tag("Download").w("Existing file ${targetFile.name} is corrupt, redownloading")
                targetFile.delete()
            }
        }

        val result = download(context, targetFile, url, print)

        if (!result.success || !targetFile.exists() || targetFile.length() <= 0) {
            throw IOException("Failed to download $fileType, reason ${result.reason}")
        }

        val sizeMB = targetFile.length() / 1024 / (if (fileType == "mod") 1 else 1024)
        print("$fileType download completed (${sizeMB}${if (fileType == "mod") "KB" else "MB"})")
    }
}