package com.grindrplus.manager.utils

import android.content.Context
import android.content.pm.PackageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import com.grindrplus.core.Constants.GRINDR_PACKAGE_NAME
import com.grindrplus.manager.installation.steps.numberToWords
import java.io.FileOutputStream
import java.io.IOException

object AppCloneUtils {
    /**
     * Check if Grindr is installed on the device
     */
    fun isGrindrInstalled(context: Context): Boolean {
        return try {
            context.packageManager.getPackageInfo(GRINDR_PACKAGE_NAME, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    /**
     * Get existing Grindr clones to determine next suffix number
     */
    fun getExistingClones(context: Context): List<String> {
        val pm = context.packageManager
        val packages = pm.getInstalledPackages(0)
        return packages
            .filter { it.packageName.startsWith("$GRINDR_PACKAGE_NAME.") }
            .map { it.packageName }
    }

    /**
     * Get next available clone number
     */
    fun getNextCloneNumber(context: Context): Int {
        val clones = getExistingClones(context)
        var nextNum = 1

        while (clones.contains("$GRINDR_PACKAGE_NAME.$nextNum") ||
            clones.any { it.endsWith(".${numberToWords(nextNum).lowercase()}") }) {
            nextNum++
        }

        return nextNum
    }
}