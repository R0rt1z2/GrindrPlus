package com.grindrplus.manager.installation.steps

import android.content.Context
import com.grindrplus.manager.installation.BaseStep
import com.grindrplus.manager.installation.Print
import com.grindrplus.manager.utils.KeyStoreUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.lsposed.patch.LSPatch
import org.lsposed.patch.util.Logger
import java.io.File
import java.io.IOException

/**
 * install modfile into base apk and save all apks to output folder
 */
class PatchApkStep(
    private val inputDir: File,
    private val outputDir: File,
    private val modFile: File,
    private val keyStoreUtils: KeyStoreUtils,
) : BaseStep() {
    override val name = "Patch Grindr APK"


    override suspend fun doExecute(context: Context, print: Print) {
        print("Cleaning output directory...")
        outputDir.listFiles()?.forEach { it.delete() }

        val apkFiles = inputDir.listFiles()?.filter { it.name.endsWith(".apk") && it.exists() && it.length() > 0 }

        if (apkFiles.isNullOrEmpty()) {
            throw IOException("No valid APK files found to patch")
        }

        print("Starting LSPatch process with ${apkFiles.size} APK files")

        val apkFilePaths = apkFiles.map { it.absolutePath }.toTypedArray()

        val logger = object : Logger() {
            override fun d(message: String?) {
                if (message?.startsWith("Original signature") ?: false)
                    return

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
        print("Using keystore: ${keyStoreUtils.keyStore.absolutePath}")

        withContext(Dispatchers.IO) {
            LSPatch(
                logger,
                *apkFilePaths,
                "-o", outputDir.absolutePath,
                "-l", "2",
                "-f",
                "-v",
                "-m", modFile.absolutePath,
                "-k", keyStoreUtils.keyStore.absolutePath,
                "password",
                "alias",
                "password"
            ).doCommandLine()
        }

        val patchedFiles = outputDir.listFiles()
        if (patchedFiles.isNullOrEmpty()) {
            throw IOException("Patching failed - no output files generated")
        }

        print("Successfully patched ${patchedFiles.size} files")

        patchedFiles.forEachIndexed { index, file ->
            print("  ${index + 1}. ${file.name} (${file.length() / 1024}KB)")
        }
    }
}