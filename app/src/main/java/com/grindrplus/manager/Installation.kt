package com.grindrplus.manager

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.grindrplus.manager.utils.DownloadResult
import com.grindrplus.manager.utils.StorageUtils
import com.grindrplus.manager.utils.SessionInstaller
import com.grindrplus.manager.utils.download
import com.grindrplus.manager.utils.newKeystore
import com.grindrplus.manager.utils.unzip
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.lsposed.patch.LSPatch
import org.lsposed.patch.util.Logger
import java.io.File
import java.io.IOException
import java.util.zip.ZipFile
import kotlin.coroutines.resume

class Installation(
    private val context: Context,
    modVer: String,
    private val modUrl: String,
    private val grindrUrl: String,
) {
    private val keyStore by lazy {
        File(context.cacheDir, "keystore.jks").also {
            if (!it.exists()) {
                try {
                    newKeystore(it)
                } catch (e: Exception) {
                    showToast("Failed to create keystore: ${e.localizedMessage}")
                    throw e
                }
            }
        }
    }

    private val folder = context.getExternalFilesDir(null)
        ?: throw IOException("External files directory not available")
    private val unzipFolder = File(folder, "splitApks/").also { it.mkdirs() }
    private val outputDir = File(folder, "LSPatchOutput/").also { it.mkdirs() }
    private val modFile = File(folder, "mod-$modVer.zip")
    private val xapkFile = File(folder, "grindr-$modVer.xapk")

    suspend fun install(print: (String) -> Unit, progress: (Float) -> Unit) = try {
        withContext(Dispatchers.IO) {
            stepWithProgress("Checking storage space", print) {
                checkStorageSpace(print)
            }

            stepWithProgress("Downloading Grindr APK", print) {
                downloadGrindrApk(print, progress)
            }

            stepWithProgress("Downloading Mod", print) {
                downloadMod(print, progress)
            }

            stepWithProgress("Patching Grindr APK", print) {
                patchGrindrApk(print, progress)
            }

            stepWithProgress("Installing Grindr APK", print) {
                installGrindrApk(print)
            }

            print("Installation completed successfully!")
            showToast("Installation completed successfully!")
        }
    } catch (e: CancellationException) {
        print("Installation was cancelled")
        showToast("Installation was cancelled")
        throw e
    } catch (e: Exception) {
        val errorMsg = "Installation failed: ${e.localizedMessage}"
        print(errorMsg)
        showToast(errorMsg)
        cleanupOnFailure()
        throw e
    }

    private fun checkStorageSpace(print: (String) -> Unit) {
        val requiredSpace = 200 * 1024 * 1024 // 200MB as a safe minimum
        val availableSpace = StorageUtils.getAvailableSpace(folder)

        print("Available storage space: ${availableSpace / 1024 / 1024}MB")

        if (availableSpace < requiredSpace) {
            throw IOException("Not enough storage space. Need ${requiredSpace / 1024 / 1024}MB, but only ${availableSpace / 1024 / 1024}MB available.")
        }

        print("Storage space check passed")
    }

    private suspend fun genericDownload(
        file: File,
        url: String,
        print: (String) -> Unit,
        progress: (Float) -> Unit,
        fileType: String,
    ): Boolean {
        print("Downloading $fileType file...")

        if (file.exists() && file.length() > 0) {
            try {
                withContext(Dispatchers.IO) {
                    ZipFile(file).close()
                }

                print("Existing $fileType file found, skipping download")
                return true
            } catch (e: Exception) {
                Log.w("Download", "Existing file ${file.name} is corrupt, redownloading", e)
                file.delete()
            }
        }

        val result = download(context, file, url) { progress, eta ->
            progress?.let {
                val percentage = (it * 100).toInt()
                progress(it)
                print(
                    "$fileType download<>: $percentage% " +
                            "(ETA:${eta?.div(60000)}m${(eta?.rem(60000))?.div(1000)}s)"
                )
            } ?: print("Preparing $fileType download...")
        }

        if (!result.success || !file.exists() || file.length() <= 0) {
            throw IOException("Failed to download $fileType, reason ${result.reason}")
        }

        val sizeMB = file.length() / 1024 / (if (fileType == "mod") 1 else 1024)
        print("$fileType download completed (${sizeMB}${if (fileType == "mod") "KB" else "MB"})")
        progress(0f)
        return true
    }

    private suspend fun downloadGrindrApk(print: (String) -> Unit, progress: (Float) -> Unit) {
        genericDownload(xapkFile, grindrUrl, print, progress, "Grindr apk")

        try {
            print("Cleaning extraction directory...")
            unzipFolder.listFiles()?.forEach { it.delete() }

            print("Extracting XAPK file...")
            xapkFile.unzip(unzipFolder)

            val apkFiles =
                unzipFolder.listFiles()?.filter { it.name.endsWith(".apk") } ?: emptyList()
            if (apkFiles.isEmpty()) {
                throw IOException("No APK files found in the XAPK archive")
            }

            print("Successfully extracted ${apkFiles.size} APK files")

            apkFiles.forEachIndexed { index, file ->
                print("  ${index + 1}. ${file.name} (${file.length() / 1024}KB)")
            }

            progress(0f)
        } catch (e: Exception) {
            throw IOException("Failed to extract XAPK file: ${e.localizedMessage}")
        }
    }

    private suspend fun downloadMod(print: (String) -> Unit, progress: (Float) -> Unit) {
        genericDownload(modFile, modUrl, print, progress, "mod")
    }

    private suspend fun patchGrindrApk(print: (String) -> Unit, progress: (Float) -> Unit) {
        try {
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
        } catch (e: Exception) {
            throw IOException("Failed to patch APK: ${e.localizedMessage}")
        }
    }

    private suspend fun installGrindrApk(print: (String) -> Unit) {
        val patchedFiles = outputDir.listFiles()?.toList() ?: emptyList()

        if (patchedFiles.isEmpty()) {
            throw IOException("No patched APK files found for installation")
        }

        val filteredApks =
            patchedFiles.filter { it.name.endsWith(".apk") && it.exists() && it.length() > 0 }
        if (filteredApks.isEmpty()) {
            throw IOException("No valid APK files found for installation")
        }

        print("Starting installation of ${filteredApks.size} APK files")
        filteredApks.forEachIndexed { index, file ->
            print("  Installing (${index + 1}/${filteredApks.size}): ${file.name}")
        }

        print("Launching installer...")
        val success = SessionInstaller().installApks(
            context,
            filteredApks,
            false,
            log = print,
            callback = { success, string ->
                if (success) {
                    print("APK installation completed successfully")
                } else {
                    print("APK installation failed: $string")
                }
            }
        )

        if (!success) {
            throw IOException("Installation failed")
        }

        print("APK installation completed successfully")
    }

    private fun cleanupOnFailure() {
        try {
            unzipFolder.listFiles()?.forEach { it.delete() }
            outputDir.listFiles()?.forEach { it.delete() }

            if (xapkFile.exists() && xapkFile.length() <= 100) xapkFile.delete()
            if (modFile.exists() && modFile.length() <= 100) modFile.delete()
        } catch (e: Exception) {
            // we dont care about these anyway
        }
    }

    private suspend fun stepWithProgress(
        name: String,
        print: (String) -> Unit,
        action: suspend () -> Unit,
    ) {
        try {
            print("===== STEP: $name =====")
            action()
            print("===== COMPLETED: $name =====")
        } catch (e: Exception) {
            print("===== FAILED: $name =====")
            throw IOException("$name failed: ${e.localizedMessage}")
        }
    }

    private fun showToast(message: String) {
        CoroutineScope(Dispatchers.Main).launch {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }
}