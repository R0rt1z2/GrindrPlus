package com.grindrplus.manager.installation

import android.content.Context
import android.net.Uri
import android.widget.Toast
import com.grindrplus.manager.MainActivity.Companion.plausible
import com.grindrplus.manager.installation.steps.CheckStorageSpaceStep
import com.grindrplus.manager.installation.steps.CloneGrindrStep
import com.grindrplus.manager.installation.steps.CopyStep
import com.grindrplus.manager.installation.steps.DownloadStep
import com.grindrplus.manager.installation.steps.ExtractBundleStep
import com.grindrplus.manager.installation.steps.InstallApkStep
import com.grindrplus.manager.installation.steps.PatchApkStep
import com.grindrplus.manager.installation.steps.SignClonedGrindrApk
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
    val embedLSPatch: Boolean,
) {
    private val keyStoreUtils = KeyStoreUtils(context)
    private val folder = context.externalCacheDir
        ?: throw IOException("External files directory not available")
    private val unzipFolder = File(folder, "splitApks/").also { it.mkdirs() }
    private val outputDir = File(folder, "LSPatchOutput/").also { it.mkdirs() }
    private val modFile = File(folder, "mod-$version.zip")
    private val bundleFile = File(folder, "grindr-$version.zip")

    sealed interface SourceFiles {
        class Local(val bundleFileUri: Uri, val modFileUri: Uri?) : SourceFiles
        class Download(val bundleUrl: String, val modUrl: String?) : SourceFiles
    }

    data class AppInfoOverride(
        val packageName: String,
        val appName: String,
        val mapsApiKey: String?
    )

    suspend fun start(
        print: Print
    ) {
        val steps = mutableListOf<Step>().apply {
            add(CheckStorageSpaceStep(folder))

            when (sourceFiles) {
                is SourceFiles.Download -> {
                    add(DownloadStep(bundleFile, sourceFiles.bundleUrl, "Grindr bundle"))
                    sourceFiles.modUrl?.let { add(DownloadStep(modFile, it, "mod")) }
                }
                is SourceFiles.Local -> {
                    add(CopyStep(bundleFile, sourceFiles.bundleFileUri, "Grindr bundle"))
                    sourceFiles.modFileUri?.let { add(CopyStep(modFile, sourceFiles.modFileUri, "mod")) }
                }
            }

            add(ExtractBundleStep(bundleFile, unzipFolder))

            if (appInfo != null)
                add(CloneGrindrStep(unzipFolder, appInfo.packageName, appInfo.appName, debuggable = false))

            val mapsApiKey = appInfo?.mapsApiKey
            add(PatchApkStep(unzipFolder, outputDir, modFile, keyStoreUtils.keyStore, mapsApiKey, embedLSPatch))

            if (appInfo != null || embedLSPatch)
                add(SignClonedGrindrApk(keyStoreUtils, outputDir))

            add(InstallApkStep(outputDir))
        }

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
                    print("Executing step: ${step.name}")

                    val time = measureTimeMillis {
                        step.execute(context, print)
                    }

                    print("Step ${step.name} completed in ${time / 1000} seconds")
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
            unzipFolder.listFiles()?.forEach { it.delete() }
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