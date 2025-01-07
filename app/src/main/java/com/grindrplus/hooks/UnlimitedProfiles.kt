package com.grindrplus.hooks

import com.grindrplus.GrindrPlus
import com.grindrplus.core.Config
import com.grindrplus.utils.Hook
import com.grindrplus.utils.HookStage
import com.grindrplus.utils.hook
import com.grindrplus.utils.hookConstructor
import de.robv.android.xposed.XposedHelpers.callMethod
import de.robv.android.xposed.XposedHelpers.getObjectField

class UnlimitedProfiles : Hook(
    "Unlimited profiles",
    "Allow unlimited profiles"
) {
    private val profileRepo = "com.grindrapp.android.persistence.repository.ProfileRepo"
    private val profileModel = "com.grindrapp.android.persistence.model.Profile"
    private val profileItemClickEvent = "com.grindrapp.android.ui.browse.s\$d"
    private val serverDrivenCascadeCachedState =
        "com.grindrapp.android.persistence.model.serverdrivencascade.ServerDrivenCascadeCacheState"
    private val serverDrivenCascadeRepo =
        "com.grindrapp.android.persistence.repository.ServerDrivenCascadeRepo"
    private val serverDrivenCascadeCachedProfile =
        "com.grindrapp.android.persistence.model.serverdrivencascade.ServerDrivenCascadeCachedProfile"
    private val profileTagCascadeFragment = "com.grindrapp.android.ui.tagsearch.ProfileTagCascadeFragment"

    override fun init() {
        findClass(serverDrivenCascadeCachedState)
            .hook("getItems", HookStage.AFTER) { param ->
                val items = (param.getResult() as List<*>).filter {
                    it?.javaClass?.name == serverDrivenCascadeCachedProfile
                }

                param.setResult(items)
            }

        findClass(profileTagCascadeFragment)
            .hook("L", HookStage.BEFORE) { param ->
                param.setResult(true)
            }

        findClass(serverDrivenCascadeCachedProfile)
            .hook("getUpsellType", HookStage.BEFORE) { param ->
                param.setResult(null)
            }

        findClass(serverDrivenCascadeRepo).hook("fetchCascadePage", HookStage.BEFORE) { param ->
            param.setArg(28, Config.get("cascade_endpoint", "v3") as String)
        }

        findClass(profileItemClickEvent).hookConstructor(HookStage.AFTER) { param ->
            val profileId = getObjectField(param.thisObject(), "a") as String
            val cruisableProfileIds = getObjectField(param.thisObject(), "c") as List<*>
            val cacheProfileRange = Config.get("cache_profile_range", 50) as Int
            val ourPosition = cruisableProfileIds.indexOf(profileId)

            val profileRepoInstance = GrindrPlus.instanceManager.getInstance<Any>(profileRepo)!!
            val addProfile = profileRepoInstance.javaClass.declaredMethods.first { it.name == "addProfile" }

            cruisableProfileIds.getProfilesToAdd(ourPosition, cacheProfileRange).forEach { profileId2 ->
                createProfile(profileId2)?.let { profile ->
                    GrindrPlus.coroutineHelper.callSuspendFunction { continuation ->
                        addProfile.invoke(profileRepoInstance, profile, false, continuation)
                    }
                }
            }
        }
    }

    private fun List<*>.getProfilesToAdd(currentPosition: Int, range: Int): List<String> {
        return (currentPosition - range..currentPosition + range)
            .filter { it in indices }
            .map { this[it] as String }
    }

    private fun createProfile(profileId: String): Any? {
        return findClass(profileModel).constructors.first().newInstance().apply {
            callMethod(this, "setProfileId", profileId)
            callMethod(this, "setRemoteUpdatedTime", 1L)
            callMethod(this, "setLocalUpdatedTime", 0L)
        }
    }
}