package com.grindrplus.hooks

import com.grindrplus.utils.Hook
import com.grindrplus.utils.HookStage
import com.grindrplus.utils.hook

// supported version: 25.20.0
class UnlockExplorer : Hook(
    "Unlock Explorer",
    "Unlock all profiles in Explorer"
) {
    override fun init() {
        // *always* give you 5 *remaining* profiles to unlock in Explorer mode
        // 5 is a nice low number that looks good in the UI

        // HOW TO MAINTAIN: Hook both methods below, both exists in the smali and should probably be
        // hooked, JADX JAVA simplifies and removes componentXX, look in JADX JAVA comment or search
        // "numOfFreeExploreChatsRemaining" in smali to find all instances

        findClass("com.grindrapp.android.ui.profileV2.model.ProfileViewState")
            .hook("getNumOfFreeExploreChatsRemaining", HookStage.BEFORE) { param ->
                param.setResult(5)
            }

        findClass("com.grindrapp.android.ui.profileV2.model.ProfileViewState")
            .hook("component58", HookStage.BEFORE) { param ->
                param.setResult(5)
            }
    }
}