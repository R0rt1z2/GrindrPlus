package com.grindrplus.hooks

import com.grindrplus.utils.Hook
import com.grindrplus.utils.HookStage
import com.grindrplus.utils.hookConstructor
import de.robv.android.xposed.XposedHelpers.setObjectField

class DisableBoosting : Hook(
    "Disable boosting",
    "Get rid of all upsells related to boosting"
) {
    private val drawerProfileUiState = "e8.h\$a"
    private val radarUiModel = "i6.a\$a"
    private val boostFabUiModel = "com.grindrapp.android.boost2.presentation.model.BoostFabUiModel"

    override fun init() {
        findClass(drawerProfileUiState).hookConstructor(HookStage.AFTER) { param ->
            setObjectField(param.thisObject(), "a", false) // showBoostMeButton
            setObjectField(param.thisObject(), "b", false) // isBoostAvailable
            setObjectField(param.thisObject(), "d", null) // showDayPassItem
            setObjectField(param.thisObject(), "e", null) // dayPassXtraItem
            setObjectField(param.thisObject(), "f", null) // unlimitedWeeklySubscriptionItem
            setObjectField(param.thisObject(), "f", null) // unlimitedWeeklySubscriptionItem
        }

        findClass(radarUiModel).hookConstructor(HookStage.AFTER) { param ->
            setObjectField(param.thisObject(), "a", false) // isBoostButtonVisible
            setObjectField(param.thisObject(), "b", false) // isBoostReportButtonVisible
            setObjectField(param.thisObject(), "c", false) // isBoostingTextVisible
            setObjectField(param.thisObject(), "d", false) // isBoostIconVisible
        }

        // Grindr decided to name this upsell with the weirdest name possible, I pay respects below.
        findClass(boostFabUiModel)
            .hookConstructor(HookStage.AFTER) { param ->
                setObjectField(param.thisObject(), "isVisible", false)
            }
    }
}