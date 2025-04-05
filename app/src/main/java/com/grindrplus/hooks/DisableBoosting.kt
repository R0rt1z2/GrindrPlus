package com.grindrplus.hooks

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
    private val drawerProfileUiState = "ed.e\$a" // search for 'DrawerProfileUiState(showBoostMeButton='
    private val radarUiModel = "oa.a\$a" // search for 'RadarUiModel(boostButton='
    private val fabUiModel = "com.grindrapp.android.boost2.presentation.model.FabUiModel"
    private val rightNowMicrosFabUiModel =
        "com.grindrapp.android.rightnow.presentation.model.RightNowMicrosFabUiModel"

    private val boostStateClass =
        "com.grindrapp.android.ui.drawer.model.MicrosDrawerItemState\$Unavailable"

    override fun init() {
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
            setObjectField(
                param.thisObject(),
                "g",
                newInstance(findClass(boostStateClass))
            ) // storeInDrawerProfileState
            setObjectField(param.thisObject(), "c", false) // showRNBoostCard
            setObjectField(param.thisObject(), "j", null) // showDayPassItem
            setObjectField(param.thisObject(), "k", null) // unlimitedWeeklySubscriptionItem
            setObjectField(param.thisObject(), "s", false) // isRightNowAvailable
        }

        findClass(radarUiModel).hookConstructor(HookStage.AFTER) { param ->
            setObjectField(param.thisObject(), "a", null) // boostButton
            setObjectField(param.thisObject(), "b", null) // roamButton
        }

        findClass(fabUiModel).hookConstructor(HookStage.AFTER) { param ->
            setObjectField(param.thisObject(), "isVisible", false) // isVisible
        }

        findClass(rightNowMicrosFabUiModel).hookConstructor(HookStage.AFTER) { param ->
            setObjectField(param.thisObject(), "isBoostFabVisible", false) // isBoostFabVisible
            setObjectField(param.thisObject(), "isClickEnabled", false) // isClickEnabled
            setObjectField(param.thisObject(), "isFabVisible", false) // isFabVisible
        }

        // the two anonymous functions that get called to invoke the annoying tooltip
        // respectively: showRadarTooltip.<anonymous> and showTapsAndViewedMePopup
        // search for:
        //   'com.grindrapp.android.ui.home.HomeActivity$showTapsAndViewedMePopup$1$1'
        //   'com.grindrapp.android.ui.home.HomeActivity.showTapsAndViewedMePopup.<anonymous> (HomeActivity.kt'
        //   'com.grindrapp.android.ui.home.HomeActivity$subscribeForBoostRedeem$1'
        //   'com.grindrapp.android.ui.home.HomeActivity.showTapsAndViewedMePopup.<anonymous>.<anonymous> (HomeActivity.kt'
        listOf("od.e0", "od.g0", "od.i0", "od.f0").forEach {
            findClass(it).hook("invoke", HookStage.BEFORE) { param ->
                param.setResult(null)
            }
        }
    }
}