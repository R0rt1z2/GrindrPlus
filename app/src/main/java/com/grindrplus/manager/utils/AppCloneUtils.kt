package com.grindrplus.manager.utils

import android.content.Context
import android.content.pm.PackageManager
import timber.log.Timber
import com.grindrplus.core.Config
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.grindrplus.BuildConfig

object AppCloneUtils {
    const val MAX_CLONES = 5
    const val GRINDR_PACKAGE_PREFIX = "com.grindrapp.android."
    const val GRINDR_PACKAGE_NAME = "com.grindrapp.android"
    
    private val _apps = MutableStateFlow<List<AppInfo>>(emptyList())
    val apps: StateFlow<List<AppInfo>> = _apps.asStateFlow()

    fun init(context: Context) {
        refresh(context)
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

    fun findApp(packageName: String) =
        apps.value.find { it.packageName == packageName }

    fun getExistingClones(context: Context): List<AppInfo> {
        val allApps = _apps.value
        if (allApps.isEmpty()) {
            return refresh(context).filter { isClone(it.packageName) && it.isInstalled }
        }
        return allApps.filter { isClone(it.packageName) && it.isInstalled }.map {
            it.copy(needsUpdate = calculateUpdateNeeded(it.versionName))
        }
    }

    private fun calculateUpdateNeeded(versionName: String?): Boolean {
        // TODO check in downloaded json versions file
        return versionName?.let { !BuildConfig.TARGET_GRINDR_VERSION_NAMES.contains(versionName) }
            ?: false
    }

    fun refresh(context: Context): List<AppInfo> {
        val pm = context.packageManager
        val packages = pm.getInstalledPackages(0)

        val installedApps = packages
            .filter { it.packageName == GRINDR_PACKAGE_NAME || isClone(it.packageName) }
            .map {
                val vName = it.versionName
                val updateNeeded = calculateUpdateNeeded(vName)
                AppInfo(
                    it.packageName,
                    getAppName(it.packageName, pm),
                    isInstalled = true,
                    needsUpdate = updateNeeded,
                    versionName = vName
                )
            }
        
        val installedPackages = installedApps.map { it.packageName }.toSet()
        val settingsClones = Config.readRemoteConfig().optJSONObject("clones")?.keys()?.asSequence()?.toList() ?: emptyList()
        val uninstalledClones = settingsClones.filter { 
            isClone(it) && !installedPackages.contains(it) 
        }.map { packageName ->
            AppInfo(packageName, formatAppName(packageName), isInstalled = false)
        }

        val allApps = (installedApps + uninstalledClones).sortedWith(compareBy { app ->
            val pkg = app.packageName
            val cloneSuffix = if (pkg == GRINDR_PACKAGE_NAME) "" else pkg.removePrefix(GRINDR_PACKAGE_PREFIX)
            when (cloneSuffix) {
                "" -> 0
                "one" -> 1
                "two" -> 2
                "three" -> 3
                "four" -> 4
                "five" -> 5
                else -> 100 // Custom clones or higher numbers
            }
        })
        _apps.value = allApps
        return allApps
    }

    fun isClone(packageName: String): Boolean {
        return packageName.startsWith(GRINDR_PACKAGE_PREFIX) && packageName != GRINDR_PACKAGE_NAME
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