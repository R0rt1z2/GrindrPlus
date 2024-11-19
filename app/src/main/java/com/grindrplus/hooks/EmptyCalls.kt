package com.grindrplus.hooks

import com.grindrplus.utils.Hook
import com.grindrplus.utils.HookStage
import com.grindrplus.utils.hook

class EmptyCalls : Hook(
    "Video calls",
    "Allow video calls on empty chats"
) {
    private val individualChatNavViewModel = "J9.a"

    override fun init() {
        findClass(individualChatNavViewModel) // isTalkBefore()
            .hook("z",  HookStage.BEFORE) { param ->
            param.setResult(true)
        }
    }
}