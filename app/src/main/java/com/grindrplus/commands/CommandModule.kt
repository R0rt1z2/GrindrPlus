package com.grindrplus.commands

import android.widget.Toast
import com.grindrplus.GrindrPlus

abstract class CommandModule(
    protected val name: String,
    protected val recipient: String,
    protected val sender: String
) {
    fun handle(inputCommand: String, args: List<String>): Boolean {
        val commandMethod = this::class.java.methods.firstOrNull {
            val annotation = it.getAnnotation(Command::class.java)
            annotation != null
                    && (annotation.name == inputCommand || inputCommand in annotation.aliases)
        }

        return commandMethod != null && try {
            commandMethod.invoke(this, args)
            true
        } catch (e: Exception) {
            val message = "Unable to execute command. Check logs for more information."
            GrindrPlus.apply {
                showToast(Toast.LENGTH_LONG, message)
                logger.log("An error occurred while executing the command: ${e.message ?: "Unknown error"}")
                logger.writeRaw(e.stackTraceToString())
            }
            false
        }
    }

    fun getHelp(): String {
        val commands = this::class.java.methods.mapNotNull { method ->
            val command = method.getAnnotation(Command::class.java)
            command?.let {
                if (it.aliases.isEmpty()) {
                    "${it.name}: ${it.help}"
                } else {
                    "${it.name} (${it.aliases.joinToString(", ")}): ${it.help}"
                }
            }
        }

        return "Help for $name:\n${commands.joinToString("\n") { command -> "- $command" }}"
    }
}