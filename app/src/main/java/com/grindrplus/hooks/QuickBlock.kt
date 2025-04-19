package com.grindrplus.hooks

import android.view.Menu
import android.view.MenuItem
import com.grindrplus.GrindrPlus
import com.grindrplus.ui.Utils.getId
import com.grindrplus.utils.Hook
import com.grindrplus.utils.HookStage
import com.grindrplus.utils.hook
import de.robv.android.xposed.XposedHelpers.callMethod
import de.robv.android.xposed.XposedHelpers.getObjectField

class QuickBlock : Hook(
    "Quick block",
    "Ability to block users quickly"
) {
    private val blockViewModel = "nd.b" // search for '("STATUS_BLOCK_DIALOG_SHOWN", 1)'
    private val profileViewHolder = "com.grindrapp.android.ui.profileV2.d" // search for 'com.grindrapp.android.ui.profileV2.ProfileViewHolder$onBind$3'
    private val profileModel = "com.grindrapp.android.persistence.model.Profile"

    override fun init() {
        findClass(profileViewHolder).hook("A", HookStage.AFTER) { param ->
            val arg0 = param.arg(0) as Any
            val profileId = param.args().getOrNull(1) ?: return@hook
            val viewBinding = getObjectField(arg0, "p")
            val profileToolbar = getObjectField(viewBinding, "q")
            val toolbarMenu = callMethod(profileToolbar, "getMenu") as Menu
            val menuActions = getId("menu_actions", "id", GrindrPlus.context)
            val actionsMenuItem = callMethod(toolbarMenu, "findItem", menuActions) as MenuItem
            actionsMenuItem.setOnMenuItemClickListener { GrindrPlus.httpClient.blockUser(profileId as String); true }
        }

        findClass(blockViewModel).hook("s", HookStage.BEFORE) { param ->
            GrindrPlus.httpClient.blockUser(getObjectField(param.thisObject(), "y") as String)
            param.setResult(null)
        }

        setOf("isBlockable", "component60").forEach {
            findClass(profileModel).hook(it, HookStage.BEFORE) { param ->
                param.setResult(true)
            }
        }
    }
}