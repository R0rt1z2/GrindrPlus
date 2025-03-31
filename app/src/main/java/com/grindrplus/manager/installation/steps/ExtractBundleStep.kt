package com.grindrplus.manager.installation.steps

import android.content.Context
import com.grindrplus.manager.installation.BaseStep
import com.grindrplus.manager.installation.Print
import com.grindrplus.manager.utils.unzip
import java.io.File
import java.io.IOException

// 3rd
class ExtractBundleStep(
    private val bundleFile: File,
    private val unzipFolder: File
) : BaseStep() {
    override val name = "Extracting Bundle"

    override suspend fun doExecute(context: Context, print: Print) {
        try {
            print("Cleaning extraction directory...")
            unzipFolder.listFiles()?.forEach { it.delete() }

            print("Extracting bundle archive...")
            bundleFile.unzip(unzipFolder)

            val apkFiles = unzipFolder.listFiles()?.filter { it.name.endsWith(".apk") } ?: emptyList()
            if (apkFiles.isEmpty()) {
                throw IOException("No APK files found in the bundle archive")
            }

            print("Successfully extracted ${apkFiles.size} APK files")

            apkFiles.forEachIndexed { index, file ->
                print("  ${index + 1}. ${file.name} (${file.length() / 1024}KB)")
            }
        } catch (e: Exception) {
            throw IOException("Failed to extract bundle file: ${e.localizedMessage}")
        }
    }
}