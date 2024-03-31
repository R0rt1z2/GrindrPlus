package com.grindrplus.core

import android.content.ClipData
import android.content.ClipboardManager
import android.os.Build
import android.util.Log
import android.widget.Toast
import com.grindrplus.Hooker
import com.grindrplus.core.Utils.showDialog
import com.grindrplus.core.Utils.showToast
import com.grindrplus.modules.Location
import com.grindrplus.modules.Profile
import com.grindrplus.modules.Settings
import kotlin.reflect.full.declaredMemberFunctions
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.jvm.javaMethod

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class Command(val name: String, val aliases: Array<String> = [], val help: String = "")

abstract class CommandModule(protected val recipient: String, protected val sender: String) {
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
                Logger.xLog(Log.getStackTraceString(e))
                showDialog("Error", "Error executing command: $inputCommand\n${e.message}",
                    "OK", {}, "Copy error", {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            val clipboard = Hooker.appContext.getSystemService(ClipboardManager::class.java)
                            clipboard.setPrimaryClip(ClipData.newPlainText("Error", Log.getStackTraceString(e)))
                        }
                        showToast(Toast.LENGTH_LONG, "Error copied to clipboard.")
                    }
                )
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

class CommandHandler(private val recipient: String, private val sender: String = "") {
    private val commandModules: MutableList<CommandModule> = mutableListOf()

    init {
        commandModules.add(Location(recipient, sender))
        commandModules.add(Settings(recipient, sender))
        commandModules.add(Profile(recipient, sender))
    }

    fun handleCommand(input: String) {
        val args = input.split(" ")
        val command = args.firstOrNull() ?: return

        if (command == "help") {
            showDialog("Available commands",
                commandModules.joinToString("\n") {
                    it.getHelp() }.drop(1), "OK", {})
        }

        for (module in commandModules) {
            if (module.handleCommand(command, args.drop(1))) break
        }
    }
}