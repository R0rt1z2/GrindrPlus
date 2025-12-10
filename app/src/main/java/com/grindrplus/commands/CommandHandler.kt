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
        commandModules.add(Utils(recipient, sender))
        commandModules.add(Database(recipient, sender))
    }

    fun handle(input: String) {
        val args = input.trim().split("\\s+".toRegex()).filter { it.isNotEmpty() }
        val command = args.firstOrNull() ?: return

        if (command == "help") {
            GrindrPlus.runOnMainThreadWithCurrentActivity { activity ->
                AlertDialog.Builder(activity)
                    .setTitle("Help")
                    .setMessage(commandModules.joinToString("\n\n") { it.getHelp() })
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