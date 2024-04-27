package com.grindrplus.hooks

import com.grindrplus.core.Utils.createServiceProxy
import com.grindrplus.utils.Hook
import com.grindrplus.utils.HookStage
import com.grindrplus.utils.hook

class ProfileViews: Hook("Profile views",
    "Don't let others know you viewed their profile") {
    private val retrofit = "retrofit2.Retrofit"
    private val createSuccessResult = "h9.a\$b"
    private val profileRestService = "com.grindrapp.android.api.ProfileRestService"
    private val methodBlacklist = arrayOf(
        "n", // Annotated with @POST("v4/views/{profileId}")
        "h"  // Annotated with @POST("v4/views")
    )

    override fun init() {
        val profileRestServiceClass = findClass(profileRestService)
        val createSuccessResultCtor = findClass(createSuccessResult)?.constructors?.firstOrNull()

        findClass(retrofit)?.hook("create", HookStage.AFTER) { param ->
            val service = param.getResult()
            if (service?.javaClass?.let { profileRestServiceClass?.isAssignableFrom(it) } == true) {
                param.setResult(profileRestServiceClass?.let {
                    createServiceProxy(service, it, methodBlacklist)
                })
            }
        }
    }
}
