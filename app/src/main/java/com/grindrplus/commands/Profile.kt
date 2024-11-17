package com.grindrplus.commands

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.widget.AppCompatTextView
import com.grindrplus.GrindrPlus
import com.grindrplus.core.Utils.openChat
import com.grindrplus.core.Utils.openProfile
import com.grindrplus.ui.Utils.copyToClipboard
import okhttp3.RequestBody.Companion.toRequestBody

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
        val silent = "silent" in args
        val profileId = if (args.isNotEmpty()) args[0] else sender
        GrindrPlus.runCatching {
            val response = httpClient.sendRequest(
                "https://grindr.mobi/v3/me/blocks/$profileId",
                "POST",
            )
            if (response.isSuccessful) {
                if (!silent) {
                    showToast(
                        Toast.LENGTH_LONG,
                        "User blocked successfully"
                    )
                }
            } else {
                showToast(
                    Toast.LENGTH_LONG,
                    "Failed to block user: ${response.body?.string()}"
                )
            }
        }.onFailure {
            GrindrPlus.showToast(
                Toast.LENGTH_LONG,
                "Failed to block user: ${it.message}"
            )
        }
    }

    @Command("clear", aliases = ["reset"], help = "Reset chat with a user")
    fun reset(args: List<String>) {
        val profileId = if (args.isNotEmpty()) args[0] else sender
        block(listOf(profileId, "silent"))
        Thread.sleep(1000) // Wait for the block to take effect
        unblock(listOf(profileId, "silent"))
        openChat("$recipient:$profileId")
    }

    @Command("unblock", help = "Unblock a user")
    fun unblock(args: List<String>) {
        val silent = "silent" in args
        if (args.isNotEmpty()) {
            GrindrPlus.runCatching {
                val response = httpClient.sendRequest(
                    "https://grindr.mobi/v3/me/blocks/${args[0]}",
                    "DELETE"
                )
                if (response.isSuccessful) {
                    if (!silent) {
                        showToast(
                            Toast.LENGTH_LONG,
                            "User unblocked successfully"
                        )
                    }
                } else {
                    showToast(
                        Toast.LENGTH_LONG,
                        "Failed to unblock user: ${response.body?.string()}"
                    )
                }
            }
        } else {
            GrindrPlus.showToast(
                Toast.LENGTH_LONG,
                "Please provide valid ID"
            )
        }
    }

    @Command("chat", help = "Open chat with a user")
    fun chat(args: List<String>) {
        if (args.isNotEmpty()) {
            return openChat("$recipient:${args[0]}")
        } else {
            GrindrPlus.showToast(
                Toast.LENGTH_LONG,
                "Please provide valid ID"
            )
        }
    }

    @Command("report", help = "Report a user")
    fun report(args: List<String>) {
        val profileId = if (args.isNotEmpty()) args[0] else sender
        val reason = if (args.size > 1) args[1] else "SPAM"
        val body = """
                {
                    "reason": "$reason",
                    "comment": "",
                    "locations": [
                        "CHAT_MESSAGE"
                    ]
                }
            """.trimIndent()
        GrindrPlus.runCatching {
            val response = httpClient.sendRequest(
                "https://grindr.mobi/v3.1/flags/$profileId",
                "POST",
                mapOf("Content-Type" to "application/json"),
                body.toRequestBody()
            )
            if (response.isSuccessful) {
                showToast(
                    Toast.LENGTH_LONG,
                    "User reported successfully"
                )
            } else {
                showToast(
                    Toast.LENGTH_LONG,
                    "Failed to report user: ${response.body?.string()}"
                )
            }
        }.onFailure {
            GrindrPlus.showToast(
                Toast.LENGTH_LONG,
                "Failed to report user: ${it.message}"
            )
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