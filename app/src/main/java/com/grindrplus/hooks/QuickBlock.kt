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
    private val blockViewModel = "yg.b" // search for '("STATUS_BLOCK_DIALOG_SHOWN", 1)'
    private val profileViewHolder = "com.grindrapp.android.ui.profileV2.g" // search for 'com.grindrapp.android.ui.profileV2.ProfileViewHolder$onBind$3'

    override fun init() {
        findClass(profileViewHolder).hook("y", HookStage.AFTER) { param ->
            val arg0 = param.arg(0) as Any
            val profileId = param.args().getOrNull(1) ?: return@hook
            val viewBinding = getObjectField(arg0, "b")
            val profileToolbar = getObjectField(viewBinding, "p")
            val toolbarMenu = callMethod(profileToolbar, "getMenu") as Menu
            val menuActions = getId("menu_actions", "id", GrindrPlus.context)
            val actionsMenuItem = callMethod(toolbarMenu, "findItem", menuActions) as MenuItem
            actionsMenuItem.setOnMenuItemClickListener { GrindrPlus.httpClient.blockUser(profileId as String); true }
        }

        findClass(blockViewModel).hook("I", HookStage.BEFORE) { param ->
            val profileId = param.thisObject().javaClass.declaredFields
                .asSequence()
                .filter { it.type == String::class.java }
                .mapNotNull { field ->
                    try {
                        field.isAccessible = true
                        field.get(param.thisObject()) as? String
                    } catch (e: Exception) { null }
                }
                .firstOrNull { it.isNotEmpty() && it.all { char -> char.isDigit() } }
            GrindrPlus.httpClient.blockUser(profileId as String)
            param.setResult(null)
        }
    }
}