package com.grindrplus.commands

import android.annotation.SuppressLint
import android.graphics.Color
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.widget.AppCompatTextView
import com.grindrplus.GrindrPlus
import com.grindrplus.core.Constants.NEWLINE
import com.grindrplus.core.Utils.openChat
import com.grindrplus.core.Utils.openProfile
import com.grindrplus.ui.Utils.copyToClipboard
import com.grindrplus.ui.Utils.formatEpochSeconds
import com.grindrplus.utils.UiHelper.DialogButton
import com.grindrplus.utils.UiHelper.showAlertDialog
import com.grindrplus.utils.UiHelper.showToast

class Profile(
    recipient: String,
    sender: String
) : CommandModule("Profile", recipient, sender) {
    @Command("open", help = "Open a user's profile")
    fun open(args: List<String>) {
        if (args.isNotEmpty()) {
            return openProfile(args[0])
        } else {
            showToast(
                "Please provide valid ID",
                Toast.LENGTH_LONG
            )
        }
    }

    @Command("block", help = "Block a user")
    fun block(args: List<String>) {
        GrindrPlus.httpClient.blockUser(
            if (args.isNotEmpty()) args[0] else sender,
            silent = args.contains("silent"),
            reflectInDb = !args.contains("no-reflect")
        )
    }

    @Command("clear", aliases = ["reset"], help = "Reset chat with a user")
    fun reset(args: List<String>) {
        val profileId = if (args.isNotEmpty()) args[0] else sender
        GrindrPlus.shouldTriggerAntiblock = false
        block(listOf(profileId, "silent", "no-reflect"))
        Thread.sleep(200)
        unblock(listOf(profileId, "silent", "no-reflect"))
        Thread.sleep(200)
        openChat("$recipient:$profileId")
        Thread.sleep(200)
        GrindrPlus.shouldTriggerAntiblock = true
    }

    @Command("unblock", help = "Unblock a user")
    fun unblock(args: List<String>) {
        GrindrPlus.httpClient.unblockUser(
            if (args.isNotEmpty()) args[0] else sender,
            silent = args.contains("silent"),
            reflectInDb = !args.contains("no-reflect")
        )
    }

    @Command("chat", help = "Open chat with a user")
    fun chat(args: List<String>) {
        if (args.isNotEmpty()) {
            return openChat("$recipient:${args[0]}")
        } else {
            showToast(
                "Please provide valid ID",
                Toast.LENGTH_LONG
            )
        }
    }

    @Command("favorite", aliases = ["fav", "favourite"], help = "Favorite a user")
    fun favorite(args: List<String>) {
        val profileId = if (args.isNotEmpty()) args[0] else sender
        GrindrPlus.httpClient.favorite(profileId, silent = false)
    }

    @Command("unfavorite", aliases = ["unfav", "unfavourite"], help = "Unfavorite a user")
    fun unfavorite(args: List<String>) {
        if (args.isNotEmpty() && args[0] == "all") {
            GrindrPlus.executeAsync {
                val favorites = GrindrPlus.httpClient.getFavorites()
                Thread.sleep(1000)
                favorites.forEach { GrindrPlus.httpClient.unfavorite(it.first, silent = true) }
            }
        }
        val profileId = if (args.isNotEmpty()) args[0] else sender
        GrindrPlus.httpClient.unfavorite(profileId, silent = false)
    }

    @Command("blocks", help = "Get a list of blocked users")
    fun blocks(args: List<String>) {
        GrindrPlus.executeAsync {
            val blocks = GrindrPlus.httpClient.getBlocks()
            val blockList = blocks.joinToString("\n") { "• $it" }
            GrindrPlus.runOnMainThreadWithCurrentActivity { activity ->
                val dialogView = LinearLayout(activity).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(60, 40, 60, 40)
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                }

                val textView = AppCompatTextView(activity).apply {
                    text = blockList
                    textSize = 14f
                    setTextColor(Color.WHITE)
                    setPadding(20, 20, 20, 20)
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        setMargins(0, 20, 0, 0)
                    }
                }

                dialogView.addView(textView)

                showAlertDialog {
                    title = "Blocked users"
                    view = dialogView
                    positiveButton = DialogButton(
                        text = "Copy",
                        onClick = {
                            copyToClipboard("Blocked users", blocks.joinToString("\n") { it })
                        }
                    )
                    negativeButton = DialogButton("Close")
                    neutralButton = DialogButton(
                        text = "Export",
                        onClick = {
                            val file = GrindrPlus.context.getFileStreamPath("blocks.txt")
                            file.writeText(blocks.joinToString("\n") { it })
                            showToast(
                                "Exported blocked users. Use Mod Settings to access the file!",
                                Toast.LENGTH_LONG
                            )
                        }
                    )
                }
            }
        }
    }

    @Command("favorites", aliases = ["favourites", "favs"], help = "Get a list of favorited users")
    fun favorites(args: List<String>) {
        GrindrPlus.executeAsync {
            val favorites = GrindrPlus.httpClient.getFavorites()
            val favoriteList = favorites.joinToString("\n") { "• ${it.first}" }

            val favoriteListExport = favorites.joinToString("\n") {
                val sanitizedNote = it.second.replace("\r\n", NEWLINE).replace("\r", NEWLINE).replace("\n", NEWLINE)
                val sanitizedPhone = it.third.replace("\r\n", NEWLINE).replace("\r", NEWLINE).replace("\n", NEWLINE)
                "${it.first}|||$sanitizedNote|||$sanitizedPhone"
            }

            GrindrPlus.runOnMainThreadWithCurrentActivity { activity ->
                val dialogView = LinearLayout(activity).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(60, 40, 60, 40)
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                }

                val textView = AppCompatTextView(activity).apply {
                    text = favoriteList
                    textSize = 14f
                    setTextColor(Color.WHITE)
                    setPadding(20, 20, 20, 20)
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        setMargins(0, 20, 0, 0)
                    }
                }

                dialogView.addView(textView)

                showAlertDialog {
                    title = "Favorited users"
                    view = dialogView
                    positiveButton = DialogButton(
                        text = "Copy",
                        onClick = { copyToClipboard("Favorited users", favoriteList) }
                    )
                    negativeButton = DialogButton("Close")
                    neutralButton = DialogButton(
                        text = "Export",
                        onClick = {
                            val file = GrindrPlus.context.getFileStreamPath("favorites.txt")
                            file.writeText(favoriteListExport)
                            showToast(
                                "Exported favorited users. Use Mod Settings to access the file!",
                                Toast.LENGTH_LONG
                            )
                        }
                    )
                }
            }
        }
    }

    @Command("report", help = "Report a user")
    fun report(args: List<String>) {
        val profileId = if (args.isNotEmpty()) args[0] else sender
        val reason = if (args.size > 1) args[1] else "SPAM"
        GrindrPlus.httpClient.reportUser(profileId, reason)
    }

    @SuppressLint("SetTextI18n")
    @Command("id", help = "Get and copy profile IDs")
    fun id(args: List<String>) {
        val accountCreationTime = formatEpochSeconds(
            GrindrPlus.spline.invert(sender.toDouble()).toLong())

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
                    text = "• Your ID: $recipient\n• Profile ID: $sender\n• Estimated creation: $accountCreationTime"
                    textSize = 18f
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                }
            }

            dialogView.addView(textView)

            showAlertDialog {
                title = "Profile IDs"
                view = dialogView
                positiveButton = DialogButton(
                    text = "Copy my ID",
                    onClick = { copyToClipboard("Your ID", recipient) },
                    onLongClick = { dialog ->
                        copyToClipboard("Your ID", " $recipient")
                        dialog.dismiss()
                    }
                )
                negativeButton = DialogButton(
                    text = "Copy profile ID",
                    onClick = { copyToClipboard("Profile ID", sender) },
                    onLongClick = { dialog ->
                        copyToClipboard("Profile ID", " $sender")
                        dialog.dismiss()
                    }
                )
                neutralButton = DialogButton("Close")
            }
        }
    }
}