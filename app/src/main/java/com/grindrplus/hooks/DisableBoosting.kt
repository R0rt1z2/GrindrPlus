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
    private val drawerProfileUiState = "V9.e\$a"
    private val radarUiModel = "J7.a\$a"
    private val roamOnBoardingFragment = "Aa.c"
    private val fabUiModel = "com.grindrapp.android.boost2.presentation.model.FabUIModel"
    private val boostStateClass =
        "com.grindrapp.android.ui.drawer.model.SideDrawerMicrosButtonState\$Unavailable"

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
            setObjectField(param.thisObject(), "i", null) // showDayPassItem
            setObjectField(param.thisObject(), "j", null) // unlimitedWeeklySubscriptionItem
            setObjectField(param.thisObject(), "s", false) // isRightNowTooltipVisible
            setObjectField(param.thisObject(), "r", false) // isRightNowAvailable
        }

        findClass(radarUiModel).hookConstructor(HookStage.AFTER) { param ->
            setObjectField(param.thisObject(), "a", false) // showReportButton
            setObjectField(param.thisObject(), "b", null) // boostButton
            setObjectField(param.thisObject(), "c", null) // roamButton
        }

        // the two anonymous functions that get called to invoke the annoying tooltip
        // respectively: showRadarTooltip.<anonymous> and showTapsAndViewedMePopup
        listOf("fa.j0", "fa.m0").forEach {
            findClass(it).hook("invoke", HookStage.BEFORE) { param ->
                param.setResult(null)
            }
        }

        findClass(fabUiModel).hook("createFragment", HookStage.BEFORE) { param ->
            param.setResult(null) // Don't let the fragment be created
        }

        findClass(roamOnBoardingFragment).hook("a", HookStage.BEFORE) { param -> // showBoostMeButton
            param.setResult(false)
        }
    }
}