package com.grindrplus.hooks

import com.grindrplus.utils.Hook
import com.grindrplus.utils.HookStage
import com.grindrplus.utils.hook

class EmptyCalls : Hook(
    "Video calls",
    "Allow video calls on empty chats"
) {
    private val individualChatNavViewModel = "to.p1" // search for 'com.grindrapp.android.chat.presentation.viewmodel.IndividualChatNavViewModel', then go to the class mentioned in the invokeSuspend method.

    override fun init() {
        findClass(individualChatNavViewModel) // isTalkBefore()
            .hook("P",  HookStage.BEFORE) { param ->
                param.setResult(true)
            }
    }
}