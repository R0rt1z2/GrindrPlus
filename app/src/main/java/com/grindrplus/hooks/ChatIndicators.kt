package com.grindrplus.hooks

import com.grindrplus.utils.Hook
import com.grindrplus.utils.RetrofitUtils
import com.grindrplus.utils.RetrofitUtils.blockServiceMethods

class ChatIndicators : Hook(
    "Chat indicators",
    "Don't show chat markers / indicators to others"
) {
    private val chatRestService = "vn.a" // search for '"(Lcom/grindrapp/android/chat/data/datasource/api/model/MessageRateResponseRequest;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;"'
    private val blacklistedPaths = setOf(
        "v4/chatstatus/typing"
    )

    override fun init() {
        val chatRestServiceClass = findClass(chatRestService)

        val methodBlacklist = blacklistedPaths.mapNotNull {
            RetrofitUtils.findPOSTMethod(chatRestServiceClass, it)?.name
        }

        blockServiceMethods(chatRestServiceClass, methodBlacklist.toSet())
    }
}