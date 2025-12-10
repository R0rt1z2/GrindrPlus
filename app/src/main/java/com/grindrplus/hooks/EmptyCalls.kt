package com.grindrplus.hooks

import com.grindrplus.utils.Hook
import com.grindrplus.utils.HookStage
import com.grindrplus.utils.hook

class EmptyCalls : Hook(
    "Video calls",
    "Allow video calls on empty chats"
) {
    private val individualChatNavViewModel = "W6.e0" // search for 'com.grindrapp.android.chat.presentation.viewmodel.IndividualChatNavViewModel'

    override fun init() {
        findClass(individualChatNavViewModel) // isTalkBefore()
            .hook("G",  HookStage.BEFORE) { param ->
                param.setResult(true)
            }
    }
}