package com.grindrplus.modules

import android.app.AlertDialog
import android.content.ClipData
import com.grindrplus.core.Command
import com.grindrplus.core.CommandModule
import com.grindrplus.core.Hooks.ownProfileId
import android.content.ClipboardManager
import android.os.Build
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.grindrplus.Hooker
import com.grindrplus.core.Utils.copyToClipboard
import com.grindrplus.core.Utils.openProfile
import com.grindrplus.core.Utils.showDialog
import com.grindrplus.core.Utils.showToast

class Profile(recipient: String, sender: String) : CommandModule(recipient, sender) {
    @Command(name = "id", help = "Get and copy profile IDs.")
    private fun idCommand(args: List<String>) {
        val dialogView = LinearLayout(Hooker.activityHook.getCurrentActivity()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(58, 32, 58, 32)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val textView = TextView(Hooker.activityHook.getCurrentActivity()).apply {
            text = "• Your ID: $ownProfileId\n• Profile ID: ${recipient}"
            textSize = 18f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        dialogView.addView(textView)

        val activity = Hooker.activityHook.getCurrentActivity()
        activity?.runOnUiThread {
            AlertDialog.Builder(Hooker.activityHook.getCurrentActivity()).apply {
                setTitle("Profile IDs")
                setView(dialogView)
                setPositiveButton("Copy my ID") { _, _ ->
                    copyToClipboard("Your Profile ID", ownProfileId!!)
                }
                setNegativeButton("Copy profile ID") { _, _ ->
                    copyToClipboard("Profile ID", recipient)
                }
                setNeutralButton("Close") { dialog, _ -> dialog.dismiss() }
                show()
            }
        }
    }

    @Command(name = "open", help = "Open a profile by its ID.")
    private fun open(args: List<String>) {
        if (args.isNotEmpty()) {
            openProfile(args[0])
        } else {
            showToast(Toast.LENGTH_LONG, "Please provide a profile ID.")
        }
    }
}