package com.grindrplus.commands

import android.app.AlertDialog
import com.grindrplus.GrindrPlus

class CommandHandler(
    recipient: String,
    sender: String = ""
) {
    private val commandModules: MutableList<CommandModule> = mutableListOf()

    init {
        commandModules.add(Location(recipient, sender))
        commandModules.add(Profile(recipient, sender))
    }

    fun handle(input: String) {
        val args = input.split(" ")
        val command = args.firstOrNull() ?: return

        if (command == "help") {
            GrindrPlus.withCurrentActivityOnMainThread { activity ->
                AlertDialog.Builder(activity)
                    .setTitle("Help")
                    .setMessage(commandModules.joinToString("\n") { it.getHelp() }.drop(1))
                    .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
                    .create()
                    .show()
            }
        }

        for (module in commandModules) {
            if (module.handle(command, args.drop(1))) break
        }
    }
}