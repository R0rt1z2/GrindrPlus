package com.grindrplus.commands

import com.grindrplus.GrindrPlus
import kotlin.reflect.full.declaredMemberFunctions
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.jvm.javaMethod

abstract class CommandModule(
    protected val recipient: String,
    protected val sender: String
) {
    fun handle(inputCommand: String, args: List<String>): Boolean {
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
                GrindrPlus.logger.log("Error executing command: $inputCommand")
                false
            }
        } ?: false
    }

    fun getHelp(): String {
        try {
            val commands = this::class.declaredMemberFunctions
                .mapNotNull { function ->
                    val command = function.findAnnotation<Command>()
                    command?.let {
                        val aliasPart = if (it.aliases.isNotEmpty()) " (${it.aliases.joinToString(", ")})" else ""
                        "${it.name}$aliasPart: ${it.help}"
                    }
                }

            return "\nHelp for ${this::class.simpleName}:\n" +
                    commands.joinToString("\n") { command -> "- $command" }
        } catch (e: Exception) {
            return ""
        }
    }
}