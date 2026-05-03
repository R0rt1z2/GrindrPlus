package com.grindrplus.manager.installation.steps

import android.content.Context
import com.grindrplus.manager.installation.BaseStep
import com.grindrplus.manager.installation.Print
import com.reandroid.apk.ApkModule
import com.reandroid.xml.StyleDocument
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
    private val keyStore: File,
    private val customMapsApiKey: String?,
    private val embedLSPatch: Boolean = true
) : BaseStep() {
    override val name = "Patching Grindr APK"

    private companion object {
        const val MAPS_API_KEY_NAME = "com.google.android.geo.API_KEY"
    }

    override suspend fun doExecute(context: Context, print: Print) {
        print("Cleaning output directory...")
        outputDir.listFiles()?.forEach { it.delete() }

        val apkFiles = unzipFolder.listFiles()
            ?.filter { it.name.endsWith(".apk") && it.exists() && it.length() > 0 }

        if (apkFiles.isNullOrEmpty()) {
            throw IOException("No valid APK files found to patch")
        }

        if (customMapsApiKey != null) {
            injectMapsApiKey(apkFiles, customMapsApiKey, print)
        }

        if (!embedLSPatch) {
            copyApksToOutput(apkFiles, print)
            return
        }

        runLSPatch(apkFiles, print)
    }

    private fun injectMapsApiKey(apkFiles: List<File>, apiKey: String, print: Print) {
        try {
            print("Attempting to apply custom Maps API key...")
            val baseApk = apkFiles.find {
                it.name == "base.apk" || it.name.startsWith("base.apk-")
            } ?: apkFiles.first()

            print("Using ${baseApk.name} for Maps API key modification")
            val apkModule = ApkModule.loadApkFile(baseApk)

            if (replaceApiKeyInManifest(apkModule, apiKey, print)) {
                print("Successfully replaced Maps API key, saving APK")
                apkModule.writeApk(baseApk)
            } else {
                throw IOException(
                    "Maps API key element ($MAPS_API_KEY_NAME) not found in manifest. " +
                    "In-app Maps will not work. Check that the base APK is unmodified " +
                    "or leave the Maps API key field blank to skip injection."
                )
            }
        } catch (e: IOException) {
            throw e
        } catch (e: Exception) {
            print("Error applying Maps API key: ${e.message}")
        }
    }

    private fun replaceApiKeyInManifest(apkModule: ApkModule, apiKey: String, print: Print): Boolean {
        val metaElements = apkModule.androidManifest.applicationElement.getElements { element ->
            element.name == "meta-data"
        }

        while (metaElements.hasNext()) {
            val element = metaElements.next()
            val nameAttr = element.searchAttributeByName("name") ?: continue
            if (nameAttr.valueString != MAPS_API_KEY_NAME) continue

            val valueAttr = element.searchAttributeByName("value") ?: continue
            print("Found Maps API key element, replacing with custom key")
            valueAttr.setValueAsString(StyleDocument.parseStyledString(apiKey))
            return true
        }
        return false
    }

    private fun copyApksToOutput(apkFiles: List<File>, print: Print) {
        print("Skipping LSPatch as embedLSPatch is disabled")

        apkFiles.forEach { apkFile ->
            apkFile.copyTo(File(outputDir, apkFile.name), overwrite = true)
            print("Copied ${apkFile.name} to output directory")
        }

        val copiedFiles = outputDir.listFiles()
        if (copiedFiles.isNullOrEmpty()) {
            throw IOException("Copying APKs failed - no output files generated")
        }

        print("Copying completed successfully")
        reportFiles(copiedFiles, print)
    }

    private suspend fun runLSPatch(apkFiles: List<File>, print: Print) {
        print("Starting LSPatch process with ${apkFiles.size} APK files")
        print("Using mod file: ${modFile.absolutePath}")
        print("Using keystore: ${keyStore.absolutePath}")

        val apkFilePaths = apkFiles.map { it.absolutePath }.toTypedArray()

        withContext(Dispatchers.IO) {
            LSPatch(
                buildPrintLogger(print),
                *apkFilePaths,
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
        reportFiles(patchedFiles, print)
    }

    private fun buildPrintLogger(print: Print): Logger = object : Logger() {
        override fun d(message: String?) { message?.let { print("DEBUG: $it") } }
        override fun i(message: String?) { message?.let { print("INFO: $it") } }
        override fun e(message: String?) { message?.let { print("ERROR: $it") } }
    }

    private fun reportFiles(files: Array<File>, print: Print) {
        print("Generated ${files.size} files")
        files.forEachIndexed { index, file ->
            print("  ${index + 1}. ${file.name} (${file.length() / 1024}KB)")
        }
    }
}
