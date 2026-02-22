package com.grindrplus.manager.installation.steps

import android.content.Context
import com.github.diamondminer88.zip.ZipWriter
import com.grindrplus.core.Constants
import com.grindrplus.manager.installation.BaseStep
import com.grindrplus.manager.installation.Print
import com.grindrplus.manager.utils.ManifestPatcher
import java.io.File
import java.util.zip.ZipFile

class AddModVersionInfoStep(
    private val dir: File,
    private val modVersion: String,
) : BaseStep() {
    override val name = "Add Mod version metadata"


    override suspend fun doExecute(
        context: Context,
        print: Print,
    ) {
        for (file in dir.listFiles()!!) {
            if (!file.name.endsWith(".apk"))
                continue

            val manifest = ZipFile(file)
                .use { zip ->
                    zip.getEntry("AndroidManifest.xml")?.let { entry ->
                        zip.getInputStream(entry).use { it.readBytes() }
                    }
                }
                ?: throw IllegalStateException("No manifest in ${file.name}")

            ZipWriter(file, true).use { zip ->
                var patchedManifestBytes = manifest

                if (file.name.contains("base")) {
                    print("Injecting MOD_VERSION into ${file.name}")
                    patchedManifestBytes = ManifestPatcher.addMetadata(
                        manifestBytes = patchedManifestBytes,
                        metadataName = Constants.MOD_VERSION_METADATA_KEY,
                        metadataValue = modVersion
                    )
                }

//                print("Deleting old AndroidManifest.xml in ${file.name}")
                zip.deleteEntry(
                    "AndroidManifest.xml",
                    /* fillVoid = */ true //TODO: maybe
                ) // Preserve alignment in libs apk

//                print("Adding patched AndroidManifest.xml in ${file.name}")
                zip.writeEntry("AndroidManifest.xml", patchedManifestBytes)
            }
        }
    }
}
