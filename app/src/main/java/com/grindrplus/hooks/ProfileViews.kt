package com.grindrplus.hooks

import com.grindrplus.GrindrPlus
import com.grindrplus.core.Utils.createServiceProxy
import com.grindrplus.utils.Hook
import com.grindrplus.utils.HookStage
import com.grindrplus.utils.RetrofitUtils.findPOSTMethod
import com.grindrplus.utils.hook

class ProfileViews : Hook(
    "Profile views",
    "Don't let others know you viewed their profile"
) {
    private val profileRestService = "com.grindrapp.android.api.ProfileRestService"
    private val blacklistedPaths = setOf(
        "v4/views/{profileId}",
        "v4/views"
    )

    override fun init() {
        val profileRestServiceClass = findClass(profileRestService)

        val methodBlacklist = blacklistedPaths.mapNotNull {
            findPOSTMethod(profileRestServiceClass, it)?.name
        }

        if (methodBlacklist.size != blacklistedPaths.size) {
            GrindrPlus.logger.log("ProfileViews: not all blacklisted paths were found! Open an issue on GitHub.")
        }

        findClass("retrofit2.Retrofit")
            .hook("create", HookStage.AFTER) { param ->
                val service = param.getResult()
                if (service != null && profileRestServiceClass.isAssignableFrom(service.javaClass)) {
                    param.setResult(createServiceProxy(
                        service,
                        profileRestServiceClass,
                        methodBlacklist.toTypedArray()
                    ))
                }
            }
    }
}
