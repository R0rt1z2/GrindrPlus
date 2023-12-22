package com.grindrplus

import com.grindrplus.Utils.logChatMessage

class CommandHandler(private val recipient: String) {
    fun handleCommand(command: String) {
        val args = command.split(" ")

        try {
            val method = this::class.java.getDeclaredMethod(
                "${args[0].lowercase()}Command",
                List::class.java
            )
            method.isAccessible = true
            method.invoke(this, args.drop(1))
        } catch (e: NoSuchMethodException) {
            logChatMessage("Unknown command: ${args[0]}",
                this.recipient, this.recipient)
        } catch (e: Exception) {
            logChatMessage("Error executing command: ${args[0]}",
                this.recipient, this.recipient)
        }
    }

    private fun helpCommand(args: List<String>) {
        logChatMessage("""
            Available commands:
            id - Get your profile ID
            open <profile ID> - Open a profile by its ID
            ping - Check if the bot is alive
        """.trimIndent(), this.recipient, this.recipient)
    }

    private fun idCommand(args: List<String>) {
        logChatMessage("""
            Your profile ID is: ${Hooks.ownProfileId}
            This person's profile ID is: ${this.recipient}
        """.trimIndent(), this.recipient, this.recipient)
    }

    private fun openCommand(args: List<String>) {
        if (args.isNotEmpty()) {
            Utils.openProfile(args[0])
        } else {
            logChatMessage("Please specify a profile ID.",
                this.recipient, this.recipient)
        }
    }

    private fun pingCommand(args: List<String>) {
        logChatMessage("\uD83C\uDFD3 Pong!", this.recipient, this.recipient)
    }
}