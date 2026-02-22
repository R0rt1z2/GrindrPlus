package com.grindrplus.manager.installation.steps

import android.content.Context
import com.grindrplus.manager.installation.BaseStep
import com.grindrplus.manager.installation.Print
import com.reandroid.apk.ApkModule
import com.reandroid.xml.StyleDocument
import java.io.File
import java.io.IOException

/**
 * replace maps api key in apk in-place
 * modifies existing apk file, does not copy to output folder
 */
class ReplaceMapsApiKeyStep(
    private val dir: File,
    private val customMapsApiKey: String,
) : BaseStep() {
    override val name = "Replace Maps API key"

    private companion object {
        const val MAPS_API_KEY_NAME = "com.google.android.geo.API_KEY"
    }

    override suspend fun doExecute(context: Context, print: Print) {
        val apkFiles = dir.listFiles()?.filter {
            it.name.endsWith(".apk") && it.exists() && it.length() > 0
        }

        if (apkFiles.isNullOrEmpty()) {
            throw IOException("No valid APK files found to patch")
        }

        try {
            print("Attempting to apply custom Maps API key...")

            val baseApk = apkFiles.find {
                it.name == "base.apk" || it.name.startsWith("base.apk-")
            } ?: apkFiles.first()

            print("Using ${baseApk.name} for Maps API key modification")
            val apkModule = ApkModule.loadApkFile(baseApk)

            val metaElements =
                apkModule.androidManifest.applicationElement.getElements { element ->
                    element.name == "meta-data"
                }

            var found = false
            while (metaElements.hasNext() && !found) {
                val element = metaElements.next()
                val nameAttr = element.searchAttributeByName("name")

                if (nameAttr != null && nameAttr.valueString == MAPS_API_KEY_NAME) {
                    val valueAttr = element.searchAttributeByName("value")
                    if (valueAttr != null) {
                        print("Found Maps API key element, replacing with custom key")
                        valueAttr.setValueAsString(
                            StyleDocument.parseStyledString(
                                customMapsApiKey
                            )
                        )
                        found = true
                    }
                }
            }

            if (found) {
                print("Successfully replaced Maps API key, saving APK")
                apkModule.writeApk(baseApk)
            } else {
                print("Maps API key element not found in manifest, skipping replacement")
            }

        } catch (e: Exception) {
            print("Error applying Maps API key: ${e.message}")
        }
    }
}