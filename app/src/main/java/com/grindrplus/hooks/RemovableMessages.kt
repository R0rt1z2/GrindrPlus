package com.grindrplus.hooks

import com.grindrplus.utils.Hook
import com.grindrplus.utils.HookStage
import com.grindrplus.utils.hook

class RemovableMessages : Hook(
    "Removable messages",
    "Allow to remove any message, no matter how old it is"
) {
    private val chatMessageContent = "com.grindrapp.android.persistence.model.ChatMessageContent"
    private val showMessageLongClickDialog = "pd.e3"

    override fun init() {
        // TODO: I think this is not needed/working, check what to do with it
        findClass(chatMessageContent)
            ?.hook("getTimestamp", HookStage.AFTER) { param ->
                if (Thread.currentThread().stackTrace.any {
                        it.className.contains(showMessageLongClickDialog)
                    }) {
                    param.result = System.currentTimeMillis()
                }
            }
    }
}