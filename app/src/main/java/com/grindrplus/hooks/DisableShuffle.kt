package com.grindrplus.hooks

import com.grindrplus.utils.Hook
import com.grindrplus.utils.HookStage
import com.grindrplus.utils.hookConstructor
import de.robv.android.xposed.XposedHelpers.setObjectField


class DisableShuffle : Hook(
    "Disable shuffle",
    "Forcefully disable the shuffle feature"
) {
    private val shuffleUiState = "com.grindrapp.android.ui.browse.s\$g"

    override fun init() {
        findClass(shuffleUiState).hookConstructor(HookStage.AFTER) { param ->
            setObjectField(param.thisObject(), "a", false) // shuffleEnabled
            setObjectField(param.thisObject(), "b", false) // isShuffled
            setObjectField(param.thisObject(), "c", false) // isShuffling
            setObjectField(param.thisObject(), "d", false) // showShuffleTooltip
            setObjectField(param.thisObject(), "f", false) // isShuffleTopBarVisible
            setObjectField(param.thisObject(), "g", false) // showShuffleUpsell
        }
    }
}