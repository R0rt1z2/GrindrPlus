package com.grindrplus.hooks

import com.grindrplus.GrindrPlus
import com.grindrplus.core.Utils.createServiceProxy
import com.grindrplus.utils.Hook
import com.grindrplus.utils.HookStage
import com.grindrplus.utils.RetrofitUtils
import com.grindrplus.utils.hook

class ChatIndicators : Hook(
    "Chat indicators",
    "Don't show chat markers / indicators to others"
) {
    private val chatRestService = "com.grindrapp.android.chat.data.datasource.api.service.ChatRestService"
    private val blacklistedPaths = setOf(
        "v4/chatstatus/typing"
    )

    override fun init() {
        val chatRestServiceClass = findClass(chatRestService)

        val methodBlacklist = blacklistedPaths.mapNotNull {
            RetrofitUtils.findPOSTMethod(chatRestServiceClass, it)?.name
        }

        findClass("retrofit2.Retrofit")
            .hook("create", HookStage.AFTER) { param ->
                val service = param.getResult()
                if (service != null && chatRestServiceClass.isAssignableFrom(service.javaClass)) {
                    param.setResult(createServiceProxy(
                        service,
                        chatRestServiceClass,
                        methodBlacklist.toTypedArray()
                    ))
                }
            }
    }
}