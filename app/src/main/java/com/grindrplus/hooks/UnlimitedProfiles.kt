package com.grindrplus.hooks

import com.grindrplus.GrindrPlus
import com.grindrplus.core.Config
import com.grindrplus.utils.Hook
import com.grindrplus.utils.HookStage
import com.grindrplus.utils.hook
import de.robv.android.xposed.XposedHelpers.callMethod
import de.robv.android.xposed.XposedHelpers.callStaticMethod
import java.lang.reflect.Proxy

class UnlimitedProfiles : Hook(
    "Unlimited profiles",
    "Allow unlimited profiles"
) {
    private val function2 = "kotlin.jvm.functions.Function2"
    private val profileWithPhoto = "com.grindrapp.android.persistence.pojo.ProfileWithPhoto"
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

        val profileClass = findClass("com.grindrapp.android.persistence.model.Profile")
        val profileWithPhotoConstructor = findClass(profileWithPhoto).getConstructor(profileClass, List::class.java)
        val profileConstructor = profileClass.getConstructor()

        findClass("com.grindrapp.android.persistence.repository.ProfileRepo")
            .hook("getProfilesWithPhotosFlow", HookStage.AFTER) { param ->
                val originalFlow = param.getResult()
                val requestedProfileIds = param.arg<List<String>>(0)

                val proxy = Proxy.newProxyInstance(GrindrPlus.classLoader, arrayOf(findClass(function2))
                ) { _, _, args ->
                    val profilesWithPhoto = args[0] as List<*>
                    val profileIds = profilesWithPhoto.map {
                        val profile = callMethod(it, "getProfile")
                        callMethod(profile, "getProfileId") as String
                    }
                    val missingProfiles = requestedProfileIds - profileIds.toSet()
                    val dummyProfiles = missingProfiles.map { profileId ->
                        val profile = profileConstructor.newInstance()
                        callMethod(profile, "setProfileId", profileId)
                        callMethod(profile, "setRemoteUpdatedTime", 1L)
                        callMethod(profile, "setLocalUpdatedTime", 0L)
                        profileWithPhotoConstructor.newInstance(profile, emptyList<Any>())
                    }
                    profilesWithPhoto + dummyProfiles
                }

                val transformedFlow = callStaticMethod(findClass("kotlinx.coroutines.flow.FlowKt"), "mapLatest", originalFlow, proxy)

                param.setResult(transformedFlow)
            }
    }
}