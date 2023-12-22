package com.grindrplus

import com.grindrplus.Utils.logChatMessage

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class CommandDescription(val description: String)

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

    @CommandDescription("Show this help message.")
    private fun helpCommand(args: List<String>) {
        val commands = this::class.java.declaredMethods
            .filter { it.isAnnotationPresent(CommandDescription::class.java) }
            .joinToString("\n") { method ->
                val annotation = method.getAnnotation(CommandDescription::class.java)
                "${method.name.removeSuffix("Command")} - ${annotation.description}"
            }

        logChatMessage("Available commands:\n$commands", this.recipient, this.recipient)
    }

    @CommandDescription("Get the profile ID of the current profile.")
    private fun idCommand(args: List<String>) {
        logChatMessage("""
            Your profile ID is: ${Hooks.ownProfileId}
            This person's profile ID is: ${this.recipient}
        """.trimIndent(), this.recipient, this.recipient)
    }

    @CommandDescription("Open a profile by its ID.")
    private fun openCommand(args: List<String>) {
        if (args.isNotEmpty()) {
            Utils.openProfile(args[0])
        } else {
            logChatMessage("Please specify a profile ID.",
                this.recipient, this.recipient)
        }
    }

    @CommandDescription("Test if the module is working.")
    private fun pingCommand(args: List<String>) {
        logChatMessage("\uD83C\uDFD3 Pong!", this.recipient, this.recipient)
    }
}