package com.grindrplus.manager.installation

import android.content.Context
import android.net.Uri
import android.widget.Toast
import com.grindrplus.manager.MainActivity.Companion.plausible
import com.grindrplus.manager.installation.steps.AddModVersionInfoStep
import com.grindrplus.manager.installation.steps.CheckStorageSpaceStep
import com.grindrplus.manager.installation.steps.CloneGrindrStep
import com.grindrplus.manager.installation.steps.CopyOutputStep
import com.grindrplus.manager.installation.steps.CopySourceFileStep
import com.grindrplus.manager.installation.steps.DownloadSourceFileStep
import com.grindrplus.manager.installation.steps.ExtractBundleStep
import com.grindrplus.manager.installation.steps.InstallApkStep
import com.grindrplus.manager.installation.steps.PatchApkStep
import com.grindrplus.manager.installation.steps.ReplaceMapsApiKeyStep
import com.grindrplus.manager.installation.steps.SignApkStep
import com.grindrplus.manager.utils.KeyStoreUtils
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import kotlin.system.measureTimeMillis

typealias Print = (String) -> Unit

class Installation(
    private val context: Context,
    val version: String,
    val sourceFiles: SourceFiles,
    val appInfo: AppInfoOverride?,
    val mapsApiKey: String?,
    val embedLSPatch: Boolean,
) {
    private val keyStoreUtils = KeyStoreUtils(context)
    private val tempDir = context.externalCacheDir
        ?: throw IOException("External files directory not available")
    private val unzipDir = File(tempDir, "splitApks/").also { it.mkdirs() }
    private val outputDir = File(tempDir, "LSPatchOutput/").also { it.mkdirs() }
    private val modFile = File(tempDir, "mod-$version.zip")
    private val bundleFile = File(tempDir, "grindr-$version.zip")

    sealed interface SourceFiles {
        class Local(val bundleFileUri: Uri, val modFileUri: Uri?) : SourceFiles
        class Download(val bundleUrl: String, val modUrl: String?) : SourceFiles
    }

    data class AppInfoOverride(
        val packageName: String,
        val appName: String,
    )

    val steps = mutableListOf<Step>().apply {
        add(CheckStorageSpaceStep(tempDir))

        when (sourceFiles) {
            is SourceFiles.Download -> {
                add(DownloadSourceFileStep(bundleFile, sourceFiles.bundleUrl, "Grindr bundle"))
                sourceFiles.modUrl?.let { add(DownloadSourceFileStep(modFile, it, "mod")) }
            }
            is SourceFiles.Local -> {
                add(CopySourceFileStep(bundleFile, sourceFiles.bundleFileUri, "Grindr bundle"))
                sourceFiles.modFileUri?.let { add(CopySourceFileStep(modFile, sourceFiles.modFileUri, "mod")) }
            }
        }

        add(ExtractBundleStep(bundleFile, unzipDir))


        // keep track of whether we need to re-sign the apk
        // in most cases we do need to, but if we are installing non-clone on lsposed
        // we will install the original apk unmodified and we don't want to modifiy the signature
        // so the original/stock maps api key keeps working.
        // (lspatch itself also changes the signature)
        var signingNeeded = embedLSPatch


        // Putting steps CloneGrindrStep or AddModVersionInfoStep after ReplaceMapsApiKeyStep
        // produces invalid apk, which then fails to sign (something with invalid resource xml).
        // ReplaceMapsApiKeyStep uses different method to patch the manifest,
        // but this alone should not be an issue
        if (appInfo != null) {
            add(CloneGrindrStep(unzipDir, appInfo.packageName, appInfo.appName))
            signingNeeded = true
        }

        if (embedLSPatch) {
            add(AddModVersionInfoStep(unzipDir, version))
            signingNeeded = true
        }

        // we should replace maps api key (only) when the signature of the final apk changes
        if (mapsApiKey != null && signingNeeded) {
            add(ReplaceMapsApiKeyStep(unzipDir, mapsApiKey))
            signingNeeded = true
        }

        // if we modified the apk prior to lspatch, we should re-sign it,
        // since lspatch requires an existing signature for some reason
        // no need to sign afterwards because lspatch regenerates the signature
        if (signingNeeded)
            add(SignApkStep(keyStoreUtils, unzipDir))

        if (embedLSPatch)
            add(PatchApkStep(unzipDir, outputDir, modFile, keyStoreUtils))
        else
            add(CopyOutputStep(unzipDir, outputDir))

        add(InstallApkStep(outputDir))
    }

    /**
     * the installation should
     * - download the files into cache folder
     * - extract the apkm into unzip folder
     * - do minor patches (maps api key, clone) still inside unzip folder
     * - sign the apks
     * - execute lspatch (which also copies the files to output) or just copy to output
     * - install
     */
    suspend fun start(
        print: Print
    ) {
        val appType = if (appInfo == null) "install" else "clone"
        val patchType = if (embedLSPatch) "lspatch" else "lsposed"
        val versionString = if (sourceFiles is SourceFiles.Local) "custom" else version

        performOperation(
            steps = steps,
            operationName = "$appType-$patchType-$versionString",
            print = print
        )
    }

    suspend fun performOperation(
        steps: List<Step>,
        operationName: String,
        onSuccess: suspend () -> Unit = {},
        print: Print,
    ) = try {
        withContext(Dispatchers.IO) {
            plausible?.pageView("app://grindrplus/$operationName")

            val time = measureTimeMillis {
                for (step in steps) {
                    step.execute(context, print)
                }
            }

            plausible?.event(
                "${operationName}_success",
                "app://grindrplus/${operationName}_success",
                props = mapOf("time" to time)
            )

            onSuccess()
        }
    } catch (e: CancellationException) {
        print("$operationName was cancelled")
        showToast("$operationName was cancelled")
        plausible?.event(
            "${operationName}_cancelled",
            "app://grindrplus/${operationName}_cancelled"
        )
        throw e
    } catch (e: Exception) {
        val errorMsg = "$operationName failed: ${e.localizedMessage}"
        plausible?.event(
            "${operationName}_failed",
            "app://grindrplus/${operationName}_failure",
            props = mapOf("error" to e.message)
        )
        print(errorMsg)
        showToast(errorMsg)
        cleanupOnFailure()
        throw e
    }

    private fun cleanupOnFailure() {
        try {
            unzipDir.listFiles()?.forEach { it.delete() }
            outputDir.listFiles()?.forEach { it.delete() }

            if (bundleFile.exists() && bundleFile.length() <= 100) bundleFile.delete()
            if (modFile.exists() && modFile.length() <= 100) modFile.delete()
        } catch (_: Exception) {
        }
    }

    fun showToast(message: String) {
        CoroutineScope(Dispatchers.Main).launch {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }
}