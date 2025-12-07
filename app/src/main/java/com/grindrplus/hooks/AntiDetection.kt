package com.grindrplus.hooks

import com.grindrplus.utils.Hook
import com.grindrplus.utils.HookStage
import com.grindrplus.utils.hook
import com.grindrplus.utils.hookConstructor

// supported version: 25.20.0
class AntiDetection : Hook(
    "Anti Detection",
    "Hides root, emulator, and environment detections"
) {

	private val singleStartActivity = "com.grindrapp.android.ui.base.SingleStartActivity"
	private val appLovinSdkClass = "com.applovin.sdk.AppLovinSdkUtils"
	private val pubnativeUtils = "net.pubnative.lite.sdk.vpaid.utils.Utils"
	private val facebookAppEventClass = "com.facebook.appevents.internal.AppEventUtility"
    private val devicePropertiesCollector = "siftscience.android.DevicePropertiesCollector"
    private val commonUtils = "com.google.firebase.crashlytics.internal.common.CommonUtils"
    private val crashlyticsOsData = "com.google.firebase.crashlytics.internal.model.AutoValue_StaticSessionData_OsData"
	private val crashlyticsDeviceData = "com.google.firebase.crashlytics.internal.model.AutoValue_StaticSessionData_DeviceData"

    override fun init() {
		findClass(singleStartActivity)
			.hook("N", HookStage.AFTER) { param ->
				param.setResult(false)
			}

		findClass(appLovinSdkClass)
			.hook("isEmulator", HookStage.AFTER) { param ->
				param.setResult(false)
			}

		findClass(pubnativeUtils)
			.hook("isEmulator", HookStage.AFTER) { param ->
				param.setResult(false)
			}

		findClass(facebookAppEventClass)
			.hook("isEmulator", HookStage.AFTER) { param ->
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

        findClass(crashlyticsOsData)
            .hookConstructor(HookStage.BEFORE) { param ->
                param.setArg(2, false) // search for 'this.isRooted = ' in constructor
            }

		findClass(crashlyticsDeviceData)
			.hookConstructor(HookStage.BEFORE) { param ->
				param.setArg(5, false) // search for 'this.isEmulator = ' in constructor
			}
    }
}