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
    private val individualChatNavViewModel = "P5.f0" // search for 'com.grindrapp.android.chat.presentation.viewmodel.IndividualChatNavViewModel'
    private val createVideoCallResponse = "com.grindrapp.android.chat.api.model.CreateVideoCallResponse"
    private val videoCallInfoResponse = "com.grindrapp.android.chat.api.model.VideoCallInfoResponse"

    override fun init() {
        findClass(individualChatNavViewModel) // isTalkBefore()
            .hook("A",  HookStage.BEFORE) { param ->
                param.setResult(true)
            }

        findClass(createVideoCallResponse)
            .hook("getRemainingSeconds", HookStage.BEFORE) { param ->
                param.setResult(Long.MAX_VALUE)
            }

        findClass(createVideoCallResponse)
            .hookConstructor(HookStage.BEFORE) { param ->
                param.setArg(1, Long.MAX_VALUE) // maxSeconds
                param.setArg(3, Long.MAX_VALUE) // remainingSeconds
            }

        findClass(createVideoCallResponse)
            .hook("fetchRemainingSeconds", HookStage.BEFORE) { param ->
                param.setResult(Long.MAX_VALUE)
            }

        findClass(videoCallInfoResponse)
            .hook("getRemainingSeconds", HookStage.BEFORE) { param ->
                param.setResult(Int.MAX_VALUE)
            }
    }
}