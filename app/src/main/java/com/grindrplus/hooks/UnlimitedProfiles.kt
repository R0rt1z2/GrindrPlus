package com.grindrplus.hooks

import com.grindrplus.utils.Hook
import com.grindrplus.utils.HookStage
import com.grindrplus.utils.hook

class UnlimitedProfiles : Hook(
    "Unlimited profiles",
    "Allow unlimited profiles"
) {
    private val serverDrivenCascadeCachedState =
        "com.grindrapp.android.persistence.model.serverdrivencascade.ServerDrivenCascadeCacheState"
    private val serverDrivenCascadeCachedProfile =
        "com.grindrapp.android.persistence.model.serverdrivencascade.ServerDrivenCascadeCachedProfile"
    private val inAccessibleProfileManager =
        "com.grindrapp.android.profile.experiments.InaccessibleProfileManager"

    override fun init() {
        findClass(inAccessibleProfileManager)
            .hook("a", HookStage.BEFORE) { param ->
                param.setResult(true)
            }

        findClass(serverDrivenCascadeCachedState)
            .hook("getItems", HookStage.AFTER) { param ->
                val items = (param.getResult() as List<*>).filter {
                    it?.javaClass?.name == serverDrivenCascadeCachedProfile
                }

                param.setResult(items)
            }

        findClass(serverDrivenCascadeCachedProfile)
            .hook("getUpsellType", HookStage.BEFORE) { param ->
                param.setResult(null)
            }
    }
}