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
    private val drawerProfileUiState = "Ba.e\$a"
    private val radarUiModel = "n8.a\$a"
    private val fabUiModel = "com.grindrapp.android.boost2.presentation.model.FabUIModel"
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

        // the two anonymous functions that get called to invoke the annoying tooltip
        // respectively: showRadarTooltip.<anonymous> and showTapsAndViewedMePopup
        listOf("La.n0", "La.p0", "La.q0", "La.o0").forEach {
            findClass(it).hook("invoke", HookStage.BEFORE) { param ->
                param.setResult(null)
            }
        }
    }
}