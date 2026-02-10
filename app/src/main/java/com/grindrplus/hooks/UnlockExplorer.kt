package com.grindrplus.hooks

import com.grindrplus.utils.Hook
import com.grindrplus.utils.HookStage
import com.grindrplus.utils.hook

class UnlockExplorer : Hook(
    "Unlock Explorer",
    "Unlock all profiles in Explorer"
) {
    override fun init() {
        findClass("com.grindrapp.android.ui.profileV2.model.ProfileViewState")
            .hook("getShouldLockQuickbar", HookStage.BEFORE) { param ->
                param.setResult(false)
            }
    }
    // Alternative methods
    // getNumOfFreeExploreChatsRemaining
    // numOfFreeExploreChatsRemaining
}