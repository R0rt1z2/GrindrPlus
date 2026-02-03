package com.grindrplus.manager.utils

import android.content.Context
import android.content.pm.PackageManager
import timber.log.Timber
import com.grindrplus.manager.installation.steps.numberToWords

object AppCloneUtils {
    const val MAX_CLONES = 5
    const val GRINDR_PACKAGE_PREFIX = "com.grindrapp.android."
    const val GRINDR_PACKAGE_NAME = "com.grindr"

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

    fun getAppName(packageName: String, packageManager: PackageManager): String {
        try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            val appName = packageManager.getApplicationLabel(appInfo).toString()

            if (appName != packageName && appName.isNotEmpty())
                return appName

        } catch (_: Exception) {
        }

        val suffix = packageName.removePrefix(GRINDR_PACKAGE_PREFIX)
        return "Grindr " + suffix.replaceFirstChar { it.uppercase() }
    }

    /**
     * Get existing Grindr clones to determine next suffix number
     */
    fun getExistingClones(context: Context): List<AppInfo> {
        val pm = context.packageManager
        val packages = pm.getInstalledPackages(0)
        return packages
            .filter { it.packageName.startsWith(GRINDR_PACKAGE_PREFIX) }
            .map {
                AppInfo(
                    it.packageName,
                    getAppName(it.packageName, pm)
                )
            }
    }

    /**
     * Get next available clone number
     * Returns -1 if maximum number of clones is reached
     */
    fun getNextCloneNumber(context: Context): Int {
        val clones = getExistingClones(context)

        if (clones.size >= MAX_CLONES) {
            Timber.e("Maximum number of clones ($MAX_CLONES) reached")
            return -1
        }

        var nextNum = 1

        while (clones.any { it.packageName == "$GRINDR_PACKAGE_PREFIX${numberToWords(nextNum).lowercase()}" }) {
            nextNum++
        }

        return nextNum
    }

    /**
     * Check if maximum number of clones is reached
     */
    fun hasReachedMaxClones(context: Context): Boolean {
        return getExistingClones(context).size >= MAX_CLONES
    }

    /**
     * Data class to hold an app's package name and display name
     */
    data class AppInfo(val packageName: String, val appName: String)
}