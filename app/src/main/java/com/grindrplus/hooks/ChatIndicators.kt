package com.grindrplus.hooks

import com.grindrplus.core.Utils.createServiceProxy
import com.grindrplus.utils.Hook
import com.grindrplus.utils.HookStage
import com.grindrplus.utils.hook

class ChatIndicators: Hook("Chat indicators",
    "Don't show chat markers / indicators to others") {
    private val retrofit = "retrofit2.Retrofit"
    private val chatRestService = "com.grindrapp.android.chat.api.ChatRestService"
    private val methodBlacklist = arrayOf(
        "p" // Annotated with @POST("v4/chatstatus/typing")
    )

    override fun init() {
        val chatRestServiceClass = findClass(chatRestService)

        findClass(retrofit)?.hook("create", HookStage.AFTER) { param ->
            val service = param.getResult()
            if (service?.javaClass?.let { chatRestServiceClass?.isAssignableFrom(it) } == true) {
                param.setResult(chatRestServiceClass?.let {
                    createServiceProxy(service, it, methodBlacklist)
                })
            }
        }
    }
}