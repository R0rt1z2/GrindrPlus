package com.grindrplus.hooks

import com.grindrplus.core.loge
import com.grindrplus.utils.Hook
import com.grindrplus.utils.HookStage
import com.grindrplus.utils.hook

class UnlockExplorer : Hook(
    "Unlock Explorer",
    "Unlock all profiles in Explorer"
) {
    override fun init() {
        try {
            findClass("com.grindrapp.android.ui.profileV2.model.ProfileViewState")
                .hook("getShouldLockQuickbar", HookStage.BEFORE) { param ->
                    param.setResult(false)
                }
        } catch (e: Throwable) {
            loge("UnlockExplorer: failed to hook ProfileViewState: ${e.message}")
        }
    }
    // Alternative methods
    // getNumOfFreeExploreChatsRemaining
    // numOfFreeExploreChatsRemaining
}