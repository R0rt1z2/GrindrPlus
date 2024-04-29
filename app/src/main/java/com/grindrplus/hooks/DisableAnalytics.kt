package com.grindrplus.hooks

import com.grindrplus.core.Utils.createServiceProxy
import com.grindrplus.utils.Hook
import com.grindrplus.utils.HookStage
import com.grindrplus.utils.hook

class DisableAnalytics : Hook(
    "Disable analytics",
    "Disable Grindr analytics (data collection)"
) {
    private val analyticsRestService = "v5.b"

    override fun init() {
        val analyticsRestServiceClass = findClass(analyticsRestService) ?: return

        findClass("retrofit2.Retrofit")
            ?.hook("create", HookStage.AFTER) { param ->
                val service = param.result
                if (service != null && analyticsRestServiceClass.isAssignableFrom(service.javaClass)) {
                    param.result = createServiceProxy(service, analyticsRestServiceClass)
                }
            }
    }
}