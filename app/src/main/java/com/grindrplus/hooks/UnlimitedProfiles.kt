package com.grindrplus.hooks

import com.grindrplus.core.Utils.openProfile
import com.grindrplus.utils.Hook
import com.grindrplus.utils.HookStage
import com.grindrplus.utils.hook
import de.robv.android.xposed.XposedHelpers.getObjectField

class UnlimitedProfiles : Hook(
    "Unlimited profiles",
    "Allow unlimited profiles"
) {
    private val serverDrivenCascadeCachedState =
        "com.grindrapp.android.persistence.model.serverdrivencascade.ServerDrivenCascadeCacheState"
    private val serverDrivenCascadeCachedProfile =
        "com.grindrapp.android.persistence.model.serverdrivencascade.ServerDrivenCascadeCachedProfile"

    override fun init() {
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

        // God forgive me for this abomination. This obviously breaks swiping between profiles
        // so I have to figure out a way to properly handle this issue, since the experiment we
        // were using back then (InaccessibleProfileManager) is no longer available.
        findClass("com.grindrapp.android.ui.browse.B").hook("invokeSuspend", HookStage.BEFORE) { param ->
            val cachedProfile = getObjectField(param.thisObject(), "j")
            val profileId = getObjectField(cachedProfile, "profileIdLong")
            openProfile(profileId.toString())
            param.setResult(null)
        }
    }
}