package com.grindrplus.hooks

import com.grindrplus.utils.Hook
import com.grindrplus.utils.RetrofitUtils.blockServiceMethods
import com.grindrplus.utils.RetrofitUtils.findPOSTMethod

class ProfileViews : Hook(
	"Profile views",
	"Don't let others know you viewed their profile"
) {
    private val profileRestService = "com.grindrapp.android.api.ProfileRestService"
    private val blacklistedPaths = setOf(
        "v5/views/{profileId}"
    )

    override fun init() {
        val profileRestServiceClass = findClass(profileRestService)

        val methodBlacklist =
            blacklistedPaths.mapNotNull { findPOSTMethod(profileRestServiceClass, it)?.name }

        blockServiceMethods(profileRestServiceClass, methodBlacklist.toSet())
    }
}
