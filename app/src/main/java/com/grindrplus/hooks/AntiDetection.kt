package com.grindrplus.hooks

import com.grindrplus.utils.Hook
import com.grindrplus.utils.HookStage
import com.grindrplus.utils.hook
import com.grindrplus.utils.hookConstructor

class AntiDetection : Hook(
    "Anti Detection",
    "Hides root, emulator, and environment detections"
) {
    private val grindrMiscClass = "mg.n" // search for '"sdk_gphone", "emulator", "simulator", "google_sdk"'
    private val devicePropertiesCollector = "siftscience.android.DevicePropertiesCollector"
    private val commonUtils = "com.google.firebase.crashlytics.internal.common.CommonUtils"
    private val osData = "com.google.firebase.crashlytics.internal.model.AutoValue_StaticSessionData_OsData"

    override fun init() {
        findClass(grindrMiscClass)
            .hook("O", HookStage.AFTER) { param ->
                param.setResult(false)
            }

        findClass(commonUtils)
            .hook("isRooted", HookStage.BEFORE) { param ->
                param.setResult(false)
            }

        findClass(commonUtils)
            .hook("isEmulator", HookStage.BEFORE) { param ->
                param.setResult(false)
            }

        findClass(commonUtils)
            .hook("isAppDebuggable", HookStage.BEFORE) { param ->
                param.setResult(false)
            }

        findClass(devicePropertiesCollector)
            .hook("existingRWPaths", HookStage.BEFORE) { param ->
                param.setResult(emptyList<String>())
            }

        findClass(devicePropertiesCollector)
            .hook("existingRootFiles", HookStage.BEFORE) { param ->
                param.setResult(emptyList<String>())
            }

        findClass(devicePropertiesCollector)
            .hook("existingRootPackages", HookStage.BEFORE) { param ->
                param.setResult(emptyList<String>())
            }

        findClass(devicePropertiesCollector)
            .hook("existingDangerousProperties", HookStage.BEFORE) { param ->
                param.setResult(emptyList<String>())
            }

        findClass(osData)
            .hookConstructor(HookStage.BEFORE) { param ->
                param.setArg(2, false) // isRooted
            }
    }
}