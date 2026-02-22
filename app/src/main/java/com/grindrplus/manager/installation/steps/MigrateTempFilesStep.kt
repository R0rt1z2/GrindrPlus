package com.grindrplus.manager.installation.steps

import android.content.Context
import com.grindrplus.manager.installation.BaseStep
import com.grindrplus.manager.installation.Installation
import com.grindrplus.manager.installation.Print
import java.io.File

/**
 * Migrate temporary files from "permanent" files to cache, so they can be cleaned up by the system
 */
class MigrateTempFilesStep(
) : BaseStep() {
    override val name = "Migrate temporary files"


    override suspend fun doExecute(
        context: Context,
        print: Print,
    ) {
        val permDirs = Installation.Directories(context.getExternalFilesDir(null))
        val tempDirs = Installation.Directories(context.externalCacheDir)

        val checkFile = File(tempDirs.base, "migrate.v1")

        if (!checkFile.exists()) {
            migrateAndDeleteFiles(permDirs, tempDirs)
            checkFile.createNewFile() // migration was successful
        } else {
            print("Migration already done")
        }

        checkLeftoverFiles(permDirs, print)
    }

    private fun migrateAndDeleteFiles(
        permDirs: Installation.Directories,
        tempDirs: Installation.Directories
    ) {
        val oldFileCount = permDirs.base.walk().toList().size

        // we are only expecting .zip files, delete older .xapk and .apkm files
        permDirs.base.listFiles()
            ?.filter { it.name.endsWith(".apkm") || it.name.endsWith(".xapk") }
            ?.forEach { it.delete() }

        permDirs.base.listFiles()
            ?.filter { it.name.endsWith(".zip") }
            ?.forEach {
                it.copyTo(File(tempDirs.input, it.name), overwrite = true)
                it.delete()
            }

        // unzip and output files are regenerated on every install, no need to keep them
        permDirs.unzip.listFiles()
            ?.forEach { it.deleteRecursively() }

        permDirs.output.walk()
            .forEach { it.deleteRecursively() }


        val newFileCount = permDirs.base.walk().toList().size

        print("Deleted or migrated ${oldFileCount - newFileCount} old files")
    }

    private fun checkLeftoverFiles(
        permDirs: Installation.Directories,
        print: Print
    ) {
        val tenMB = 10 * 1024 * 1024L
        val leftoverFiles = permDirs.base.walk()
            .filter { it.isFile && it.length() > tenMB }.toList()

        if (leftoverFiles.isNotEmpty()) {
            print("WARNING: Found ${leftoverFiles.size} leftover file(s) larger than 10MB in ${permDirs.base.absolutePath}:")
            leftoverFiles.forEach {
                print("  - ${it.relativeTo(permDirs.base)} (${it.length() / (1024 * 1024)} MB)")
            }
        }
    }

}