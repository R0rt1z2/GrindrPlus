package com.grindrplus.core

import android.util.Log
import com.grindrplus.core.Utils.logChatMessage
import com.grindrplus.modules.Location
import com.grindrplus.modules.Profile
import com.grindrplus.modules.Settings
import kotlin.reflect.full.declaredMemberFunctions
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.jvm.javaMethod

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class Command(val name: String, val aliases: Array<String> = [], val help: String = "")

abstract class CommandModule(protected val recipient: String) {
    fun handleCommand(inputCommand: String, args: List<String>): Boolean {
        val commandMethod = this::class.declaredMemberFunctions
            .firstOrNull { function ->
                val command = function.findAnnotation<Command>()
                command?.let {
                    it.name == inputCommand || inputCommand in it.aliases
                } ?: false
            }

        commandMethod?.javaMethod?.isAccessible = true

        return commandMethod?.let { method ->
            try {
                method.call(this, args)
                true
            } catch (e: Exception) {
                Log.e("CommandModule", "Error executing command: $inputCommand", e)
                logChatMessage("Error executing command: $inputCommand",
                    this.recipient, this.recipient)
                false
            }
        } ?: false
    }

    fun getHelp(): String {
        val commands = this::class.declaredMemberFunctions
            .mapNotNull { function ->
                val command = function.findAnnotation<Command>()
                command?.let {
                    val aliasPart = if (it.aliases.isNotEmpty()) " (${it.aliases.joinToString(", ")})" else ""
                    "${it.name}$aliasPart: ${it.help}"
                }
            }

        return "\n\nHelp for ${this::class.simpleName}:\n" +
                commands.joinToString("\n") { command -> "- $command" }
    }
}

class CommandHandler(private val recipient: String) {
    private val commandModules: MutableList<CommandModule> = mutableListOf()

    init {
        commandModules.add(Location(recipient))
        commandModules.add(Settings(recipient))
        commandModules.add(Profile(recipient))
    }

    fun handleCommand(input: String) {
        val args = input.split(" ")
        val command = args.firstOrNull() ?: return

        if (command == "help") {
            logChatMessage((commandModules.joinToString(
                "") { it.getHelp() }).drop(2), recipient, recipient)
        }

        for (module in commandModules) {
            if (module.handleCommand(command, args.drop(1))) break
        }
    }
}