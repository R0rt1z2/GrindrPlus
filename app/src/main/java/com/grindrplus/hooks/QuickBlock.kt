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
    private val blockViewModel = "ml.a" // ViewModel posting STATUS_BLOCK_DIALOG_SHOWN
    private val profileScreen = "com.grindrapp.android.ui.profileV2.g" // Profile screen binder (inflates menu_profile)

    override fun init() {
        // Short-circuit block dialog: when block dialog is about to show, just block directly and skip UI.
        findClass(blockViewModel).hook("O", HookStage.BEFORE) { param ->
            val profileId = param.argNullable<String>(0)
            if (!profileId.isNullOrEmpty()) {
                GrindrPlus.httpClient.blockUser(profileId)
            }
            param.setResult(null) // skip showing dialog
        }

        // Attach block action to profile toolbar menu action.
        findClass(profileScreen).hook("A", HookStage.AFTER) { param ->
            val profileId = param.argNullable<String>(1) ?: return@hook
            val bindingArg = param.argNullable<Any>(0) ?: return@hook
            val viewBinding = getObjectField(bindingArg, "p") // LHb/q5; field p is ProfileToolbar
            val toolbarMenu = callMethod(viewBinding, "getMenu") as? Menu ?: return@hook
            val menuActions = getId("menu_actions", "id", GrindrPlus.context)
            val actionsMenuItem = callMethod(toolbarMenu, "findItem", menuActions) as? MenuItem ?: return@hook
            actionsMenuItem.setOnMenuItemClickListener {
                GrindrPlus.httpClient.blockUser(profileId)
                true
            }
        }
    }
}
