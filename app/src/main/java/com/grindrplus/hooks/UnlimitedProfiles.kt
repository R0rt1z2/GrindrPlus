package com.grindrplus.hooks

import com.grindrplus.GrindrPlus
import com.grindrplus.core.Config
import com.grindrplus.core.Utils.openProfile
import com.grindrplus.core.Logger
import com.grindrplus.core.loge
import com.grindrplus.utils.Hook
import com.grindrplus.utils.HookStage
import com.grindrplus.utils.hook
import de.robv.android.xposed.XposedHelpers.callMethod
import de.robv.android.xposed.XposedHelpers.callStaticMethod
import de.robv.android.xposed.XposedHelpers.getObjectField
import java.lang.reflect.Proxy

class UnlimitedProfiles : Hook(
    "Unlimited profiles",
    "Allow unlimited profiles"
) {
    private val function2 = "kotlin.jvm.functions.Function2"
    private val onProfileClicked = "com.grindrapp.android.ui.browse.E" // search for 'com.grindrapp.android.ui.browse.ServerDrivenCascadeViewModel$onProfileClicked$1'
    private val profileWithPhoto = "com.grindrapp.android.persistence.pojo.ProfileWithPhoto"
    private val serverDrivenCascadeCachedState =
        "com.grindrapp.android.persistence.model.serverdrivencascade.ServerDrivenCascadeCacheState"
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

        findClass(profileTagCascadeFragment) // search for 'new StringBuilder("cascadeClickEvent/position=");'
            .hook("R", HookStage.BEFORE) { param ->
                param.setResult(true)
            }

        findClass(serverDrivenCascadeCachedProfile)
            .hook("getUpsellType", HookStage.BEFORE) { param ->
                param.setResult(null)
            }

        val profileClass = findClass("com.grindrapp.android.persistence.model.Profile")
        val profileWithPhotoClass = findClass(profileWithPhoto)
        val function2Class = findClass(function2)
        val flowKtClass = findClass("kotlinx.coroutines.flow.FlowKt")
        val profileRepoClass = findClass("com.grindrapp.android.persistence.repository.ProfileRepo")

        profileRepoClass.hook("getProfilesWithPhotosFlow", HookStage.AFTER) { param ->
            val requestedProfileIds = param.arg<List<String>>(0)
            if (requestedProfileIds.isEmpty()) return@hook

            val originalFlow = param.getResult()
            val profileWithPhotoConstructor = profileWithPhotoClass
                .getConstructor(profileClass, List::class.java)
            val profileConstructor = profileClass.getConstructor()

            val proxy = Proxy.newProxyInstance(
                GrindrPlus.classLoader,
                arrayOf(function2Class)
            ) { _, _, args ->
                @Suppress("UNCHECKED_CAST")
                val profilesWithPhoto = args[0] as List<Any>

                if (requestedProfileIds.size > profilesWithPhoto.size) {
                    val profileIds = ArrayList<String>(profilesWithPhoto.size)

                    for (profileWithPhoto in profilesWithPhoto) {
                        val profile = callMethod(profileWithPhoto, "getProfile")
                        profileIds.add(callMethod(profile, "getProfileId") as String)
                    }

                    val profileIdSet = profileIds.toHashSet()

                    val missingProfiles = ArrayList<Any>()
                    for (profileId in requestedProfileIds) {
                        if (profileId !in profileIdSet) {
                            val profile = profileConstructor.newInstance()
                            callMethod(profile, "setProfileId", profileId)
                            callMethod(profile, "setRemoteUpdatedTime", 1L)
                            callMethod(profile, "setLocalUpdatedTime", 0L)
                            missingProfiles.add(
                                profileWithPhotoConstructor.newInstance(profile, emptyList<Any>())
                            )
                        }
                    }

                    if (missingProfiles.isNotEmpty()) {
                        val result = ArrayList<Any>(profilesWithPhoto.size + missingProfiles.size)
                        result.addAll(profilesWithPhoto)
                        result.addAll(missingProfiles)
                        return@newProxyInstance result
                    }
                }

                profilesWithPhoto
            }

            val transformedFlow = callStaticMethod(flowKtClass, "mapLatest", originalFlow, proxy)
            param.setResult(transformedFlow)
        }

        findClass(onProfileClicked).hook("invokeSuspend", HookStage.BEFORE) { param ->
            if (Config.get("disable_profile_swipe", false) as Boolean) {
                getObjectField(param.thisObject(), param.thisObject().javaClass.declaredFields
                    .firstOrNull { it.type.name.contains("ServerDrivenCascadeCachedProfile") }?.name
                )?.let { cachedProfile ->
                    runCatching { getObjectField(cachedProfile, "profileIdLong").toString() }
                        .onSuccess { profileId ->
                            openProfile(profileId)
                            param.setResult(null)
                        }
                        .onFailure { loge("Profile ID not found in cached profile") }
                }
            }
        }
    }
}