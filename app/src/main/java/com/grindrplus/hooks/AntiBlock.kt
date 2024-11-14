package com.grindrplus.hooks

import android.os.Build
import androidx.annotation.RequiresApi
import com.grindrplus.GrindrPlus
import com.grindrplus.utils.Hook
import com.grindrplus.utils.HookStage
import com.grindrplus.utils.hook
import com.grindrplus.utils.hookConstructor
import de.robv.android.xposed.XposedBridge

class AntiBlock : Hook(
    "Anti Block",
    "Alerts you when people block you (prevents the chat from clearing)"
) {
    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    override fun init() {
        findClass("com.grindrapp.android.chat.model.ConversationDeleteNotification")
            .hookConstructor(HookStage.BEFORE) { param ->
                @Suppress("UNCHECKED_CAST")
                val profiles = param.args().firstOrNull() as? List<String> ?: emptyList()
                param.setArg(0, emptyList<String>())
            }
    }
}
