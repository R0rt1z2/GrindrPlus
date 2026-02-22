package com.grindrplus.manager.installation.steps

import android.content.Context
import com.grindrplus.manager.installation.BaseStep
import com.grindrplus.manager.installation.Installation
import com.grindrplus.manager.installation.Print
import java.io.File

/**
 * Cleanup temporary install files
 */
class CleanupStep(
    private val dirs: Installation.Directories,
    private vararg val aditionalFiles: File
) : BaseStep() {
    override val name = "Migrate temporary files"


    override suspend fun doExecute(
        context: Context,
        print: Print,
    ) {
        // unzip and output files are regenerated on every install, no need to keep them
        val unzipFiles = dirs.unzip.listFiles()?.toList() ?: listOf()
        val outputFiles = dirs.output.listFiles()?.toList() ?: listOf()
        val additionalFilesList = aditionalFiles.filter { it.exists() && it.length() <= 100 }

        val allFiles = unzipFiles + outputFiles + additionalFilesList
        allFiles.forEach { it.deleteRecursively() }

        print("Deleted ${allFiles.size} temporary files")
    }
}