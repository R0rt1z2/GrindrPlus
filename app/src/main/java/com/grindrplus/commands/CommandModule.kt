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
            GrindrPlus.showToast(Toast.LENGTH_LONG,
                "An error occurred while executing the command: ${e.message}")
            GrindrPlus.logger.log("Error executing command: $inputCommand")
            e.printStackTrace()
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