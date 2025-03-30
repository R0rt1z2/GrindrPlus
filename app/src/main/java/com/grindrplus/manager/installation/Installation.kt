package com.grindrplus.manager.installation

import android.content.Context
import android.widget.Toast
import com.grindrplus.manager.MainActivity
import com.grindrplus.manager.MainActivity.Companion.plausible
import com.grindrplus.manager.installation.steps.CheckStorageSpaceStep
import com.grindrplus.manager.installation.steps.DownloadStep
import com.grindrplus.manager.installation.steps.ExtractBundleStep
import com.grindrplus.manager.installation.steps.InstallApkStep
import com.grindrplus.manager.installation.steps.PatchApkStep
import com.grindrplus.manager.utils.newKeystore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import kotlin.system.measureTimeMillis

class Installation(
    private val context: Context,
    modVer: String,
    modUrl: String,
    grindrUrl: String,
) {
    private val folder = context.getExternalFilesDir(null)
        ?: throw IOException("External files directory not available")
    private val unzipFolder = File(folder, "splitApks/").also { it.mkdirs() }
    private val outputDir = File(folder, "LSPatchOutput/").also { it.mkdirs() }
    private val modFile = File(folder, "mod-$modVer.zip")
    private val bundleFile = File(folder, "grindr-$modVer.zip")
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

    // Order matters
    private val steps = listOf<Step>(
        CheckStorageSpaceStep(folder),
        DownloadStep(bundleFile, grindrUrl, "Grindr bundle"),
        ExtractBundleStep(bundleFile, unzipFolder),
        DownloadStep(modFile, modUrl, "mod"),
        PatchApkStep(unzipFolder, outputDir, modFile, keyStore),
        InstallApkStep(outputDir)
    )

    suspend fun install(print: (String) -> Unit, progress: (Float) -> Unit) = try {
        withContext(Dispatchers.IO) {
            plausible?.pageView("app://grindrplus/install")

            val time = measureTimeMillis {
                for (step in steps.dropLast(1)) {
                    step.execute(context, print, progress)
                }
            }

            print("Patching completed successfully in ${time / 1000 / 60}m${time / 1000}s!")
            plausible?.event(
                "install_success",
                "app://grindrplus/install_success",
                props = mapOf("time" to time)
            )

            steps.last().execute(context, print, progress)

            showToast("Installation completed successfully!")
        }
    } catch (e: CancellationException) {
        print("Installation was cancelled")
        showToast("Installation was cancelled")
        plausible?.event("install_cancelled", "app://grindrplus/install_cancelled")
        throw e
    } catch (e: Exception) {
        val errorMsg = "Installation failed: ${e.localizedMessage}"
        plausible?.event(
            "install_failed",
            "app://grindrplus/install_failure",
            props = mapOf("error" to e.message)
        )
        print(errorMsg)
        showToast(errorMsg)
        cleanupOnFailure()
        throw e
    }

    private fun cleanupOnFailure() {
        try {
            unzipFolder.listFiles()?.forEach { it.delete() }
            outputDir.listFiles()?.forEach { it.delete() }

            if (bundleFile.exists() && bundleFile.length() <= 100) bundleFile.delete()
            if (modFile.exists() && modFile.length() <= 100) modFile.delete()
        } catch (_: Exception) {
        }
    }

    private fun showToast(message: String) {
        CoroutineScope(Dispatchers.Main).launch {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }
}