package com.grindrplus.manager.installation.steps

import android.content.Context
import com.github.diamondminer88.zip.ZipReader
import com.github.diamondminer88.zip.ZipWriter
import com.grindrplus.manager.installation.BaseStep
import com.grindrplus.manager.installation.Print
import com.grindrplus.manager.utils.ManifestPatcher
import java.io.File
import java.util.zip.ZipFile

class CloneGrindrStep(
    val folder: File,
    val packageName: String,
    val appName: String,
    val debuggable: Boolean,
) : BaseStep() {
    override suspend fun doExecute(
        context: Context,
        print: Print,
    ) {
        print("Cloning Grindr APK...")

        for (file in folder.listFiles()!!) {
            if (!file.name.endsWith(".apk")) {
                print("Skipping ${file.name} as it is not an APK")
                continue
            }

            val manifest = ZipFile(file)
                .use { zip ->
                    zip.getEntry("AndroidManifest.xml")?.let { entry ->
                        zip.getInputStream(entry).use { it.readBytes() }
                    }
                }
                ?: throw IllegalStateException("No manifest in ${file.name}")

            ZipWriter(file, true).use { zip ->
                print("Changing package and app name in ${file.name}")
                val patchedManifestBytes = if (file.name.contains("base")) {
                    ManifestPatcher.patchManifest(
                        manifestBytes = manifest,
                        packageName = packageName,
                        appName = appName,
                        debuggable = debuggable,
                    )
                } else {
                    print("Changing package name in ${file.name}")
                    ManifestPatcher.renamePackage(manifest, packageName)
                }

                print("Deleting old AndroidManifest.xml in ${file.name}")
                zip.deleteEntry(
                    "AndroidManifest.xml",
                    /* fillVoid = */ true //TODO: maybe
                ) // Preserve alignment in libs apk

                print("Adding patched AndroidManifest.xml in ${file.name}")
                zip.writeEntry("AndroidManifest.xml", patchedManifestBytes)
            }
        }

        print("Cloning Grindr APK completed")
    }

    override val name = "Clone grindr apk"
}