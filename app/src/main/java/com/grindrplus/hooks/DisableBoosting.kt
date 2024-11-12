package com.grindrplus.hooks

import com.grindrplus.utils.Hook
import com.grindrplus.utils.HookStage
import com.grindrplus.utils.hook
import com.grindrplus.utils.hookConstructor
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers.findAndHookMethod
import de.robv.android.xposed.XposedHelpers.setObjectField

class DisableBoosting : Hook(
    "Disable boosting",
    "Get rid of all upsells related to boosting"
) {
    private val drawerProfileUiState = "V9.e\$a"
    private val radarUiModel = "J7.a\$a"

    // BoostFabUiModel was refactored into BoostOverviewUIModel; however, this val is now unused
    // see: this class's TODO
    private val boostFabUiModel = "com.grindrapp.android.boost2.presentation.model.BoostOverviewUIModel"

    private val boostStateClass =
        "com.grindrapp.android.ui.drawer.model.SideDrawerMicrosButtonState.Unavailable"

    // SideDrawerRightNowBoostState.UNAVAILABLE has been shifted from a field to a class, and
    // the whole structure has been rearranged under SideDrawerMicrosButtonState
    // i think the new target field would be SideDrawerMicrosButtonState.Unavailable.INSTANCE

    override fun init() {
        findClass(drawerProfileUiState).hookConstructor(HookStage.AFTER) { param ->
            setObjectField(param.thisObject(), "a", false) // showBoostMeButton
            setObjectField(
                param.thisObject(),
                "e",
                findClass(boostStateClass).getField("INSTANCE").get(null)
            ) // boostButtonState
            setObjectField(param.thisObject(), "i", null) // showDayPassItem
            // setObjectField(param.thisObject(), "i", null) // dayPassXtraItem --> no longer exists
            setObjectField(param.thisObject(), "j", null) // unlimitedWeeklySubscriptionItem
            setObjectField(param.thisObject(), "s", false) // isRightNowTooltipVisible
            setObjectField(param.thisObject(), "r", false) // isRightNowAvailable
        }


        // see: this class's TODO

        /*
        findClass(radarUiModel).hookConstructor(HookStage.AFTER) { param ->
            setObjectField(param.thisObject(), "a", false) // isBoostButtonVisible
            setObjectField(param.thisObject(), "b", false) // isBoostReportButtonVisible
            setObjectField(param.thisObject(), "c", false) // isBoostingTextVisible
            setObjectField(param.thisObject(), "d", false) // isBoostIconVisible
        }
         */

        // the two anonymous functions that get called to invoke the annoying tooltip
        // respectively: showRadarTooltip.<anonymous> and showTapsAndViewedMePopup
        listOf("fa.j0", "fa.m0").forEach {
            findClass(it).hook("invoke", HookStage.BEFORE) { param ->
                param.setResult(null)
            }
        }

        // Grindr decided to name this upsell with the weirdest name possible, I pay respects below.

        // TODO: reevaluate this hook, along with everything else pertaining to the Boost buttons
        //  these params no longer exist, code blocks need revisiting
        //  my best guess as to how to handle this is passing a null BoostSession
        //  can't be sure, however, and don't intend to break the module where it needn't be broken
        //  -mlr


        /*
        findClass(boostFabUiModel)
            .hookConstructor(HookStage.AFTER) { param ->
                setObjectField(param.thisObject(), "isVisible", false)
            }
         */



    }
}