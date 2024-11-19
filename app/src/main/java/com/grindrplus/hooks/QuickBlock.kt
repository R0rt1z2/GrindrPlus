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
    private val blockViewModel = "H9.f"
    private val profileViewHolder = "com.grindrapp.android.ui.profileV2.j"
    override fun init() {
        findClass(profileViewHolder).hook("F", HookStage.AFTER) { param ->
            val jVar = param.arg(0) as Any
            val profileViewState = param.args().getOrNull(1) ?: return@hook
            val profileId = getObjectField(profileViewState, "profileId") as String
            val viewBinding = getObjectField(jVar, "p")
            val profileToolbar = getObjectField(viewBinding, "r")
            val toolbarMenu = callMethod(profileToolbar, "getMenu") as Menu
            val menuActions = getId("menu_actions", "id", GrindrPlus.context)
            val actionsMenuItem = callMethod(toolbarMenu, "findItem", menuActions) as MenuItem
            actionsMenuItem.setOnMenuItemClickListener { GrindrPlus.httpClient.blockUser(profileId); true }
        }

        findClass(blockViewModel).hook("B", HookStage.BEFORE) { param ->
            GrindrPlus.httpClient.blockUser(getObjectField(param.thisObject(), "y") as String)
            param.setResult(null)
        }
    }
}