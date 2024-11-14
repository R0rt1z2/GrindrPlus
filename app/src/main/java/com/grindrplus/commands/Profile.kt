package com.grindrplus.commands

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.widget.AppCompatTextView
import com.grindrplus.GrindrPlus
import com.grindrplus.core.Config
import com.grindrplus.core.Utils.blockProfile
import com.grindrplus.core.Utils.openProfile
import com.grindrplus.ui.Utils.copyToClipboard

class Profile(
    recipient: String,
    sender: String
) : CommandModule("Profile", recipient, sender) {
    @Command("open", help = "Open a user's profile")
    fun open(args: List<String>) {
        if (args.isNotEmpty()) {
            return openProfile(args[0])
        } else {
            GrindrPlus.showToast(
                Toast.LENGTH_LONG,
                "Please provide valid ID"
            )
        }
    }

    @Command("block", help = "Block a user")
    fun block(args: List<String>) {
        if (args.isNotEmpty()) {
            blockProfile(args[0])
        } else {
            if (Config.get("confirm_block", true) as Boolean) {
                GrindrPlus.runOnMainThreadWithCurrentActivity { activity ->
                    AlertDialog.Builder(activity)
                        .setTitle("Block user")
                        .setMessage("Are you sure you want to block this user?")
                        .setPositiveButton("Block") { _, _ ->
                            blockProfile(sender)
                        }
                        .setNegativeButton("Cancel") { dialog, _ ->
                            dialog.dismiss()
                        }
                        .create()
                        .show()
                }
            } else {
                blockProfile(sender)
            }
        }
    }

    @SuppressLint("SetTextI18n")
    @Command("id", help = "Get and copy profile IDs")
    fun id(args: List<String>) {
        GrindrPlus.runOnMainThreadWithCurrentActivity { activity ->
            val dialogView = LinearLayout(activity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(60, 0, 60, 0)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }

            val textView = activity.let {
                AppCompatTextView(it).apply {
                    text = "• Your ID: $recipient\n• Profile ID: $sender"
                    textSize = 18f
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                }
            }

            dialogView.addView(textView)

            AlertDialog.Builder(activity)
                .setTitle("Profile IDs")
                .setView(dialogView)
                .setPositiveButton("Copy my ID") { _, _ ->
                    copyToClipboard("Your ID", recipient)
                }
                .setNegativeButton("Copy profile ID") { _, _ ->
                    copyToClipboard("Profile ID", sender)
                }
                .setNeutralButton("Close") { dialog, _ ->
                    dialog.dismiss()
                }
                .create()
                .also { alertDialog ->
                    alertDialog.show()
                    alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnLongClickListener {
                        copyToClipboard("Your ID", " $recipient")
                        alertDialog.dismiss()
                        true
                    }

                    alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnLongClickListener {
                        copyToClipboard("Profile ID", " $sender")
                        alertDialog.dismiss()
                        true
                    }
                }
        }
    }
}