package com.grindrplus.manager.installation.steps

import android.content.Context
import com.grindrplus.manager.installation.BaseStep
import com.grindrplus.manager.installation.Print
import java.io.File
import java.io.IOException

/**
 * Copy all apks from uzip to output folder, without further modifying them
 */
class CopyOutputStep(
    private val sourceDir: File,
    private val targetDir: File,
) : BaseStep() {
    override val name = "Copy APK files to be installed"


    override suspend fun doExecute(context: Context, print: Print) {
        print("Cleaning output directory...")
        targetDir.listFiles()?.forEach { it.delete() }

        val apkFiles = sourceDir.listFiles()?.filter { it.name.endsWith(".apk") && it.exists() && it.length() > 0 }

        if (apkFiles.isNullOrEmpty()) {
            throw IOException("No APK files found")
        }

        print("Copying ${apkFiles.size} files")

        apkFiles.forEachIndexed { index, apkFile ->
            val outputFile = File(targetDir, apkFile.name)
            apkFile.copyTo(outputFile, overwrite = true)
        }

        val copiedFiles = targetDir.listFiles()
        if (copiedFiles.isNullOrEmpty()) {
            throw IOException("Copying APKs failed - no files copied")
        }
    }
}