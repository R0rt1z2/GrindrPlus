package com.grindrplus.manager.utils

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
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

    private var _availableModVersions: List<String> = emptyList()

    fun init(context: Context) {
        refresh(context)
    }

    fun setAvailableModVersions(versions: List<String>, context: Context) {
        _availableModVersions = versions
        refresh(context)
    }

    private fun getAppName(packageName: String, packageManager: PackageManager): String {
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

    private fun calculateUpdateNeeded(versionName: String, modVersionName: String?): Boolean {
        if (isLSPosed()) {
            val isSupportedAppVersion = BuildConfig.TARGET_GRINDR_VERSION_NAMES.contains(versionName)
            val hasEmbeddedLSPatch = modVersionName != null
            return !isSupportedAppVersion || hasEmbeddedLSPatch
        } else {
            return false
        }
    }

    private fun calculateUpdateAvailable(versionName: String, modVersionName: String?): Boolean {
        if (isLSPosed())
            return false
        else {
            return modVersionName?.let { !_availableModVersions.contains(modVersionName) } ?: true
        }
    }

    fun refresh(context: Context): List<AppInfo> {
        val pm = context.packageManager
        val packages = pm.getInstalledPackages(PackageManager.GET_META_DATA)

        val installedApps = packages
            .filter { it.packageName == GRINDR_PACKAGE_NAME || isClone(it.packageName) }
            .map {
                val versionName = it.versionName!!
                val modVersion = it.applicationInfo?.metaData?.getString(MOD_VERSION_METADATA_KEY)
                val updateNeeded = calculateUpdateNeeded(versionName, modVersion)
                val updateAvailable = calculateUpdateAvailable(versionName, modVersion)

                AppInfo(
                    it.packageName,
                    getAppName(it.packageName, pm),
                    isClone = isClone(it.packageName),
                    isInstalled = true,
                    updateNeeded = updateNeeded,
                    updateAvailable = updateAvailable,
                    versionName = versionName,
                    modVersionName = modVersion
                )
            }

        val installedPackages = installedApps.map { it.packageName }.toSet()
        val settingsClones = Config.readRemoteConfig().optJSONObject("clones")?.keys()?.asSequence()?.toList() ?: emptyList()
        val uninstalledClones = settingsClones.filter { 
            isClone(it) && !installedPackages.contains(it) 
        }.map { packageName ->
            AppInfo(
                packageName,
                formatAppName(packageName),
                isClone = isClone(packageName),
                isInstalled = false
            )
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

    private fun isClone(packageName: String): Boolean {
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
        refresh(context)

        if (hasReachedMaxClones()) {
            Timber.e("Maximum number of clones ($MAX_CLONES) reached")
            return -1
        }

        var nextNum = 1

        while (apps.value.any { it.packageName == "$GRINDR_PACKAGE_PREFIX${numberToWords(nextNum).lowercase()}" }) {
            nextNum++
        }

        return nextNum
    }

    /**
     * Check if maximum number of clones is reached
     */
    fun hasReachedMaxClones(): Boolean {
        return getClones().size >= MAX_CLONES
    }

    fun getClones(): List<AppInfo> {
        return apps.value.filter { it.isClone }
    }

    /**
     * Data class to hold an app's package name and display name
     */
    data class AppInfo(
        val packageName: String,
        val appName: String,
        val isClone: Boolean,
        val isInstalled: Boolean = true,
        val updateNeeded: Boolean = false,
        val updateAvailable: Boolean = false,
        val versionName: String? = null,
        val modVersionName: String? = null
    )
}