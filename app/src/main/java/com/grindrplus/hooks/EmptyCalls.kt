package com.grindrplus.hooks

import com.grindrplus.GrindrPlus
import com.grindrplus.utils.Hook
import com.grindrplus.utils.HookStage
import com.grindrplus.utils.hook
import com.grindrplus.utils.hookConstructor

class EmptyCalls : Hook(
    "Video calls",
    "Allow video calls on empty chats"
) {
    private val individualChatNavViewModel = "l6.d0" // search for 'com.grindrapp.android.chat.presentation.viewmodel.IndividualChatNavViewModel'

    override fun init() {
        findClass(individualChatNavViewModel) // isTalkBefore()
            .hook("E",  HookStage.BEFORE) { param ->
                param.setResult(true)
            }
    }
}