package com.grindrplus

import com.grindrplus.Hooker.Companion.configManager
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
        logChatMessage("This person's profile ID is: ${this.recipient}",
            this.recipient, this.recipient)
    }

    @CommandDescription("Get your own profile ID.")
    private fun myidCommand(args: List<String>) {
        logChatMessage("Your profile ID is: ${Hooks.ownProfileId}",
            this.recipient, this.recipient)
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

    @CommandDescription("Toggle the profile redesign feature.")
    private fun redesignCommand(args: List<String>) {
        val newState = !configManager.readBoolean("profile_redesign", true)
        configManager.writeConfig("profile_redesign", newState)
        logChatMessage(
            "Profile redesign ${if (newState) "enabled" else "disabled"}.",
            this.recipient, this.recipient
        )
    }

    @CommandDescription("Control whether you want to be hidden from the view list.")
    private fun viewsCommand(args: List<String>) {
        val newState = !configManager.readBoolean("dont_record_views", true)
        configManager.writeConfig("dont_record_views", newState)
        logChatMessage(
            "You are ${if (newState) "now" else "no longer"} hidden from the view list.",
            this.recipient, this.recipient
        )
    }

    @CommandDescription("Teleport to a location.")
    private fun teleportCommand(args: List<String>) {
        if (args.isEmpty()) {
            val newState = !configManager.readBoolean("teleport_enabled", false)
            configManager.writeConfig("teleport_enabled", newState)
            return logChatMessage(
                "Teleport ${if (newState) "enabled" else "disabled"}.",
                this.recipient, this.recipient
            )
        }

        when {
            !configManager.readBoolean("teleport_enabled", false) -> {
                configManager.writeConfig("teleport_enabled", true)
            }

            args.size == 1 && args[0].contains(",") -> {
                val (lat, lon) = args[0].split(",").map { it.trim().toDouble() }
                Utils.setLocationPreference("teleport_location", lat, lon)
                logChatMessage("Teleported to $lat, $lon.", this.recipient, this.recipient)
            }
            args.size == 2 && args.all { it.toDoubleOrNull() != null } -> {
                val lat = args[0].toDouble()
                val lon = args[1].toDouble()
                Utils.setLocationPreference("teleport_location", lat, lon)
                logChatMessage("Teleported to $lat, $lon.", this.recipient, this.recipient)
            }
            else -> {
                val coordinates = Utils.getLatLngFromLocationName(args.joinToString(" "))
                if (coordinates != null) {
                    Utils.setLocationPreference("teleport_location", coordinates.first, coordinates.second)
                    logChatMessage("Teleported to ${coordinates.first}, ${coordinates.second}.",
                        this.recipient, this.recipient)
                } else {
                    logChatMessage("Could not find location.", this.recipient, this.recipient)
                }
            }
        }
    }

    @CommandDescription("Show the current teleport location.")
    private fun locationCommand(args: List<String>) {
        val location = Utils.getLocationPreference("teleport_location")
        if (!configManager.readBoolean("teleport_enabled", false)) {
            logChatMessage("Teleport is disabled.", this.recipient, this.recipient)
        } else {
            if (location != null) {
                logChatMessage(
                    "Current teleport location: ${location.first}, ${location.second}.",
                    this.recipient, this.recipient
                )
            } else {
                logChatMessage("No teleport location set.", this.recipient, this.recipient)
            }
        }
    }

    @CommandDescription("Toggle the profile details feature.")
    private fun detailsCommand(args: List<String>) {
        val newState = !configManager.readBoolean("show_profile_details", true)
        configManager.writeConfig("show_profile_details", newState)
        logChatMessage(
            "Profile details ${if (newState) "enabled" else "disabled"}.",
            this.recipient, this.recipient
        )
    }
}