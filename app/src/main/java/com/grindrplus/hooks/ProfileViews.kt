package com.grindrplus.hooks

import com.grindrplus.core.loge
import com.grindrplus.utils.Hook
import com.grindrplus.utils.HookStage
import com.grindrplus.utils.RetrofitUtils.RETROFIT_NAME
import com.grindrplus.utils.RetrofitUtils.createServiceProxy
import com.grindrplus.utils.RetrofitUtils.findPOSTMethod
import com.grindrplus.utils.hook

class ProfileViews : Hook(
	"Profile views",
	"Don't let others know you viewed their profile"
) {
    private val profileRestService = "com.grindrapp.android.api.ProfileRestService"
    private val blacklistedPaths = setOf(
        "v5/views/{profileId}"
    )

    override fun init() {
        val profileRestServiceClass = try {
            findClass(profileRestService)
        } catch (e: Throwable) {
            loge("ProfileViews: failed to hook $profileRestService: ${e.message}")
            return
        }

        val methodBlacklist =
            blacklistedPaths.mapNotNull { findPOSTMethod(profileRestServiceClass, it)?.name }

        findClass(RETROFIT_NAME).hook("create", HookStage.AFTER) { param ->
            val service = param.getResult()
            if (service != null && profileRestServiceClass.isAssignableFrom(service.javaClass)) {
                param.setResult(
                    createServiceProxy(
                        service,
                        profileRestServiceClass,
                        methodBlacklist.toTypedArray()
                    )
                )
            }
        }
    }
}
