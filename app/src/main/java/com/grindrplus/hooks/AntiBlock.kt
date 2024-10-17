package com.grindrplus.hooks

import android.os.Build
import androidx.annotation.RequiresApi
import com.grindrplus.utils.Hook
import com.grindrplus.utils.HookStage
import com.grindrplus.utils.hook
import com.grindrplus.utils.hookConstructor
import de.robv.android.xposed.XposedBridge

class AntiBlock : Hook(
    "Anti Block",
    "Alerts you when people block you (prevents the chat from clearing)" // review my english pls
) {
    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    override fun init() {
        findClass("com.grindrapp.android.chat.model.ConversationDeleteNotification")
            .hookConstructor(HookStage.BEFORE) { param ->
                @Suppress("UNCHECKED_CAST")
                val assholes = param.args().first() as List<String>

                XposedBridge.log("${assholes.joinToString()} are trying to block us, yeeting...")
                param.setArg(0, emptyList<String>())

                // TODO: Do something after clearing this to warn the user
            }

        // looks like it's for old blocks???? strangely returns empty??? wtf??????? BlockManager is useless
        // com.grindrapp.android.manager.BlockInteractor for old blocks, also this almost never
        // gets called so if its annoying to update we can yeet it
        findClass("v5.F\$d").hook("invokeSuspend", HookStage.BEFORE) {
            it.setResult(null)
        }
    }
}