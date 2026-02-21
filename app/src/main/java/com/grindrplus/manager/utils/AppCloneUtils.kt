package com.grindrplus.manager.utils

import android.content.Context
import android.content.pm.PackageManager
import timber.log.Timber
import com.grindrplus.manager.installation.steps.numberToWords
import com.grindrplus.core.Config

object AppCloneUtils {
    const val MAX_CLONES = 5
    const val GRINDR_PACKAGE_PREFIX = "com.grindrapp.android."
    const val GRINDR_PACKAGE_NAME = "com.grindrapp.android"
    
    private var cachedClones: List<AppInfo>? = null

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

    fun getExistingClones(context: Context): List<AppInfo> {
        return cachedClones?.map {
            it.copy(needsUpdate = !com.grindrplus.BuildConfig.TARGET_GRINDR_VERSION_NAMES.contains(it.versionName))
        } ?: refreshCache(context)
    }

    fun refreshCache(context: Context): List<AppInfo> {
        val pm = context.packageManager
        val packages = pm.getInstalledPackages(0)

        cachedClones = packages
            .filter { isClone(it.packageName) }
            .map {
                val vName = it.versionName
                val updateNeeded = !com.grindrplus.BuildConfig.TARGET_GRINDR_VERSION_NAMES.contains(vName)
                AppInfo(
                    it.packageName,
                    getAppName(it.packageName, pm),
                    isInstalled = true,
                    needsUpdate = updateNeeded,
                    versionName = vName
                )
            }
        return getExistingClones(context) // re-apply needsUpdate if needed, or just return cachedClones
    }

    /**
     * Returns union of installed clones AND uninstalled clones that still have stored configuration
     */
    fun getKnownClones(context: Context): List<AppInfo> {
        val installed = getExistingClones(context)
        val installedPackages = installed.map { it.packageName }.toSet()
        
        val settingsClones = Config.readRemoteConfig().optJSONObject("clones")?.keys()?.asSequence()?.toList() ?: emptyList()
        val uninstalled = settingsClones.filter { 
            isClone(it) && !installedPackages.contains(it) 
        }.map { packageName ->
            AppInfo(packageName, formatAppName(packageName), isInstalled = false)
        }

        return installed + uninstalled
    }

    fun isClone(packageName: String): Boolean {
        return packageName.startsWith(GRINDR_PACKAGE_PREFIX) && packageName != GRINDR_PACKAGE_NAME
    }

    fun formatAppNameDescriptive(packageName: String): String {
        if (packageName == GRINDR_PACKAGE_NAME) return "Main Grindr App"

        val suffix = packageName.removePrefix(GRINDR_PACKAGE_PREFIX)
        return "Clone ${suffix.replaceFirstChar { it.uppercase() }}"
    }

    fun formatAppName(packageName: String): String {
        if (packageName == GRINDR_PACKAGE_NAME) return "Grindr"

        val suffix = packageName.removePrefix(GRINDR_PACKAGE_PREFIX)
        return "Grindr ${suffix.replaceFirstChar { it.uppercase() }}"
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
    data class AppInfo(
        val packageName: String, 
        val appName: String, 
        val isInstalled: Boolean = true,
        val needsUpdate: Boolean = false,
        val versionName: String? = null
    )
}