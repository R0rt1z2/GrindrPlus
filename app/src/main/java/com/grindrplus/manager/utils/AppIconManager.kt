package com.grindrplus.manager.utils

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import timber.log.Timber

class AppIconManager(private val context: Context) {

    companion object {
        const val DEFAULT_ICON = "default"
        const val DISCREET_ICON = "disguised"
    }

    fun changeAppIcon(iconType: String): Boolean {
        return try {
            val packageManager = context.packageManager
            val componentName = when (iconType) {
                DEFAULT_ICON -> ComponentName(context, "com.grindrplus.manager.MainActivity")
                DISCREET_ICON -> ComponentName(context, "com.grindrplus.manager.MainActivityAlias")
                else -> return false
            }

            disableAllAliases()

            packageManager.setComponentEnabledSetting(
                componentName,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
            )

            true
        } catch (e: Exception) {
            Timber.e(e, "Failed to change app icon")
            false
        }
    }

    private fun disableAllAliases() {
        val packageManager = context.packageManager
        val aliasesToDisable = listOf(
            ComponentName(context, "com.grindrplus.manager.MainActivity"),
            ComponentName(context, "com.grindrplus.manager.MainActivityAlias")
        )

        for (alias in aliasesToDisable) {
            packageManager.setComponentEnabledSetting(
                alias,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
            )
        }
    }
}