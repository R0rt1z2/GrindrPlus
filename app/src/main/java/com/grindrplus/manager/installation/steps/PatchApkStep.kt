package com.grindrplus.manager.installation.steps

import android.content.Context
import com.grindrplus.manager.installation.BaseStep
import com.grindrplus.manager.installation.Print
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.lsposed.patch.LSPatch
import org.lsposed.patch.util.Logger
import java.io.File
import java.io.IOException

// 5th
class PatchApkStep(
    private val unzipFolder: File,
    private val outputDir: File,
    private val modFile: File,
    private val keyStore: File
) : BaseStep() {
    override val name = "Patching Grindr APK"

    override suspend fun doExecute(context: Context, print: Print) {
        print("Cleaning output directory...")
        outputDir.listFiles()?.forEach { it.delete() }

        val apkFiles = unzipFolder.listFiles()
            ?.filter { it.name.endsWith(".apk") && it.exists() && it.length() > 0 }
            ?.map { it.absolutePath }
            ?.toTypedArray()

        if (apkFiles.isNullOrEmpty()) {
            throw IOException("No valid APK files found to patch")
        }

        print("Starting LSPatch process with ${apkFiles.size} APK files")

        val logger = object : Logger() {
            override fun d(message: String?) {
                message?.let { print("DEBUG: $it") }
            }

            override fun i(message: String?) {
                message?.let { print("INFO: $it") }
            }

            override fun e(message: String?) {
                message?.let { print("ERROR: $it") }
            }
        }

        print("Using mod file: ${modFile.absolutePath}")
        print("Using keystore: ${keyStore.absolutePath}")

        withContext(Dispatchers.IO) {
            LSPatch(
                logger,
                *apkFiles,
                "-o", outputDir.absolutePath,
                "-l", "2",
                "-f",
                "-v",
                "-m", modFile.absolutePath,
                "-k", keyStore.absolutePath,
                "password",
                "alias",
                "password"
            ).doCommandLine()
        }

        val patchedFiles = outputDir.listFiles()
        if (patchedFiles.isNullOrEmpty()) {
            throw IOException("Patching failed - no output files generated")
        }

        print("Patching completed successfully")
        print("Generated ${patchedFiles.size} patched files")

        patchedFiles.forEachIndexed { index, file ->
            print("  ${index + 1}. ${file.name} (${file.length() / 1024}KB)")
        }
    }
}