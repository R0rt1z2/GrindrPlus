package com.grindrplus.hooks

import com.grindrplus.core.loge
import com.grindrplus.utils.Hook
import com.grindrplus.utils.HookStage
import com.grindrplus.utils.hook
import com.grindrplus.utils.hookConstructor
import de.robv.android.xposed.XposedHelpers.newInstance
import de.robv.android.xposed.XposedHelpers.setObjectField

class DisableBoosting : Hook(
    "Disable boosting",
    "Get rid of all upsells related to boosting"
) {
    private val drawerProfileUiState = "sc0.j\$a" // search for 'DrawerProfileUiState(showBoostMeButton='
    private val radarUiModel = "r50.a\$a" // search for 'RadarUiModel(boostButton='
    private val fabUiModel = "com.grindrapp.android.boost2.presentation.model.FabUiModel"
    private val rightNowMicrosFabUiModel =
        "com.grindrapp.android.rightnow.presentation.model.RightNowMicrosFabUiModel"

    private val boostStateClass =
        "com.grindrapp.android.ui.drawer.model.MicrosDrawerItemState\$Unavailable"

	private val navbarClass = "com.grindrapp.android.home.presentation.model.HomeScreenBottomNavigationUiModel"
	private val smallPersistentVector = "kotlinx.collections.immutable.implementations.immutableList.SmallPersistentVector"

    override fun init() {
        try {
            findClass(drawerProfileUiState).hookConstructor(HookStage.AFTER) { param ->
                setObjectField(param.thisObject(), "a", false) // showBoostMeButton
                setObjectField(
                    param.thisObject(),
                    "e",
                    newInstance(findClass(boostStateClass))
                ) // boostButtonState
                setObjectField(
                    param.thisObject(),
                    "f",
                    newInstance(findClass(boostStateClass))
                ) // roamButtonState
                setObjectField(param.thisObject(), "c", false) // showRNBoostCard
                setObjectField(param.thisObject(), "i", null) // showDayPassItem
                setObjectField(param.thisObject(), "j", null) // unlimitedWeeklySubscriptionItem
                setObjectField(param.thisObject(), "t", false) // isRightNowAvailable
                setObjectField(param.thisObject(), "v", false) // showMegaBoost
            }
        } catch (e: Throwable) {
            loge("DisableBoosting: failed to hook DrawerProfileUiState: ${e.message}")
        }

        try {
            findClass(radarUiModel).hookConstructor(HookStage.AFTER) { param ->
                setObjectField(param.thisObject(), "a", null) // boostButton
                setObjectField(param.thisObject(), "b", null) // roamButton
            }
        } catch (e: Throwable) {
            loge("DisableBoosting: failed to hook RadarUiModel: ${e.message}")
        }

        try {
            findClass(fabUiModel).hookConstructor(HookStage.AFTER) { param ->
                setObjectField(param.thisObject(), "isVisible", false) // isVisible
            }
        } catch (e: Throwable) {
            loge("DisableBoosting: failed to hook FabUiModel: ${e.message}")
        }

        try {
            findClass(rightNowMicrosFabUiModel).hookConstructor(HookStage.AFTER) { param ->
                setObjectField(param.thisObject(), "isBoostFabVisible", false) // isBoostFabVisible
                setObjectField(param.thisObject(), "isClickEnabled", false) // isClickEnabled
                setObjectField(param.thisObject(), "isFabVisible", false) // isFabVisible
            }
        } catch (e: Throwable) {
            loge("DisableBoosting: failed to hook RightNowMicrosFabUiModel: ${e.message}")
        }

        val spvConstructor = findClass(smallPersistentVector).constructors[0]

        try {
            findClass(navbarClass).hookConstructor(HookStage.BEFORE) { param ->
                val routeList = param.args()[2] as List<*>
                val newRouteArray =	routeList.filter { it?.javaClass?.simpleName != "Store" }.toTypedArray()
                val newRouteList = spvConstructor.newInstance(newRouteArray)

                param.setArg(2, newRouteList)
            }
        } catch (e: Throwable) {
            loge("DisableBoosting: failed to hook HomeScreenBottomNavigationUiModel: ${e.message}")
        }

        // Anonymous functions that invoke the boost/taps tooltip popups.
        // Obfuscated names change every Grindr release — to find the new names open the APK in
        // jadx and search smali for the string literals listed below; the containing class is the target.
        //   showTapsAndViewedMePopup lambdas:
        //     search → 'com.grindrapp.android.ui.home.HomeActivity$showTapsAndViewedMePopup$1$1'
        //     search → 'HomeActivity.showTapsAndViewedMePopup.<anonymous> (HomeActivity.kt'
        //     search → 'HomeActivity.showTapsAndViewedMePopup.<anonymous>.<anonymous> (HomeActivity.kt'
        //   subscribeForBoostRedeem lambda:
        //     search → 'com.grindrapp.android.ui.home.HomeActivity$subscribeForBoostRedeem$1'
        //   Last known obfuscated names: "cd0.j2" (pre-25.20.0), "Il.w0" (subscribeForBoostRedeem)
        //   Update this list after each Grindr version bump.
        listOf("cd0.j2", "Il.w0").forEach { className ->
            runCatching {
                findClass(className).hook("invoke", HookStage.BEFORE) { param ->
                    param.setResult(null)
                }
            }.onFailure {
                com.grindrplus.core.Logger.w(
                    "DisableBoosting: tooltip class '$className' not found — needs class name update for this Grindr version",
                    com.grindrplus.core.LogSource.MODULE
                )
            }
        }
    }
}