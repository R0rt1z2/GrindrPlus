package com.grindrplus.manager.installation

import android.content.Context
import android.widget.Toast
import com.grindrplus.manager.MainActivity.Companion.plausible
import com.grindrplus.manager.installation.steps.CheckStorageSpaceStep
import com.grindrplus.manager.installation.steps.CloneGrindrStep
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
    modUrl: String,
    grindrUrl: String,
    private val mapsApiKey: String?
) {
    private val keyStoreUtils = KeyStoreUtils(context)
    private val folder = context.getExternalFilesDir(null)
        ?: throw IOException("External files directory not available")
    private val unzipFolder = File(folder, "splitApks/").also { it.mkdirs() }
    private val outputDir = File(folder, "LSPatchOutput/").also { it.mkdirs() }
    private val modFile = File(folder, "mod-$version.zip")
    private val bundleFile = File(folder, "grindr-$version.zip")

    private val installStep = InstallApkStep(outputDir)
    private val patchApkStep = PatchApkStep(unzipFolder, outputDir, modFile, keyStoreUtils.keyStore, mapsApiKey)
    private val commonSteps = listOf(
        // Order matters
        CheckStorageSpaceStep(folder),
        DownloadStep(bundleFile, grindrUrl, "Grindr bundle"),
        DownloadStep(modFile, modUrl, "mod"),
        ExtractBundleStep(bundleFile, unzipFolder),
    )

    suspend fun install(print: Print) = performOperation(
        steps = commonSteps + listOf(patchApkStep, installStep),
        operationName = "install-$version",
        print = print,
    )

    suspend fun cloneGrindr(
        packageName: String,
        appName: String,
        debuggable: Boolean,
        embedLSpatch: Boolean,
        print: Print,
    ) = performOperation(
        steps = commonSteps + listOf(
            CloneGrindrStep(
                folder = unzipFolder,
                packageName = packageName,
                appName = appName,
                debuggable = debuggable,
            ),
            SignClonedGrindrApk(keyStoreUtils, unzipFolder),
            PatchApkStep(unzipFolder, outputDir, modFile,
                keyStoreUtils.keyStore, mapsApiKey, embedLSpatch),
            installStep
        ),
        operationName = "clone",
        print = print,
    )

    suspend fun installCustom(
        bundleFile: File,
        modFile: File,
        print: Print
    ) = performOperation(
        steps = listOf(
            CheckStorageSpaceStep(folder),
            ExtractBundleStep(bundleFile, unzipFolder),
            PatchApkStep(unzipFolder, outputDir, modFile, keyStoreUtils.keyStore, mapsApiKey),
            InstallApkStep(outputDir)
        ),
        operationName = "custom_install",
        print = print
    )

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