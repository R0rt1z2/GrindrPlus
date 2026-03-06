package com.grindrplus.commands

import com.grindrplus.utils.UiHelper.showAlertDialog

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
            showAlertDialog {
                title = "Help"
                message = commandModules.joinToString("\n\n") { it.getHelp() }
            }
        }

        for (module in commandModules) {
            if (module.handle(command, args.drop(1))) break
        }
    }
}