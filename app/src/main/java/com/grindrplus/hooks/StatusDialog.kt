package com.grindrplus.hooks

import android.app.AlertDialog
import android.content.Context
import android.os.Build
import android.view.View
import android.view.ViewGroup
import com.grindrplus.BuildConfig
import com.grindrplus.GrindrPlus
import com.grindrplus.core.Config
import com.grindrplus.utils.Hook
import com.grindrplus.utils.HookStage
import com.grindrplus.utils.hookConstructor

class StatusDialog : Hook(
    "Status Dialog",
    "Check whether GrindrPlus is alive or not"
) {
    private val tabView = "com.google.android.material.tabs.TabLayout\$TabView"

    override fun init() {
        findClass(tabView).hookConstructor(HookStage.AFTER) { param ->
            val tabView = param.thisObject() as View

            tabView.post {
                val parent = tabView.parent as? ViewGroup
                val position = parent?.indexOfChild(tabView) ?: -1

                if (position == 0) {
                    tabView.setOnLongClickListener { v ->
                        showGrindrPlusDialog(v.context)
                        false
                    }
                }
            }
        }
    }

    private fun showGrindrPlusDialog(context: Context) {
        GrindrPlus.currentActivity?.runOnUiThread {
            try {
                val packageManager = context.packageManager
                val packageInfo = packageManager.getPackageInfo(context.packageName, 0)

                val appVersionName = packageInfo.versionName
                val appVersionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    packageInfo.longVersionCode
                } else {
                    @Suppress("DEPRECATION")
                    packageInfo.versionCode.toLong()
                }

                val packageName = context.packageName
                val deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}"
                val androidVersion = "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"
                val moduleVersion = try {
                    BuildConfig.VERSION_NAME
                } catch (e: Exception) {
                    "Unknown"
                }

                val bridgeStatus = if (GrindrPlus.bridgeClient.isConnected()) {
                    "Connected"
                } else {
                    "Disconnected"
                }

                val androidDeviceIdStatus = (Config.get("android_device_id", "") as String)
                    .let { id -> if (id.isNotEmpty()) "Spoofing ($id)" else "Not Spoofing (stock)" }

                val message = buildString {
                    appendLine("GrindrPlus is active and running")
                    appendLine()
                    appendLine("App Information:")
                    appendLine("• Version: $appVersionName ($appVersionCode)")
                    appendLine("• Package: $packageName")
                    appendLine("• Android ID: $androidDeviceIdStatus")
                    appendLine()
                    appendLine("Module Information:")
                    appendLine("• GrindrPlus: $moduleVersion")
                    appendLine("• Bridge Status: $bridgeStatus")
                    appendLine()
                    appendLine("Device Information:")
                    appendLine("• Device: $deviceModel")
                    appendLine("• Android: $androidVersion")
                    appendLine()
                    appendLine("Long press this tab to show this dialog")
                }

                AlertDialog.Builder(context)
                    .setTitle("GrindrPlus")
                    .setMessage(message)
                    .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
                    .setIcon(android.R.drawable.ic_dialog_info)
                    .show()

            } catch (e: Exception) {
                AlertDialog.Builder(context)
                    .setTitle("GrindrPlus")
                    .setMessage("GrindrPlus is active and running\n\nError retrieving details: ${e.message}")
                    .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
                    .show()
            }
        }
    }
}