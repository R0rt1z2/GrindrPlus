package com.grindrplus.manager.installation.steps

import android.content.Context
import com.grindrplus.manager.installation.BaseStep
import com.grindrplus.manager.installation.Print
import com.grindrplus.manager.utils.SessionInstaller
import java.io.File
import java.io.IOException

// Last
class InstallApkStep(
    private val outputDir: File
) : BaseStep() {
    override val name = "Installing Grindr APK"

    override suspend fun doExecute(context: Context, print: Print) {
        val patchedFiles = outputDir.listFiles()?.toList() ?: emptyList()
        if (patchedFiles.isEmpty()) {
            throw IOException("No patched APK files found for installation")
        }

        val filteredApks = patchedFiles.filter { 
            it.name.endsWith(".apk") && it.exists() && it.length() > 0 
        }
        
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
}